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

package com.google.android.libraries.feed.infraintegration;

import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.EMPTY_MUTATION_CONTEXT;
import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.ROOT_CONTENT_ID;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.api.modelprovider.TokenCompletedObserver;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.WireProtocolInfo;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/** Test of handling paging operations within the Stream. */
@RunWith(RobolectricTestRunner.class)
public class StreamPagingTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private RequestManager requestManager;

  private ProtocolAdapter protocolAdapter;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;
  private ContentIdGenerators contentIdGenerators = new ContentIdGenerators();

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, requestManager, MoreExecutors.newDirectExecutorService())
            .build();
    protocolAdapter = scope.getProtocolAdapter();
    modelProviderFactory = scope.getModelProviderFactory();
    modelValidator = new ModelProviderValidator(protocolAdapter);
  }

  @Test
  public void testPaging() {
    // ModelProvider created after $HEAD has content, one root, and two Cards and a token
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    ByteString token = ByteString.copyFrom("1", Charset.defaultCharset());
    responseBuilder.addStreamToken(token);
    StreamToken streamToken = getStreamToken(token);

    // Response for the second page of
    ContentId[] pageCards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(3),
          WireProtocolResponseBuilder.createFeatureContentId(4)
        };
    WireProtocolResponseBuilder pageResponseBuilder = new WireProtocolResponseBuilder();
    for (ContentId contentId : pageCards) {
      pageResponseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }

    // wire up the request manager to handle the paging request
    AtomicReference<MutationContext> mutationContextRef = new AtomicReference<>();
    doAnswer(
            new Answer<Void>() {
              public Void answer(InvocationOnMock invocation) throws Throwable {
                // when we ask the RequestManager to load more, here is the next response
                protocolAdapter.createModel(pageResponseBuilder.build(), mutationContextRef.get());
                return null;
              }
            })
        .when(requestManager)
        .loadMore(Matchers.any());

    // Create an initial model
    protocolAdapter.createModel(responseBuilder.build(), EMPTY_MUTATION_CONTEXT);
    ModelProvider modelProvider = modelProviderFactory.createNew();
    modelValidator.assertRoot(modelProvider);
    WireProtocolInfo protocolInfo = responseBuilder.getWireProtocolInfo();
    assertThat(protocolInfo.hasToken).isTrue();

    // For the page request build the proper mutation.  this is used above in the doAnswer
    // when the protocol adapter processes that response.
    StreamSession streamSession =
        StreamSession.newBuilder().setStreamToken(modelProvider.getSessionToken()).build();
    mutationContextRef.set(
        new MutationContext.Builder()
            .setContinuationToken(streamToken)
            .setRequestingSession(streamSession)
            .build());

    // Validate the structure of the stream after the initial response
    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor cursor = rootFeature.getCursor();
    ModelChild tokenFeature = modelValidator.assertCursorContentsWithToken(cursor, cards);
    assertThat(tokenFeature).isNotNull();

    // Add an observer to the Token to get an event when the response is processed
    TokenCompletedObserver tokenCompletedObserver = mock(TokenCompletedObserver.class);
    ModelToken modelToken = tokenFeature.getModelToken();
    assertThat(modelToken).isNotNull();

    // Capture the event triggered once the paging operation is finished
    modelToken.registerObserver(tokenCompletedObserver);
    modelProvider.handleToken(modelToken);
    ArgumentCaptor<TokenCompleted> completedArgumentCaptor =
        ArgumentCaptor.forClass(TokenCompleted.class);
    verify(tokenCompletedObserver).onChange(completedArgumentCaptor.capture());
    ModelCursor pageCursor = completedArgumentCaptor.getValue().getCursor();
    assertThat(pageCursor).isNotNull();

    // The event cursor will have only the new items added in the page
    modelValidator.assertCursorContents(pageCursor, pageCards);

    // The full cursor should now contain all the features
    cursor = rootFeature.getCursor();
    modelValidator.assertCursorContents(cursor, cards[0], cards[1], pageCards[0], pageCards[1]);
  }

  private StreamToken getStreamToken(ByteString token) {
    ContentId tokenId =
        ContentId.newBuilder().setContentDomain("token").setId(1).setTable("feature").build();

    StreamToken.Builder tokenBuilder = StreamToken.newBuilder();
    // TODO: This needs to be the real root id.  How do we get the root id?
    tokenBuilder.setParentId(contentIdGenerators.createContentId(ROOT_CONTENT_ID));
    tokenBuilder.setContentId(contentIdGenerators.createContentId(tokenId));
    tokenBuilder.setNextPageToken(token);
    return tokenBuilder.build();
  }
}
