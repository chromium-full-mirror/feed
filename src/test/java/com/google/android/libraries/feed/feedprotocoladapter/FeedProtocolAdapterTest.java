// Copyright 2018 The Feed Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.libraries.feed.feedprotocoladapter;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.ResponseProto.Response;
import org.robolectric.RobolectricTestRunner;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link FeedProtocolAdapter} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedProtocolAdapterTest {
  @Mock private SessionManager sessionManager;

  private final InternalProtocolUtils protocolUtils = new InternalProtocolUtils();
  private final TimingUtils timingUtils = new TimingUtils();
  private FakeSessionMutation sessionMutation;
  private WireProtocolResponseBuilder responseBuilder;

  @Before
  public void init() {
    initMocks(this);
    sessionMutation = new FakeSessionMutation();
    when(sessionManager.edit()).thenReturn(sessionMutation);
    responseBuilder = new WireProtocolResponseBuilder();
  }

  @Test
  public void testConvertContentId() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    ContentId contentId = WireProtocolResponseBuilder.createFeatureContentId(13);
    StreamContentId streamContentId = protocolAdapter.getStreamContentId(contentId);
    assertThat(streamContentId).isNotNull();
    assertThat(streamContentId.getContentDomain()).isEqualTo(contentId.getContentDomain());
    assertThat(streamContentId.getId()).isEqualTo(contentId.getId());
    assertThat(streamContentId.getTable()).isEqualTo(contentId.getTable());
  }

  @Test
  public void testSimpleResponse_clear() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    ByteString token = ByteString.copyFromUtf8("token");
    StreamToken streamToken = StreamToken.newBuilder().setNextPageToken(token).build();
    protocolAdapter.streamTokens.put(token.hashCode(), streamToken);

    Response response = responseBuilder.addClearOperation().build();
    protocolAdapter.createModel(response, token);
    assertThat(sessionMutation.streamToken.getNextPageToken()).isEqualTo(token);
    assertThat(sessionMutation.operations).hasSize(1);
  }

  @Test
  public void testSimpleResponse_feature() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    Response response = responseBuilder.addRootFeature().build();

    protocolAdapter.createModel(response, null);
    assertThat(sessionMutation.streamToken).isNull();
    assertThat(sessionMutation.operations).hasSize(1);
    StreamDataOperation sdo = sessionMutation.operations.get(0);
    assertThat(sdo.hasStreamPayload()).isTrue();
    assertThat(sdo.getStreamPayload().hasStreamFeature()).isTrue();
    assertThat(sdo.hasStreamStructure()).isTrue();
    assertThat(sdo.getStreamStructure().hasContentId()).isTrue();
    // Added the root
    assertThat(sdo.getStreamStructure().hasParentContentId()).isFalse();
  }

  @Test
  public void testResponse_remove() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    Response response =
        responseBuilder
            .removeFeature(ContentId.getDefaultInstance(), ContentId.getDefaultInstance())
            .build();
    protocolAdapter.createModel(response, null);
    assertThat(sessionMutation.streamToken).isNull();
    assertThat(sessionMutation.operations).hasSize(1);
  }

  @Test
  public void testPietSharedState() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    Response response = responseBuilder.addPietSharedState().build();
    protocolAdapter.createModel(response, null);
    assertThat(sessionMutation.streamToken).isNull();
    assertThat(sessionMutation.operations).hasSize(1);
    StreamDataOperation sdo = sessionMutation.operations.get(0);
    assertThat(sdo.hasStreamPayload()).isTrue();
    assertThat(sdo.getStreamPayload().hasStreamSharedState()).isTrue();
    assertThat(sdo.hasStreamStructure()).isTrue();
    assertThat(sdo.getStreamStructure().hasContentId()).isTrue();
  }

  @Test
  public void testContinuationToken_nextPageToken() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    ByteString tokenForMutation = ByteString.copyFrom("token", Charset.defaultCharset());
    Response response = responseBuilder.addStreamToken(tokenForMutation).build();

    protocolAdapter.createModel(response, tokenForMutation);
    assertThat(sessionMutation.operations).hasSize(1);
    StreamDataOperation sdo = sessionMutation.operations.get(0);
    assertThat(sdo.hasStreamPayload()).isTrue();
    assertThat(sdo.getStreamPayload().hasStreamToken()).isTrue();
  }

  @Test
  public void testContinuationToken_responseFromToken() {
    FeedProtocolAdapter protocolAdapter =
        new FeedProtocolAdapter(sessionManager, timingUtils, protocolUtils);
    ByteString tokenForMutation = ByteString.copyFrom("token", Charset.defaultCharset());
    Response response = responseBuilder.addStreamToken(tokenForMutation).build();
    StreamToken streamToken = StreamToken.newBuilder().setNextPageToken(tokenForMutation).build();
    protocolAdapter.streamTokens.put(tokenForMutation.hashCode(), streamToken);

    protocolAdapter.createModel(response, tokenForMutation);
    assertThat(sessionMutation.streamToken.getNextPageToken()).isEqualTo(tokenForMutation);
  }

  private static class FakeSessionMutation implements SessionMutation {
    List<StreamDataOperation> operations = new ArrayList<>();
    StreamToken streamToken;

    @Override
    public void addOperation(StreamDataOperation dataOperation) {
      operations.add(dataOperation);
    }

    @Override
    public void setMutationSourceToken(StreamToken streamToken) {
      this.streamToken = streamToken;
    }

    @Override
    public void commit() {
      // do nothing...
    }
  }
}
