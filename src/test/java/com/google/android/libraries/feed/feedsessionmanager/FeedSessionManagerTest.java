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

package com.google.android.libraries.feed.feedsessionmanager;

import static com.google.android.libraries.feed.api.store.Store.HEAD;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelError.ErrorType;
import com.google.android.libraries.feed.api.modelprovider.ModelMutation;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedLifecycleListener.LifecycleEvent;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.model.ModelFilter;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FeedSessionManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedSessionManagerTest {

  private static final MutationContext EMPTY_MUTATION = new MutationContext.Builder().build();
  private static final ContentId SHARED_STATE_ID =
      ContentId.newBuilder()
          .setContentDomain("piet-shared-state")
          .setId(1)
          .setTable("piet-shared-state")
          .build();

  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final String rootContentId = idGenerators.createRootContentId(0);

  private MainThreadRunner mainThreadRunner;
  private FeedAppLifecycleListener appLifecycleListener;
  private TaskQueue taskQueue;
  @Mock private ThreadUtils threadUtils;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;
  @Mock private SchedulerApi schedulerApi;
  @Mock private ModelProvider modelProvider;
  @Mock private ModelMutation modelMutation;
  @Mock private ModelFilter modelFilter;
  @Mock private Store store;
  @Mock private Clock clock;

  @Captor private ArgumentCaptor<Consumer<Result<List<StreamDataOperation>>>> resultCapture;

  private final ContentIdGenerators contentIdGenerators = new ContentIdGenerators();
  private final TimingUtils timingUtils = new TimingUtils();
  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setUp() {
    initMocks(this);
    mainThreadRunner = new MainThreadRunner();
    appLifecycleListener = new FeedAppLifecycleListener(threadUtils);
    when(store.getHeadSession()).thenReturn(HEAD);
    when(modelProvider.edit()).thenReturn(modelMutation);
    when(store.triggerContentGc(anyObject(), anyObject())).thenReturn(() -> {});
    when(store.getSharedStates()).thenReturn(Result.success(new ArrayList<>()));
    when(store.getStreamStructures(any(StreamSession.class)))
        .thenReturn(Result.success(new ArrayList<>()));
    when(store.getPayloads(any())).thenReturn(Result.success(new ArrayList<>()));
    when(store.getAllSessions()).thenReturn(Result.success(new ArrayList<>()));
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.NO_REQUEST_WITH_CONTENT);
    taskQueue = new TaskQueue(MoreExecutors.newDirectExecutorService(), clock, false);
  }

  @Test
  public void testInitialization() {
    StreamSharedState sharedState =
        StreamSharedState.newBuilder()
            .setContentId(idGenerators.createFeatureContentId(0))
            .setPietSharedStateItem(PietSharedStateItem.getDefaultInstance())
            .build();
    List<StreamSharedState> sharedStates = new ArrayList<>();
    sharedStates.add(sharedState);
    when(store.getSharedStates()).thenReturn(Result.success(sharedStates));

    List<StreamStructure> streamStructures = new ArrayList<>();
    StreamStructure operation =
        StreamStructure.newBuilder()
            .setContentId(idGenerators.createFeatureContentId(0))
            .setOperation(StreamStructure.Operation.UPDATE_OR_APPEND)
            .build();
    streamStructures.add(operation);
    com.google.android.libraries.feed.api.store.SessionMutation sessionMutation =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.getStreamStructures(any(StreamSession.class)))
        .thenReturn(Result.success(streamStructures));
    when(store.editSession(HEAD)).thenReturn(sessionMutation);
    when(sessionMutation.commit()).thenReturn(true);

    FeedSessionManager sessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                mainThreadRunner,
                protocolAdapter,
                requestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    assertThat(sessionManager.initialized.get()).isFalse();
    sessionManager.initialize();
    assertThat(sessionManager.initialized.get()).isTrue();

    Map<String, StreamSharedState> sharedStateCache = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStateCache).hasSize(sharedStates.size());

    Map<String, Session> sessions = sessionManager.getSessionsMapForTest();
    Session head = sessions.get(HEAD.getStreamToken());
    assertThat(head).isInstanceOf(HeadSessionImpl.class);
    String itemKey = idGenerators.createFeatureContentId(0);
    Set<String> content = ((HeadSessionImpl) head).getContentInSessionForTest();
    assertThat(content).contains(itemKey);
    assertThat(content).hasSize(streamStructures.size());
  }

  @Test
  public void testLifecycleInitialization() {
    FeedSessionManager sessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                mainThreadRunner,
                protocolAdapter,
                requestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    assertThat(sessionManager.initialized.get()).isFalse();
    sessionManager.onLifecycleEvent(LifecycleEvent.INITIALIZE);
    assertThat(sessionManager.initialized.get()).isTrue();
    sessionManager.onLifecycleEvent(LifecycleEvent.INITIALIZE);
    assertThat(sessionManager.initialized.get()).isTrue();
  }

  @Test
  public void testSessionWithContent() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    int featureCnt = 3;
    populateSession(sessionManager, featureCnt, 1, true, null);

    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(
            sessionManager, threadUtils, timingUtils, mainThreadRunner, modelFilter, configuration);
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getRootFeature()).isNotNull();

    ModelCursor cursor = modelProvider.getRootFeature().getCursor();
    int cursorCount = 0;
    while (cursor.getNextItem() != null) {
      cursorCount++;
    }
    assertThat(cursorCount).isEqualTo(featureCnt);

    // append a couple of others
    populateSession(sessionManager, featureCnt, featureCnt + 1, false, null);

    cursor = modelProvider.getRootFeature().getCursor();
    cursorCount = 0;
    while (cursor.getNextItem() != null) {
      cursorCount++;
    }
    assertThat(cursorCount).isEqualTo(featureCnt * 2);
  }

  @Test
  public void testNoCardsError() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.noCardsError = new ModelError(ErrorType.NO_CARDS_ERROR, null);
    int featureCnt = 2;
    populateSession(sessionManager, featureCnt, 1, true, null);

    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(
            sessionManager, threadUtils, timingUtils, mainThreadRunner, modelFilter, configuration);
    // TODO: We can't verify that raiseError was called on ModelProvider since all of that
    // happens in the createNew() call due to the single threaded nature.  We may want to have a
    // test method on the factory to install the observer when creating the ModelProvider.
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    // Verify the failed session is correct
    Map<String, Session> sessions = sessionManager.getSessionsMapForTest();
    assertThat(sessions).hasSize(2);
    Session session = sessions.get(modelProvider.getSessionToken());
    assertThat(session).isNotNull();
  }

  @Test
  public void testModelErrorObserver() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    // verify this runs.  Another method that can'be be verified on a single thread since
    // the noCardsError will be set and unset.
    sessionManager.modelErrorObserver(null, new ModelError(ErrorType.NO_CARDS_ERROR, null));
  }

  @Test
  public void testReset() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    int featureCnt = 3;
    int fullFeatureCount = populateSession(sessionManager, featureCnt, 1, true, null);
    assertThat(fullFeatureCount).isEqualTo(featureCnt + 1);

    fullFeatureCount = populateSession(sessionManager, featureCnt, 1, true, null);
    assertThat(fullFeatureCount).isEqualTo(featureCnt + 1);
  }

  @Test
  public void testHandleToken() {
    ByteString bytes = ByteString.copyFrom("continuation", Charset.defaultCharset());
    StreamToken streamToken =
        StreamToken.newBuilder().setNextPageToken(bytes).setParentId(rootContentId).build();
    FeedSessionManager sessionManager = getInitializedSessionManager();
    StreamSession streamSession = StreamSession.getDefaultInstance();
    sessionManager.handleToken(streamSession, streamToken);
    verify(requestManager).loadMore(eq(streamToken), resultCapture.capture());
  }

  @Test
  public void testForceRefresh() {
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken("session:1").build();
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.triggerRefresh(streamSession);
    verify(requestManager)
        .triggerRefresh(eq(RequestReason.MANUAL_REFRESH), resultCapture.capture());
  }

  @Test
  public void testGetSharedState() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    String sharedStateId = idGenerators.createSharedStateContentId(0);

    populateSession(sessionManager, 3, 1, true, sharedStateId);
    when(protocolAdapter.getStreamContentId(SHARED_STATE_ID)).thenReturn(sharedStateId);
    assertThat(sessionManager.getSharedState(SHARED_STATE_ID)).isNotNull();

    // test the null condition
    ContentId undefinedSharedStateId =
        ContentId.newBuilder()
            .setContentDomain("shared-state")
            .setId(5)
            .setTable("shared-states")
            .build();
    String undefinedStreamSharedStateId =
        idGenerators.createSharedStateContentId(undefinedSharedStateId.getId());
    when(protocolAdapter.getStreamContentId(undefinedSharedStateId))
        .thenReturn(undefinedStreamSharedStateId);
    assertThat(sessionManager.getSharedState(undefinedSharedStateId)).isNull();
  }

  @Test
  public void testEdit_semanticProperties() {
    com.google.android.libraries.feed.api.store.SessionMutation session =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.editSession(any(StreamSession.class))).thenReturn(session);
    ContentMutation content = mock(ContentMutation.class);
    when(store.editContent()).thenReturn(content);

    SemanticPropertiesMutation semanticPropertiesMutation = mock(SemanticPropertiesMutation.class);
    when(store.editSemanticProperties()).thenReturn(semanticPropertiesMutation);

    FeedSessionManager sessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                mainThreadRunner,
                protocolAdapter,
                requestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    sessionManager.initialize();

    ByteString semanticData = ByteString.copyFromUtf8("helloWorld");
    StreamDataOperation streamDataOperation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.newBuilder().setSemanticData(semanticData))
            .setStreamStructure(
                StreamStructure.newBuilder()
                    .setContentId(rootContentId)
                    .setOperation(Operation.UPDATE_OR_APPEND))
            .build();

    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(EMPTY_MUTATION);
    List<StreamDataOperation> operations = new ArrayList<>();
    operations.add(streamDataOperation);
    Result<List<StreamDataOperation>> result = Result.success(operations);
    updateConsumer.accept(result);

    verify(semanticPropertiesMutation).add(rootContentId, semanticData);
    verify(semanticPropertiesMutation).commit();
  }

  @Test
  public void testSwitchToEphemeralMode() {
    FeedStore spyFeedStore = getSpyFeedStore();
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(spyFeedStore);

    spySessionManager.switchToEphemeralMode("An Error Message");
    verify(spyFeedStore).switchToEphemeralMode();
    verify(spySessionManager).onSwitchToEphemeralMode();
  }

  @Test
  public void testOnSwitchToEphemeralMode() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    String sharedStateId = idGenerators.createSharedStateContentId(0);

    int featureCount = 3;
    populateSession(sessionManager, featureCount, 1, true, sharedStateId);

    Map<String, StreamSharedState> sharedStates = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStates).hasSize(1);
    Map<String, Session> sessions = sessionManager.getSessionsMapForTest();
    assertThat(sessions).hasSize(1);
    Session session = sessions.get(Store.HEAD.getStreamToken());
    assertThat(session).isNotNull();
    Set<String> contentInSession = session.getContentInSession();
    assertThat(contentInSession).hasSize(featureCount + 1);

    sessionManager.onSwitchToEphemeralMode();

    sharedStates = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStates).hasSize(0);
    sessions = sessionManager.getSessionsMapForTest();
    assertThat(sessions).hasSize(1);
    session = sessions.get(Store.HEAD.getStreamToken());
    assertThat(session).isNotNull();
    contentInSession = session.getContentInSession();
    assertThat(contentInSession).hasSize(0);
  }

  @Test
  public void testErrors_initializationSharedStateError() {
    Store errorStore = mock(Store.class);
    when(errorStore.getSharedStates()).thenReturn(Result.failure());
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(errorStore);

    verify(spySessionManager).switchToEphemeralMode(anyString());
    verify(errorStore).switchToEphemeralMode();
  }

  @Test
  public void testErrors_initializationStreamStructureError() {
    Store errorStore = mock(Store.class);
    when(errorStore.getHeadSession()).thenReturn(HEAD);
    when(errorStore.getSharedStates()).thenReturn(Result.success(new ArrayList<>()));
    when(errorStore.getStreamStructures(any(StreamSession.class))).thenReturn(Result.failure());
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(errorStore);

    verify(spySessionManager).switchToEphemeralMode(anyString());
  }

  @Test
  public void testErrors_createNewSessionError() {
    FeedStore spyFeedStore = getSpyFeedStore();
    doReturn(Result.failure()).when(spyFeedStore).createNewSession();
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(spyFeedStore);
    populateSession(spySessionManager, 5, 1, true, null);

    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(
            spySessionManager,
            threadUtils,
            timingUtils,
            mainThreadRunner,
            modelFilter,
            configuration);
    modelProviderFactory.createNew(null);
    verify(spySessionManager).switchToEphemeralMode(anyString());
  }

  @Test
  public void testErrors_getStreamStructuresError() {
    FeedStore spyFeedStore = getSpyFeedStore();
    doReturn(Result.failure())
        .when(spyFeedStore)
        .getStreamStructures(AdditionalMatchers.not(eq(Store.HEAD)));
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(spyFeedStore);
    populateSession(spySessionManager, 5, 1, true, null);

    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(
            spySessionManager,
            threadUtils,
            timingUtils,
            mainThreadRunner,
            modelFilter,
            configuration);
    modelProviderFactory.createNew(null);
    verify(spySessionManager).switchToEphemeralMode(anyString());
  }

  @Test
  public void testInvalidateHead() {
    FeedStore spyFeedStore = getSpyFeedStore();
    FeedSessionManager spySessionManager = getInitializedSpySessionManager(spyFeedStore);
    spySessionManager.invalidateHead();
    verify(spySessionManager).resetHead(null);
    verify(spyFeedStore).clearHead();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderInitializing() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.INITIALIZING);
    assertThat(sessionManager.shouldInvalidateSession(null, modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderInvalidated() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.INVALIDATED);
    assertThat(sessionManager.shouldInvalidateSession(null, modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderReady() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    assertThat(sessionManager.shouldInvalidateSession(null, modelProvider)).isTrue();
  }

  @Test
  public void testShouldInvalidateSession_noModelProviderSession() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken("session:2").build();
    assertThat(sessionManager.shouldInvalidateSession(streamSession, modelProvider)).isTrue();
  }

  @Test
  public void testShouldInvalidateSession_differentSession() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    String sessionToken = "session:1";
    modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    when(modelProvider.getSessionToken()).thenReturn(sessionToken);
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken("session:2").build();
    assertThat(sessionManager.shouldInvalidateSession(streamSession, modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_sameSession() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    String sessionToken = "session:1";
    modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    when(modelProvider.getSessionToken()).thenReturn(sessionToken);
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken(sessionToken).build();
    assertThat(sessionManager.shouldInvalidateSession(streamSession, modelProvider)).isTrue();
  }

  private int populateSession(
      FeedSessionManager sessionManager,
      int featureCnt,
      int idStart,
      boolean reset,
      /*@Nullable*/ String sharedStateId) {
    int operationCount = 0;

    InternalProtocolBuilder internalProtocolBuilder = new InternalProtocolBuilder();
    if (reset) {
      internalProtocolBuilder.addClearOperation().addRootFeature();
      operationCount++;
    }
    for (int i = 0; i < featureCnt; i++) {
      internalProtocolBuilder.addFeature(
          contentIdGenerators.createFeatureContentId(idStart++),
          idGenerators.createRootContentId(0));
      operationCount++;
    }
    if (sharedStateId != null) {
      internalProtocolBuilder.addSharedState(sharedStateId);
      operationCount++;
    }
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(EMPTY_MUTATION);
    List<StreamDataOperation> operations = new ArrayList<>();
    operations.addAll(internalProtocolBuilder.build());
    Result<List<StreamDataOperation>> result = Result.success(operations);
    updateConsumer.accept(result);
    return operationCount;
  }

  private FeedSessionManager getInitializedSessionManager() {
    FeedStore store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            new InMemoryContentStorage(),
            new InMemoryJournalStorage(threadUtils),
            threadUtils,
            mainThreadRunner,
            taskQueue,
            clock);
    FeedSessionManager fsm =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                mainThreadRunner,
                protocolAdapter,
                requestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    fsm.initialize();
    return fsm;
  }

  private FeedStore getSpyFeedStore() {
    return spy(
        new FeedStore(
            timingUtils,
            extensionRegistry,
            new InMemoryContentStorage(),
            new InMemoryJournalStorage(threadUtils),
            threadUtils,
            mainThreadRunner,
            taskQueue,
            clock));
  }

  private FeedSessionManager getInitializedSpySessionManager(Store feedStore) {
    FeedSessionManager spySessionManager =
        spy(
            new FeedSessionManagerFactory(
                    taskQueue,
                    feedStore,
                    timingUtils,
                    threadUtils,
                    mainThreadRunner,
                    protocolAdapter,
                    requestManager,
                    schedulerApi,
                    configuration,
                    clock,
                    appLifecycleListener)
                .create());
    spySessionManager.initialize();
    return spySessionManager;
  }
}
