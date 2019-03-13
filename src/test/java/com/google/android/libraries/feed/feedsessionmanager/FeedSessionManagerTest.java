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
import static org.mockito.Matchers.anySetOf;
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
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.ActionUploadRequestManager;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.store.ActionPropertiesMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedLifecycleListener.LifecycleEvent;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager.SessionMutationTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.feedstore.testing.DelegatingStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.android.libraries.feed.host.storage.CommitResult;
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
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.feed.client.StreamDataProto.UiContext;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.OpaqueActionDataForTestProto.OpaqueActionDataForTest;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
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
  private static final String SESSION_ID = "session:1";

  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final String rootContentId = idGenerators.createRootContentId(0);

  private MainThreadRunner mainThreadRunner;
  private FeedAppLifecycleListener appLifecycleListener;
  private TaskQueue taskQueue;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private ThreadUtils threadUtils;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;
  @Mock private ActionUploadRequestManager actionUploadRequestManager;
  @Mock private SchedulerApi schedulerApi;
  @Mock private ModelProvider modelProvider;
  @Mock private ModelMutation modelMutation;
  @Mock private Store store;
  @Mock private Clock clock;
  @Captor private ArgumentCaptor<Consumer<Result<ConsistencyToken>>> tokenCapture;
  @Captor private ArgumentCaptor<Consumer<Result<List<StreamDataOperation>>>> resultCapture;

  private final ContentIdGenerators contentIdGenerators = new ContentIdGenerators();
  private final TimingUtils timingUtils = new TimingUtils();
  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final Configuration configuration =
      new Configuration.Builder().put(ConfigKey.UNDOABLE_ACTIONS_ENABLED, true).build();

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
    when(store.getAllUploadableActions()).thenReturn(Result.success(new HashSet<>()));
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.NO_REQUEST_WITH_CONTENT);
    taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
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
                protocolAdapter,
                requestManager,
                actionUploadRequestManager,
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
    Session head = sessions.get(HEAD.getSessionId());
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
                protocolAdapter,
                requestManager,
                actionUploadRequestManager,
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

    ModelProvider modelProvider = getModelProvider(sessionManager);
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
  public void testNoRequestWithContent_populateIsUserFacing() {
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.NO_REQUEST_WITH_CONTENT);

    FeedSessionManager sessionManager = getInitializedSessionManager();
    populateSession(sessionManager, 3, 1, true, null);
    taskQueue.resetCounts();

    // Population will happen in a user-facing task and no request is sent.
    ModelProvider modelProvider = getModelProvider(sessionManager);
    assertThat(modelProvider).isNotNull();
    assertThat(taskQueue.getImmediateTaskCount()).isEqualTo(0);
    assertThat(taskQueue.getBackgroundTaskCount()).isEqualTo(0);
    assertThat(taskQueue.getUserFacingTaskCount()).isEqualTo(1);
    assertThat(taskQueue.isMakingRequest()).isFalse();
  }

  @Test
  public void testRequestWithContent_populateIsImmediate() {
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_CONTENT);

    FeedSessionManager sessionManager = getInitializedSessionManager();
    populateSession(sessionManager, 3, 1, true, null);
    taskQueue.resetCounts();

    // Population will happen immediately and a request is sent.
    ModelProvider modelProvider = getModelProvider(sessionManager);
    assertThat(modelProvider).isNotNull();
    assertThat(taskQueue.getImmediateTaskCount()).isEqualTo(1);
    assertThat(taskQueue.getBackgroundTaskCount()).isEqualTo(0);
    assertThat(taskQueue.getUserFacingTaskCount()).isEqualTo(0);
    assertThat(taskQueue.isMakingRequest()).isTrue();
  }

  @Test
  public void testRequestWithWait_populateIsUserFacing() {
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_WAIT);

    FeedSessionManager sessionManager = getInitializedSessionManager();
    populateSession(sessionManager, 3, 1, true, null);
    taskQueue.resetCounts();

    // Population will happen in a user-facing task and a request is sent.
    ModelProvider modelProvider = getModelProvider(sessionManager);
    assertThat(modelProvider).isNotNull();
    assertThat(taskQueue.getImmediateTaskCount()).isEqualTo(0);
    assertThat(taskQueue.getBackgroundTaskCount()).isEqualTo(0);
    assertThat(taskQueue.getUserFacingTaskCount()).isEqualTo(1);
    assertThat(taskQueue.isMakingRequest()).isTrue();
  }

  @Test
  public void testNoCardsError() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.noCardsError = new ModelError(ErrorType.NO_CARDS_ERROR, null);
    int featureCnt = 2;
    populateSession(sessionManager, featureCnt, 1, true, null);

    // We can't verify that raiseError was called on ModelProvider since all of that happens in
    // the createNew() call due to the single threaded nature.  We may want to have a test method
    // on the factory to install the observer when creating the ModelProvider.
    ModelProvider modelProvider = getModelProvider(sessionManager);

    // Verify the failed session is correct
    Map<String, Session> sessions = sessionManager.getSessionsMapForTest();
    assertThat(sessions).hasSize(2);
    Session session = sessions.get(modelProvider.getSessionId());
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
    sessionManager.handleToken(SESSION_ID, streamToken);
    verify(requestManager).loadMore(eq(streamToken), resultCapture.capture());
    verify(actionUploadRequestManager)
        .triggerUploadActions(
            anySetOf(StreamUploadableAction.class),
            any(ConsistencyToken.class),
            tokenCapture.capture());
  }

  @Test
  public void testForceRefresh() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.triggerRefresh(
        SESSION_ID, RequestReason.UNKNOWN, UiContext.getDefaultInstance());
    verify(requestManager).triggerRefresh(eq(RequestReason.UNKNOWN), resultCapture.capture());

    verify(actionUploadRequestManager)
        .triggerUploadActions(
            anySetOf(StreamUploadableAction.class),
            any(ConsistencyToken.class),
            tokenCapture.capture());
  }

  @Test
  public void testForceRefresh_scheduledRefresh() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.triggerRefresh(
        SESSION_ID, RequestReason.HOST_REQUESTED, UiContext.getDefaultInstance());
    verify(requestManager)
        .triggerRefresh(eq(RequestReason.HOST_REQUESTED), resultCapture.capture());

    verify(actionUploadRequestManager)
        .triggerUploadActions(
            anySetOf(StreamUploadableAction.class),
            any(ConsistencyToken.class),
            tokenCapture.capture());
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
  public void testUpdateConsumer() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    assertThat(sessionManager.outstandingMutations).isEmpty();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(EMPTY_MUTATION);
    assertThat(updateConsumer).isInstanceOf(SessionMutationTracker.class);
    assertThat(sessionManager.outstandingMutations).hasSize(1);
    assertThat(sessionManager.outstandingMutations).contains(updateConsumer);
    updateConsumer.accept(Result.success(new ArrayList<>()));
    assertThat(sessionManager.outstandingMutations).isEmpty();
  }

  @Test
  public void testUpdateConsumer_clearAll() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    assertThat(sessionManager.outstandingMutations).isEmpty();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(EMPTY_MUTATION);
    assertThat(sessionManager.outstandingMutations).hasSize(1);
    appLifecycleListener.onClearAll();
    assertThat(sessionManager.outstandingMutations).isEmpty();

    // verify this still runs (as a noop)
    updateConsumer.accept(Result.success(new ArrayList<>()));
    assertThat(sessionManager.outstandingMutations).isEmpty();
  }

  @Test
  public void testUpdateConsumer_clearAllWithRefresh() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    assertThat(sessionManager.outstandingMutations).isEmpty();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(EMPTY_MUTATION);
    assertThat(sessionManager.outstandingMutations).hasSize(1);
    appLifecycleListener.onClearAllWithRefresh();
    assertThat(sessionManager.outstandingMutations).isEmpty();

    // verify this still runs (as a noop)
    updateConsumer.accept(Result.success(new ArrayList<>()));
    assertThat(sessionManager.outstandingMutations).isEmpty();
  }

  @Test
  public void testEdit_actionProperties() {
    com.google.android.libraries.feed.api.store.SessionMutation session =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.editSession(any(StreamSession.class))).thenReturn(session);
    ContentMutation content = mock(ContentMutation.class);
    when(content.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editContent()).thenReturn(content);
    SemanticPropertiesMutation semanticPropertiesMutation = mock(SemanticPropertiesMutation.class);
    when(semanticPropertiesMutation.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editSemanticProperties()).thenReturn(semanticPropertiesMutation);
    ActionPropertiesMutation actionPropertiesMutation = mock(ActionPropertiesMutation.class);
    when(actionPropertiesMutation.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editActionProperties()).thenReturn(actionPropertiesMutation);

    FeedSessionManager sessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                protocolAdapter,
                requestManager,
                actionUploadRequestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    sessionManager.initialize();

    OpaqueActionData actionData =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId("id").build())
            .build();
    StreamDataOperation streamDataOperation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.newBuilder().setActionData(actionData))
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

    verify(actionPropertiesMutation).add(rootContentId, actionData);
    verify(actionPropertiesMutation).commit();
  }

  @Test
  public void testEdit_semanticProperties() {
    com.google.android.libraries.feed.api.store.SessionMutation session =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.editSession(any(StreamSession.class))).thenReturn(session);
    ContentMutation content = mock(ContentMutation.class);
    when(content.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editContent()).thenReturn(content);
    ActionPropertiesMutation actionPropertiesMutation = mock(ActionPropertiesMutation.class);
    when(actionPropertiesMutation.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editActionProperties()).thenReturn(actionPropertiesMutation);
    SemanticPropertiesMutation semanticPropertiesMutation = mock(SemanticPropertiesMutation.class);
    when(semanticPropertiesMutation.commit()).thenReturn(CommitResult.SUCCESS);
    when(store.editSemanticProperties()).thenReturn(semanticPropertiesMutation);

    FeedSessionManager sessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                protocolAdapter,
                requestManager,
                actionUploadRequestManager,
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
    Store store = mock(Store.class);
    FeedSessionManager sessionManager = getUninitializedSessionManager(store);
    sessionManager.switchToEphemeralMode("An Error Message");
    verify(store).switchToEphemeralMode();
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
    Session session = sessions.get(Store.HEAD.getSessionId());
    assertThat(session).isNotNull();
    Set<String> contentInSession = session.getContentInSession();
    assertThat(contentInSession).hasSize(featureCount + 1);

    sessionManager.onSwitchToEphemeralMode();

    sharedStates = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStates).hasSize(0);
    sessions = sessionManager.getSessionsMapForTest();
    assertThat(sessions).hasSize(1);
    session = sessions.get(Store.HEAD.getSessionId());
    assertThat(session).isNotNull();
    contentInSession = session.getContentInSession();
    assertThat(contentInSession).hasSize(0);
  }

  @Test
  public void testErrors_initializationSharedStateError() {
    Store store = mock(Store.class);
    when(store.getSharedStates()).thenReturn(Result.failure());
    FeedSessionManager spySessionManager = getUninitializedSessionManager(store);
    spySessionManager.initialize();
    verify(store).switchToEphemeralMode();
  }

  @Test
  public void testErrors_initializationStreamStructureError() {
    Store store = mock(Store.class);
    when(store.getHeadSession()).thenReturn(HEAD);
    when(store.getSharedStates()).thenReturn(Result.success(new ArrayList<>()));
    when(store.getStreamStructures(any(StreamSession.class))).thenReturn(Result.failure());
    FeedSessionManager spySessionManager = getUninitializedSessionManager(store);
    spySessionManager.initialize();
    verify(store).switchToEphemeralMode();
  }

  @Test
  public void testErrors_createNewSessionError() {
    Store spyFeedStore = getSpyFeedStore();
    doReturn(Result.failure()).when(spyFeedStore).createNewSession();
    FeedSessionManager sessionManager = getUninitializedSessionManager(spyFeedStore);
    sessionManager.initialize();
    populateSession(sessionManager, 5, 1, true, null);

    ModelProvider unused = getModelProvider(sessionManager);
    verify(spyFeedStore).switchToEphemeralMode();
  }

  @Test
  public void testErrors_getStreamStructuresError() {
    Store spyFeedStore = getSpyFeedStore();
    doReturn(Result.failure())
        .when(spyFeedStore)
        .getStreamStructures(AdditionalMatchers.not(eq(Store.HEAD)));
    FeedSessionManager sessionManager = getUninitializedSessionManager(spyFeedStore);
    sessionManager.initialize();
    populateSession(sessionManager, 5, 1, true, null);

    ModelProvider unused = getModelProvider(sessionManager);
    verify(spyFeedStore).switchToEphemeralMode();
  }

  @Test
  public void testTriggerUploadActions() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    actionSet.add(StreamUploadableAction.getDefaultInstance());
    ConsistencyToken token =
        ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x1, 0xf})).build();
    sessionManager.getConsistencyTokenConsumer().accept(Result.success(token));
    sessionManager.triggerUploadActions(actionSet);
    verify(actionUploadRequestManager)
        .triggerUploadActions(eq(actionSet), eq(token), tokenCapture.capture());
  }

  @Test
  public void testGetConsistencyToken() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    ConsistencyToken token =
        ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x1, 0xf})).build();
    sessionManager.getConsistencyTokenConsumer().accept(Result.success(token));
    assertThat(sessionManager.getConsistencyToken()).isEqualTo(token);
  }

  @Test
  public void testGetConsistencyTokenEmpty() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    assertThat(sessionManager.getConsistencyToken())
        .isEqualTo(ConsistencyToken.getDefaultInstance());
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

  private ModelProvider getModelProvider(FeedSessionManager sessionManager) {
    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(
            sessionManager, threadUtils, timingUtils, taskQueue, mainThreadRunner, configuration);
    return modelProviderFactory.createNew(null);
  }

  private FeedSessionManager getInitializedSessionManager() {
    FeedStore store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            new InMemoryContentStorage(),
            new InMemoryJournalStorage(),
            threadUtils,
            taskQueue,
            clock,
            basicLoggingApi,
            FakeMainThreadRunner.runTasksImmediately());
    FeedSessionManager fsm =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                protocolAdapter,
                requestManager,
                actionUploadRequestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    fsm.initialize();
    return fsm;
  }

  private Store getSpyFeedStore() {
    return spy(
        new DelegatingStore(
            new FeedStore(
                timingUtils,
                extensionRegistry,
                new InMemoryContentStorage(),
                new InMemoryJournalStorage(),
                threadUtils,
                taskQueue,
                clock,
                basicLoggingApi,
                FakeMainThreadRunner.runTasksImmediately())));
  }

  private FeedSessionManager getUninitializedSessionManager(Store feedStore) {
    return new FeedSessionManagerFactory(
            taskQueue,
            feedStore,
            timingUtils,
            threadUtils,
            protocolAdapter,
            requestManager,
            actionUploadRequestManager,
            schedulerApi,
            configuration,
            clock,
            appLifecycleListener)
        .create();
  }
}
