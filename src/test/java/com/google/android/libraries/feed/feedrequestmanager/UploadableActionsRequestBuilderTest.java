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

package com.google.android.libraries.feed.feedrequestmanager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.Result;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata.Type;
import com.google.search.now.wire.feed.ActionPropertiesProto.ActionProperties;
import com.google.search.now.wire.feed.ActionRequestProto.ActionRequest;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedActionProto.FeedAction;
import com.google.search.now.wire.feed.FeedActionRequestProto.FeedActionRequest;
import com.google.search.now.wire.feed.OpaqueActionDataForTestProto.OpaqueActionDataForTest;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import com.google.search.now.wire.feed.UploadActionTypeProto.UploadActionType;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Test of the {@link UploadableActionsRequestBuilder} class. */
@RunWith(RobolectricTestRunner.class)
public class UploadableActionsRequestBuilderTest {

  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private ActionReader actionReader;
  private static final String CONTENT_ID = "contentId";
  private UploadableActionsRequestBuilder builder;
  private HashSet<StreamUploadableAction> actionSet = new HashSet<>();
  private ConsistencyToken token =
      ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x1, 0xf})).build();
  private ActionRequest.Builder requestBuilder;
  private FeedActionRequest.Builder feedActionRequestBuilder;
  private OpaqueActionData data;

  @Before
  public void setUp() {
    initMocks(this);
    data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(CONTENT_ID).build())
            .build();
    when(actionReader.getActionProperties(Collections.singletonList(CONTENT_ID)))
        .thenReturn(
            Result.success(
                Collections.singletonList(new ActionPropertiesWithId(CONTENT_ID, data))));
    when(protocolAdapter.getWireContentId(CONTENT_ID))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    builder = new UploadableActionsRequestBuilder(actionReader, protocolAdapter);
    actionSet.add(
        StreamUploadableAction.newBuilder()
            .setFeatureContentId(CONTENT_ID)
            .setActionType(Type.DISMISS)
            .build());
    requestBuilder =
        ActionRequest.newBuilder()
            .setRequestVersion(ActionRequest.RequestVersion.FEED_UPLOAD_ACTION);
    feedActionRequestBuilder = FeedActionRequest.newBuilder();
  }

  @Test
  public void testUploadableActionsRequest_noToken() throws Exception {

    FeedAction.Builder feedAction =
        FeedAction.newBuilder()
            .setActionProperties(ActionProperties.newBuilder().setActionData(data).build())
            .setActionType(UploadActionType.DISMISS_UPLOAD);
    feedAction.setContentId(ContentId.getDefaultInstance());
    feedActionRequestBuilder.addFeedAction(feedAction);
    requestBuilder.setExtension(
        FeedActionRequest.feedActionRequest, feedActionRequestBuilder.build());

    ActionRequest expectedResult = requestBuilder.build();
    ActionRequest result = builder.setActions(actionSet).build();
    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  public void testUploadableActionsRequest_noActions() throws Exception {
    feedActionRequestBuilder.setConsistencyToken(token);
    requestBuilder.setExtension(
        FeedActionRequest.feedActionRequest, feedActionRequestBuilder.build());

    ActionRequest expectedResult = requestBuilder.build();
    ActionRequest result = builder.setConsistencyToken(token).build();
    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  public void testUploadableActionsRequest() throws Exception {
    FeedAction.Builder feedAction =
        FeedAction.newBuilder()
            .setActionProperties(ActionProperties.newBuilder().setActionData(data).build())
            .setActionType(UploadActionType.DISMISS_UPLOAD);
    feedAction.setContentId(ContentId.getDefaultInstance());
    feedActionRequestBuilder.addFeedAction(feedAction);
    feedActionRequestBuilder.setConsistencyToken(token);
    requestBuilder.setExtension(
        FeedActionRequest.feedActionRequest, feedActionRequestBuilder.build());

    ActionRequest expectedResult = requestBuilder.build();
    ActionRequest result = builder.setActions(actionSet).setConsistencyToken(token).build();
    assertThat(result).isEqualTo(expectedResult);
  }
}
