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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/**
 * This test will create multiple sessions with different content in each. It will then page each
 * session with different tokens to verify that paging sessions doesn't affect sessions without the
 * paging token.
 *
 * <p>The test runs the following tasks:
 *
 * <ol>
 *   <li>Create the initial response and page response for Session 1 and 2, including tokens
 *   <li>Create Session 1 and $HEAD from Session 1 initial response
 *   <li>Create Session 2 from $HEAD (Session 1 initial response)
 *   <li>Refresh Session 2 (directly using the protocol adapter) using Session 2 initial response.
 *       This creates a new $HEAD and invalidates the Session 2 Model Provider
 *   <li>Create a new Session 2 against the new $HEAD
 *   <li>Validate that Session 1 and 2 have the expected content
 *   <li>Page Session 1 and validate that both sessions have expected content
 *   <li>Page Session 2 and validate that both sessions have expected content
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
public class MultiSessionPagingTest {
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
    // Create session 1 content
    ContentId[] s1Cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2)
        };
    ContentId[] s1PageCards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(3),
          WireProtocolResponseBuilder.createFeatureContentId(4)
        };
    ByteString token1 = ByteString.copyFrom("s1-page", Charset.defaultCharset());
    StreamToken streamToken1 = getStreamToken(token1, 1);
    WireProtocolResponseBuilder s1InitialResponse = getInitialResponse(s1Cards, token1);
    WireProtocolResponseBuilder s1PageResponse = getPageResponse(s1PageCards);

    // Create session 2 content
    ContentId[] s2Cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(101),
          WireProtocolResponseBuilder.createFeatureContentId(102)
        };
    ContentId[] s2PageCards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(103),
          WireProtocolResponseBuilder.createFeatureContentId(104)
        };
    ByteString token2 = ByteString.copyFrom("s2-page", Charset.defaultCharset());
    StreamToken streamToken2 = getStreamToken(token2, 2);
    WireProtocolResponseBuilder s2InitialResponse = getInitialResponse(s2Cards, token2);
    WireProtocolResponseBuilder s2PageResponse = getPageResponse(s2PageCards);

    // Create an initial S1 $HEAD session
    protocolAdapter.createModel(s1InitialResponse.build(), EMPTY_MUTATION_CONTEXT);
    ModelProvider mp1 = modelProviderFactory.createNew();
    modelValidator.assertRoot(mp1);
    WireProtocolInfo protocolInfo = s1InitialResponse.getWireProtocolInfo();
    assertThat(protocolInfo.hasToken).isTrue();
    ModelChild mp1Token = modelValidator.assertCursorContentsWithToken(mp1, s1Cards);
    assertThat(mp1.getCurrentState()).isEqualTo(State.READY);

    // Create a second session against the S1 head.
    ModelProvider mp2 = modelProviderFactory.createNew();
    assertThat(mp2.getCurrentState()).isEqualTo(State.READY);
    ModelChild mp2Token = modelValidator.assertCursorContentsWithToken(mp2, s1Cards);

    // Refresh the Stream with the S2 initial response
    StreamSession streamSession =
        StreamSession.newBuilder().setStreamToken(mp2.getSessionToken()).build();
    protocolAdapter.createModel(
        s2InitialResponse.build(),
        new MutationContext.Builder().setRequestingSession(streamSession).build());
    assertThat(mp1.getCurrentState()).isEqualTo(State.READY);
    assertThat(mp2.getCurrentState()).isEqualTo(State.INVALIDATED);

    // Now create a ModelProvider against the new session.
    mp2 = modelProviderFactory.createNew();
    modelValidator.assertRoot(mp2);
    protocolInfo = s1InitialResponse.getWireProtocolInfo();
    assertThat(protocolInfo.hasToken).isTrue();
    modelValidator.assertCursorContentsWithToken(mp2, s2Cards);
    assertThat(mp2.getCurrentState()).isEqualTo(State.READY);

    // Verify that we didn't change the first session
    modelValidator.assertCursorContentsWithToken(mp1, s1Cards);
    assertThat(mp1.getCurrentState()).isEqualTo(State.READY);

    // now page S1
    streamSession = StreamSession.newBuilder().setStreamToken(mp1.getSessionToken()).build();
    AtomicReference<MutationContext> mutationContextRef = new AtomicReference<>();
    mutationContextRef.set(
        new MutationContext.Builder()
            .setContinuationToken(streamToken1)
            .setRequestingSession(streamSession)
            .build());
    // wire up the request manager to handle the paging request
    doAnswer(
            new Answer<Void>() {
              public Void answer(InvocationOnMock invocation) throws Throwable {
                // when we ask the RequestManager to load more, here is the next response
                protocolAdapter.createModel(s1PageResponse.build(), mutationContextRef.get());
                return null;
              }
            })
        .when(requestManager)
        .loadMore(Matchers.any());
    mp1.handleToken(mp1Token.getModelToken());
    modelValidator.assertCursorContents(
        mp1, s1Cards[0], s1Cards[1], s1PageCards[0], s1PageCards[1]);
    modelValidator.assertCursorContentsWithToken(mp2, s2Cards);

    // now page S2
    streamSession = StreamSession.newBuilder().setStreamToken(mp2.getSessionToken()).build();
    mutationContextRef.set(
        new MutationContext.Builder()
            .setContinuationToken(streamToken2)
            .setRequestingSession(streamSession)
            .build());
    doAnswer(
            new Answer<Void>() {
              public Void answer(InvocationOnMock invocation) throws Throwable {
                // when we ask the RequestManager to load more, here is the next response
                protocolAdapter.createModel(s2PageResponse.build(), mutationContextRef.get());
                return null;
              }
            })
        .when(requestManager)
        .loadMore(Matchers.any());
    mp2.handleToken(mp2Token.getModelToken());
    modelValidator.assertCursorContents(
        mp1, s1Cards[0], s1Cards[1], s1PageCards[0], s1PageCards[1]);
    modelValidator.assertCursorContents(
        mp2, s2Cards[0], s2Cards[1], s2PageCards[0], s2PageCards[1]);
  }

  private WireProtocolResponseBuilder getInitialResponse(ContentId[] cards, ByteString token) {
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    responseBuilder.addStreamToken(token);
    return responseBuilder;
  }

  private WireProtocolResponseBuilder getPageResponse(ContentId[] cards) {
    WireProtocolResponseBuilder pageResponseBuilder = new WireProtocolResponseBuilder();
    for (ContentId contentId : cards) {
      pageResponseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    return pageResponseBuilder;
  }

  private StreamToken getStreamToken(ByteString token, long id) {
    ContentId tokenId =
        ContentId.newBuilder().setContentDomain("token").setId(id).setTable("feature").build();

    StreamToken.Builder tokenBuilder = StreamToken.newBuilder();
    tokenBuilder.setParentId(contentIdGenerators.createContentId(ROOT_CONTENT_ID));
    tokenBuilder.setContentId(contentIdGenerators.createContentId(tokenId));
    tokenBuilder.setNextPageToken(token);
    return tokenBuilder.build();
  }
}
