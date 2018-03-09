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

package com.google.android.libraries.feed.feedmodelprovider;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor.ProcessNextChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelMutator;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.api.modelprovider.TokenCompletedObserver;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProvider.InitializeModel;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProvider.TokenMutation;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProvider.TokenTracking;
import com.google.android.libraries.feed.feedmodelprovider.internal.UpdatableModelChild;
import com.google.android.libraries.feed.feedmodelprovider.internal.UpdatableModelToken;
import com.google.android.libraries.feed.host.common.Callback;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.robolectric.RobolectricTestRunner;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests of the {@link FeedModelProvider}. */
@RunWith(RobolectricTestRunner.class)
public class FeedModelProviderTest {

  private StreamContentId rootContentId =
      StreamContentId.newBuilder()
          .setContentDomain("stream_root")
          .setId(0)
          .setTable("feature")
          .build();

  @Mock private SessionManager sessionManager;
  @Mock private ThreadUtils threadUtils;

  private int cursorCount = 0;
  private ModelChild continuationToken = null;
  private TimingUtils timingUtils = new TimingUtils();
  private InternalProtocolUtils internalProtocolUtils = new InternalProtocolUtils();

  private List<PayloadWithId> childBindings = new ArrayList<>();

  @Before
  public void setUp() {
    initMocks(this);
    childBindings.clear();
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("unchecked")
                Callback<List<PayloadWithId>> callback =
                    (Callback<List<PayloadWithId>>) invocation.getArguments()[1];
                callback.handleResult(childBindings);
                return null;
              }
            })
        .when(sessionManager)
        .getStreamFeatures(Matchers.any(), Matchers.any());
  }

  @Test
  public void testMinimalModelProvider() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INITIALIZING);

    // Add a root to the model provider
    ModelMutator mutator = modelProvider.edit();
    assertThat(mutator).isNotNull();

    StreamFeature rootStreamFeature = getRootFeature();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    mutator.commit();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);

    // Verify that we have a root
    ModelFeature rootFeature = modelProvider.getRootFeature();
    assertThat(rootFeature).isNotNull();
    assertThat(rootFeature.getStreamFeature()).isEqualTo(rootStreamFeature);

    // Verify that the cursor exists but is at end
    ModelCursor modelCursor = rootFeature.getCursor();
    assertThat(modelCursor).isNotNull();
    assertThat(modelCursor.isAtEnd()).isTrue();
  }

  @Test
  public void testEmptyStream() {
    FeedModelProvider modelProvider = createFeedModelProvider();

    ModelMutator mutator = modelProvider.edit();
    mutator.commit();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider.getRootFeature()).isNull();
  }

  @Test
  public void testCursor() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelMutator mutator = getRootedModelMutator(modelProvider);
    int featureCnt = 2;
    for (int i = 0; i < featureCnt; i++) {
      mutator.addChild(createStreamStructureAndBinding(createFeature(i + 1, rootContentId)));
    }
    mutator.commit();

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();
    assertThat(modelCursor).isNotNull();
    assertThat(modelCursor.isAtEnd()).isFalse();

    cursorCount = 0;
    while (modelCursor.advanceCursor(incrementChildCountProcessor())) ;
    assertThat(cursorCount).isEqualTo(featureCnt);
    assertThat(modelCursor.isAtEnd()).isTrue();
  }

  @Test
  public void testRemove() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelMutator mutator = getRootedModelMutator(modelProvider);
    int featureCnt = 2;
    for (int i = 0; i < featureCnt; i++) {
      mutator.addChild(createStreamStructureAndBinding(createFeature(i + 1, rootContentId)));
    }
    mutator.removeChild(createRemove(rootContentId, createFeatureContentId(2)));
    mutator.commit();

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();

    // Verify that one of the features was removed by a the last operation
    cursorCount = 0;
    while (modelCursor.advanceCursor(incrementChildCountProcessor())) ;
    assertThat(cursorCount).isEqualTo(featureCnt - 1);
  }

  @Test
  public void testMultiLevelCursors() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelMutator mutator = getRootedModelMutator(modelProvider);

    int featureCnt = 3;
    for (int i = 0; i < featureCnt; i++) {
      mutator.addChild(createStreamStructureAndBinding(createFeature(i + 1, rootContentId)));
    }
    StreamContentId featureParent = createFeatureContentId(2);
    for (int i = 0; i < featureCnt; i++) {
      mutator.addChild(
          createStreamStructureAndBinding(createFeature(i + 1 + featureCnt, featureParent)));
    }
    mutator.commit();

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();
    assertThat(modelCursor).isNotNull();
    assertThat(modelCursor.isAtEnd()).isFalse();

    cursorCount = 0;
    while (modelCursor.advanceCursor(
        new ProcessNextChild() {
          @Override
          public void processNextChild(ModelChild child) {
            cursorCount++;
            if (child.getType() == Type.FEATURE) {
              ModelFeature feature = child.getModelFeature();
              ModelCursor nextCursor = feature.getCursor();
              if (!nextCursor.isAtEnd()) {
                while (nextCursor.advanceCursor(incrementChildCountProcessor())) {;
                }
              }
            }
          }
        })) {}
    assertThat(cursorCount).isEqualTo(featureCnt + featureCnt);
  }

  @Test
  public void testSharedState() {
    ContentId contentId = ContentId.getDefaultInstance();
    StreamSharedState streamSharedState = StreamSharedState.getDefaultInstance();
    when(sessionManager.getSharedState(contentId)).thenReturn(streamSharedState);

    FeedModelProvider modelProvider = createFeedModelProvider();
    assertThat(modelProvider.getSharedState(contentId)).isEqualTo(streamSharedState);
  }

  @Test
  public void testTokenTracking() {
    UpdatableModelToken continuationToken = mock(UpdatableModelToken.class);
    ArrayList<UpdatableModelChild> location = new ArrayList<>();
    StreamContentId parentContentId = StreamContentId.getDefaultInstance();
    TokenTracking tokenTracking = new TokenTracking(continuationToken, parentContentId, location);
    assertThat(tokenTracking.tokenChild).isEqualTo(continuationToken);
    assertThat(tokenTracking.parentContentId).isEqualTo(parentContentId);
    assertThat(tokenTracking.location).isEqualTo(location);
  }

  @Test
  public void testHandleToken() {
    StreamToken streamToken = StreamToken.getDefaultInstance();
    ModelToken modelToken = mock(ModelToken.class);
    when(modelToken.getStreamToken()).thenReturn(streamToken);

    FeedModelProvider modelProvider = createFeedModelProvider();
    modelProvider.handleToken(modelToken);
    verify(sessionManager).handleToken(streamToken);
  }

  @Test
  public void testInvalidate() {
    // Create a valid model
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelMutator mutator = modelProvider.edit();
    StreamFeature rootStreamFeature = getRootFeature();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    mutator.commit();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();

    modelProvider.invalidate();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);
    assertThat(modelCursor.isAtEnd()).isTrue();
  }

  @Test
  public void testObserverLifecycle() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelProviderObserver observer1 = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer1);
    verify(observer1, never()).onSessionStart();

    ModelMutator mutator = modelProvider.edit();
    StreamFeature rootStreamFeature = getRootFeature();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    mutator.commit();
    verify(observer1).onSessionStart();
    verify(observer1, never()).onRootSet();

    ModelProviderObserver observer2 = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer2);
    verify(observer2).onSessionStart();

    modelProvider.invalidate();
    verify(observer1).onSessionFinished();
    verify(observer2).onSessionFinished();

    ModelProviderObserver observer3 = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer3);
    verify(observer3).onSessionFinished();
  }

  @Test
  public void testObserverLifecycle_resetRoot() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);
    verify(observer, never()).onSessionStart();

    ModelMutator mutator = modelProvider.edit();
    StreamFeature rootStreamFeature = getRootFeature();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    mutator.commit();
    verify(observer).onSessionStart();
    verify(observer, never()).onRootSet();

    mutator = modelProvider.edit();
    StreamContentId anotherRootId =
        StreamContentId.newBuilder()
            .setContentDomain("stream_root")
            .setId(100)
            .setTable("feature")
            .build();
    rootStreamFeature = StreamFeature.newBuilder().setContentId(anotherRootId).build();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    mutator.commit();
    verify(observer).onRootSet();
  }

  @Test
  public void testObserverList() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);

    List<ModelProviderObserver> observers = modelProvider.getObserversToNotify();
    assertThat(observers.size()).isEqualTo(1);
    assertThat(observers.get(0)).isEqualTo(observer);
  }

  @Test
  public void testInitializationModelMutationHandler() {
    FeedModelProvider modelProvider = createFeedModelProvider();
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INITIALIZING);

    InitializeModel initializeModel = modelProvider.new InitializeModel();
    initializeModel.postMutation();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    verify(observer).onSessionStart();
  }

  @Test
  public void testTokens() {
    FeedModelProvider modelProvider = createFeedModelProvider();

    int featureCnt = 3;
    StreamToken streamToken = initializeStreamWithToken(modelProvider, featureCnt);

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();
    cursorCount = 0;
    continuationToken = null;
    while (modelCursor.advanceCursor(
        new ProcessNextChild() {
          @Override
          public void processNextChild(ModelChild child) {
            cursorCount++;
            if (child.getType() == Type.TOKEN) {
              continuationToken = child;
            }
          }
        })) {}
    assertThat(cursorCount).isEqualTo(featureCnt + 1);
    assertThat(continuationToken).isNotNull();
    assertThat(continuationToken.getModelToken().getStreamToken()).isEqualTo(streamToken);
  }

  @Test
  public void testTokenMutation() {
    FeedModelProvider modelProvider = createFeedModelProvider();

    int featureCnt = 3;
    StreamToken streamToken = initializeStreamWithToken(modelProvider, featureCnt);
    Map<ByteString, TokenTracking> tokens = modelProvider.getTokensForTest();
    assertThat(tokens).hasSize(1);

    ModelFeature rootFeature = modelProvider.getRootFeature();
    ModelCursor modelCursor = rootFeature.getCursor();
    continuationToken = null;
    while (modelCursor.advanceCursor(
        new ProcessNextChild() {
          @Override
          public void processNextChild(ModelChild child) {
            if (child.getType() == Type.TOKEN) {
              continuationToken = child;
            }
          }
        })) {}
    assertThat(continuationToken).isNotNull();

    TokenMutation tokenMutation = modelProvider.new TokenMutation(streamToken);
    TokenTracking tokenTracking = tokenMutation.getTokenTrackingForTest();
    assertThat(tokenTracking.location.size()).isEqualTo(featureCnt + 1);

    tokenMutation.preMutation();
    assertThat(tokenTracking.location.size()).isEqualTo(featureCnt + 1);
    assertThat(tokenMutation.newCursorStart).isEqualTo(tokenTracking.location.size());
    tokens = modelProvider.getTokensForTest();
    assertThat(tokens).hasSize(0);

    TokenCompletedObserver tokenCompletedObserver =
        new TokenCompletedObserver() {
          @Override
          public void onChange(TokenCompleted change) {
            assertThat(change).isNotNull();
          }
        };
    continuationToken.getModelToken().registerObserver(tokenCompletedObserver);
    tokenMutation.postMutation();
  }

  private StreamToken initializeStreamWithToken(FeedModelProvider modelProvider, int featureCnt) {
    ModelMutator mutator = getRootedModelMutator(modelProvider);

    // Populate the model provider with a continuation token at the end.
    for (int i = 0; i < featureCnt; i++) {
      mutator.addChild(createStreamStructureAndBinding(createFeature(i + 1, rootContentId)));
    }
    ByteString bytes = ByteString.copyFrom("continuation", Charset.defaultCharset());
    StreamToken streamToken =
        StreamToken.newBuilder().setNextPageToken(bytes).setParentId(rootContentId).build();
    mutator.addChild(createStreamStructureAndBinding(streamToken));
    mutator.commit();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    return streamToken;
  }

  private FeedModelProvider createFeedModelProvider() {
    return new FeedModelProvider(
        sessionManager,
        threadUtils,
        timingUtils,
        internalProtocolUtils,
        new UiRunnableHandlerImpl());
  }

  private StreamFeature createFeature(int i, StreamContentId parentContentId) {
    return StreamFeature.newBuilder()
        .setParentId(parentContentId)
        .setContentId(createFeatureContentId(i))
        .setCard(Card.getDefaultInstance())
        .build();
  }

  private StreamContentId createFeatureContentId(int i) {
    return StreamContentId.newBuilder()
        .setContentDomain("feature")
        .setId(i)
        .setTable("feature")
        .build();
  }

  private ModelMutator getRootedModelMutator(ModelProvider modelProvider) {
    ModelMutator mutator = modelProvider.edit();
    assertThat(mutator).isNotNull();
    StreamFeature rootStreamFeature = getRootFeature();
    mutator.addChild(createStreamStructureAndBinding(rootStreamFeature));
    return mutator;
  }

  private StreamStructure createStreamStructureFromFeature(StreamFeature feature) {
    StreamStructure.Builder builder =
        StreamStructure.newBuilder()
            .setContentId(feature.getContentId())
            .setOperation(Operation.UPDATE_OR_APPEND);
    if (feature.hasParentId()) {
      builder.setParentContentId(feature.getParentId());
    }
    return builder.build();
  }

  private StreamStructure createStreamStructureFromToken(StreamToken token) {
    StreamStructure.Builder builder =
        StreamStructure.newBuilder()
            .setContentId(token.getContentId())
            .setOperation(Operation.UPDATE_OR_APPEND);
    if (token.hasParentId()) {
      builder.setParentContentId(token.getParentId());
    }
    return builder.build();
  }

  private StreamStructure createRemove(StreamContentId parentContentId, StreamContentId contentId) {
    return StreamStructure.newBuilder()
        .setContentId(contentId)
        .setParentContentId(parentContentId)
        .setOperation(Operation.REMOVE)
        .build();
  }

  /** This has the side affect of populating {@code childBindings} with a {@code PayloadWithId}. */
  private StreamStructure createStreamStructureAndBinding(StreamFeature feature) {
    StreamPayload payload = StreamPayload.newBuilder().setStreamFeature(feature).build();
    childBindings.add(new PayloadWithId(feature.getContentId(), payload));
    return createStreamStructureFromFeature(feature);
  }

  /** This has the side affect of populating {@code childBindings} with a {@code PayloadWithId}. */
  private StreamStructure createStreamStructureAndBinding(StreamToken token) {
    StreamPayload payload = StreamPayload.newBuilder().setStreamToken(token).build();
    childBindings.add(new PayloadWithId(token.getContentId(), payload));
    return createStreamStructureFromToken(token);
  }

  private StreamFeature getRootFeature() {
    return StreamFeature.newBuilder().setContentId(rootContentId).build();
  }

  private ProcessNextChild incrementChildCountProcessor() {
    return new ProcessNextChild() {
      @Override
      public void processNextChild(ModelChild child) {
        cursorCount++;
      }
    };
  }
}
