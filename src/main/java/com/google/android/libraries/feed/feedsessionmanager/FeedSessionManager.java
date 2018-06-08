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

import static com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior.NO_REQUEST_WITH_TIMEOUT;
import static com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior.REQUEST_WITH_CONTENT;
import static com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior.REQUEST_WITH_TIMEOUT;
import static com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior.REQUEST_WITH_WAIT;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.SimpleSettableFuture;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.logging.StringFormattingUtils;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.ContentCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.InitializableSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.TaskQueue;
import com.google.android.libraries.feed.feedsessionmanager.internal.TaskQueue.TaskType;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Prototype implementation of the SessionManager. All state is kept in memory. */
public class FeedSessionManager implements SessionManager, Dumpable {

  private static final String TAG = "FeedSessionManager";

  private static final long DEFAULT_LIFETIME_MS = TimeUnit.HOURS.toMillis(1);
  private static final long TIMEOUT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);

  // For the Shared State we will always cache them in the Session Manager
  // Accessed on main thread, updated on Executor task
  private final Map<String, StreamSharedState> sharedStateCache = new HashMap<>();

  // All writes are done on background threads, there are accesses on the main thread.  Leaving
  // the lock back accessibleContentSupplier may eventually run on a background task and not
  // on the executor thread.
  private final SessionCache sessionCache;

  // All access to the content cache happens on the executor thread so there is no need to
  // synchronize access.
  private final ContentCache contentCache;

  private final SessionFactory sessionFactory;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final MainThreadRunner mainThreadRunner;
  private final ProtocolAdapter protocolAdapter;
  private final RequestManager requestManager;
  private final SchedulerApi schedulerApi;
  private final TaskQueue taskQueue;
  private final Clock clock;
  private final long sessionPopulationTimeoutMs;

  // operation counts for the dumper
  private int commitCount = 0;
  private int newSessionCount = 0;
  private int existingSessionCount = 0;
  private int handleTokenCount = 0;

  public FeedSessionManager(
      ExecutorService executor,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      MainThreadRunner mainThreadRunner,
      ProtocolAdapter protocolAdapter,
      RequestManager requestManager,
      SchedulerApi schedulerApi,
      Configuration configuration,
      Clock clock) {
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.mainThreadRunner = mainThreadRunner;
    this.protocolAdapter = protocolAdapter;
    this.requestManager = requestManager;
    this.schedulerApi = schedulerApi;
    this.clock = clock;

    this.taskQueue = new TaskQueue(executor);
    long lifetimeMs =
        configuration.getValueOrDefault(ConfigKey.SESSION_LIFETIME_MS, DEFAULT_LIFETIME_MS);
    sessionFactory = new SessionFactory(store, taskQueue, timingUtils, threadUtils, configuration);
    this.sessionCache =
        new SessionCache(
            store, taskQueue, sessionFactory, lifetimeMs, timingUtils, threadUtils, clock);

    sessionPopulationTimeoutMs =
        configuration.getValueOrDefault(ConfigKey.TIMEOUT_TIMEOUT_MS, TIMEOUT_TIMEOUT_MS);
    contentCache = new ContentCache();
  }

  /**
   * Called to initialize the session manager. This creates an executor task which does the actual
   * work of setting up the current state. If this is not called, the session manager will not
   * populate new or existing sessions. There isn't error checking on this since this happens on an
   * executor task.
   */
  public void initialize() {
    taskQueue.initialize(this::initializationTask);
  }

  // Task which initializes the Session Manager.  This must be the first task run on the
  // Session Manager thread so it's complete before we create any sessions.
  private void initializationTask() {
    threadUtils.checkNotMainThread();
    Logger.i(TAG, "Task: Initialization");
    timingUtils.pinThread(Thread.currentThread(), "SessionManager");
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    // Initialize the Shared States cached here.
    ElapsedTimeTracker sharedStateTimeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Result<List<StreamSharedState>> sharedStatesResult = store.getSharedStates();
    if (sharedStatesResult.isSuccessful()) {
      for (StreamSharedState sharedState : sharedStatesResult.getValue()) {
        sharedStateCache.put(sharedState.getContentId(), sharedState);
      }
    } else {
      // without shared states we need to switch to ephemeral mode
      switchToEphemeralMode("SharedStates failed to load, no shared states are loaded.");
      taskQueue.resetTaskQueue();
      timeTracker.stop("task", "Initialization", "error", "switchToEphemeralMode");
      return;
    }
    sharedStateTimeTracker.stop("", "sharedStateTimeTracker");

    // create the head session from the data in the Store
    if (!sessionCache.initialize()) {
      // we failed to initialize the sessionCache, so switch to ephemeral mode.
      switchToEphemeralMode("unable to initialize the sessionCache");
      timeTracker.stop("task", "Initialization", "error", "switchToEphemeralMode");
      return;
    }
    timeTracker.stop("task", "Initialization");
  }

  @Override
  public void getNewSession(ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    InitializableSession session = sessionFactory.getSession();
    session.bindModelProvider(modelProvider);

    if (!sessionCache.isHeadInitialized()) {
      Logger.i(TAG, "Delaying populateSession until initialization is finished");
      taskQueue.execute(
          "postInitialization/populateSession",
          TaskType.POST_INIT,
          () -> populateSessionWaitForHead(session));
    } else {
      populateSession(session, (result) -> {});
    }
  }

  private void populateSessionWaitForHead(InitializableSession session) {
    threadUtils.checkNotMainThread();

    // Wait for the main thread call to complete.  This is done to ensure that the
    // background populateSession task is queued before the task running this finishes.
    SimpleSettableFuture<Boolean> sharedStatesFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(TAG, () -> populateSession(session, sharedStatesFuture::put));
    try {
      sharedStatesFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "populateSession main thread portion failed");
    }
  }

  // This returns a result through a consumer allowing use to wait on this task.  It must
  // run on the main thread, but does most of it's work through the populateSession task.
  private void populateSession(InitializableSession session, Consumer<Boolean> result) {
    threadUtils.checkMainThread();
    HeadSessionImpl headSession = Validators.checkNotNull(sessionCache.getHead());
    SessionManagerState sessionManagerState =
        new SessionManagerState(
            !headSession.isHeadEmpty(),
            headSession.getStreamSession().getLastAccessed(),
            taskQueue.isMakingRequest());
    @RequestBehavior int behavior = schedulerApi.shouldSessionRequestData(sessionManagerState);

    Runnable timeoutTask = null;
    if (behavior == REQUEST_WITH_TIMEOUT || behavior == NO_REQUEST_WITH_TIMEOUT) {
      timeoutTask = () -> populateSessionTask(session);
    }
    boolean makeRequest =
        (behavior == REQUEST_WITH_TIMEOUT
                || behavior == REQUEST_WITH_WAIT
                || behavior == REQUEST_WITH_CONTENT)
            && !taskQueue.isMakingRequest();
    Logger.i(
        TAG,
        "request behavior: %s, making request %s, timeout task %s",
        getRequestBehavior(behavior),
        makeRequest,
        timeoutTask != null);

    // if we are making a request, there are two orders, request -> populate for all cases except
    // for REQUEST_WITH_CONTENT which is populate -> request
    if (makeRequest && behavior != REQUEST_WITH_CONTENT) {
      // determine if we make the request before or after populating a session
      triggerRefresh(null);
    }

    if (behavior == RequestBehavior.NO_REQUEST_WITH_CONTENT && taskQueue.isMakingRequest()) {
      // TODO: Need to support this
      Logger.i(TAG, "No support for showing content while a request is being made");
    }

    // Task which creates and populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    taskQueue.execute(
        "populateNewSession",
        TaskType.USER_FACING,
        () -> populateSessionTask(session),
        timeoutTask,
        sessionPopulationTimeoutMs);
    if (makeRequest && behavior == REQUEST_WITH_CONTENT) {
      triggerRefresh(null);
    }
    result.accept(true);
  }

  private void populateSessionTask(InitializableSession session) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

    Result<StreamSession> streamSessionResult = store.createNewSession();
    if (!streamSessionResult.isSuccessful()) {
      notifySessionError(session);
      switchToEphemeralMode("Unable to create a new session, createNewSession failed");
      timeTracker.stop("task", "Create/Populate New Session", "Failure", "createNewSession");
      return;
    }
    StreamSession streamSession = streamSessionResult.getValue();
    StreamSession recordedSession =
        streamSession.toBuilder().setLastAccessed(clock.currentTimeMillis()).build();

    Result<List<StreamStructure>> streamStructuresResult = store.getStreamStructures(streamSession);
    if (!streamStructuresResult.isSuccessful()) {
      notifySessionError(session);
      switchToEphemeralMode("Unable to create a new session, getStreamStructures failed");
      timeTracker.stop("task", "Create/Populate New Session", "Failure", "getStreamStructures");
      return;
    }

    boolean cachedBindings;
    cachedBindings = contentCache.size() > 0;
    session.populateModelProvider(
        recordedSession, streamStructuresResult.getValue(), cachedBindings);
    sessionCache.put(session.getStreamSession().getStreamToken(), session);
    newSessionCount++;
    Logger.i(
        TAG,
        "Populate new session: %s, creation time %s",
        session.getStreamSession().getStreamToken(),
        StringFormattingUtils.formatLogDate(session.getStreamSession().getLastAccessed()));
    timeTracker.stop("task", "Create/Populate New Session");
  }

  @VisibleForTesting
  void switchToEphemeralMode(String message) {
    Logger.e(TAG, message);
    // TODO: This needs to call the store method which switches to ephemeral mode
  }

  @VisibleForTesting
  void notifySessionError(Session session) {
    Logger.e(TAG, "notify Session an error has occurred");
    // TODO: We should invalidate the session with an error
  }

  @Override
  public boolean getExistingSession(String sessionToken, ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    Session existingSession = sessionCache.get(sessionToken);
    if (existingSession == null) {
      // We didn't find the existing session so it probably expired and was cleaned up.
      Logger.i(TAG, "getExistingSession didn't find the requested session %s", sessionToken);
      return false;
    }
    InitializableSession session = sessionFactory.getSession();
    session.bindModelProvider(modelProvider);
    ModelProvider existingModelProvider = existingSession.getModelProvider();
    if (existingModelProvider != null) {
      existingModelProvider.invalidate();
    }
    StreamSession streamSession = existingSession.getStreamSession();

    // Task which populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    taskQueue.execute(
        "createExistingSession",
        TaskType.USER_FACING,
        () -> {
          threadUtils.checkNotMainThread();
          Result<List<StreamStructure>> streamStructuresResult =
              store.getStreamStructures(streamSession);
          if (streamStructuresResult.isSuccessful()) {
            session.populateModelProvider(streamSession, streamStructuresResult.getValue(), false);
            sessionCache.put(session.getStreamSession().getStreamToken(), session);
            existingSessionCount++;
          } else {
            Logger.e(
                TAG,
                "unable to get stream structure for existing session %s",
                session.getStreamSession().getStreamToken());
            notifySessionError(session);
            switchToEphemeralMode("unable to get stream structure for existing session");
          }
        });
    return true;
  }

  @Override
  public void invalidateHead() {
    taskQueue.execute("invalidateHead", TaskType.HEAD_INVALIDATE, () -> resetHead(null));
  }

  @Override
  public void handleToken(StreamSession streamSession, StreamToken streamToken) {
    Logger.i(
        TAG,
        "HandleToken on stream %s, token %s",
        streamSession.getStreamToken(),
        streamToken.getContentId());
    threadUtils.checkMainThread();

    // TODO: This should only create the request once.
    handleTokenCount++;
    requestManager.loadMore(
        streamToken,
        new CommitterTask(
            "handleToken",
            new MutationContext.Builder()
                .setContinuationToken(streamToken)
                .setRequestingSession(streamSession)
                .build()));
  }

  @Override
  public void triggerRefresh(/*@Nullable*/ StreamSession streamSession) {
    threadUtils.checkMainThread();
    taskQueue.execute(
        "triggerRefresh/Request/InvalidateSession",
        TaskType.HEAD_INVALIDATE, // invalidate because we are requesting a refresh
        () -> triggerRefreshTask(streamSession));
  }

  private void triggerRefreshTask(/*@Nullable*/ StreamSession streamSession) {
    threadUtils.checkNotMainThread();
    requestManager.triggerRefresh(
        RequestReason.MANUAL_REFRESH,
        new CommitterTask("triggerRefresh", MutationContext.EMPTY_CONTEXT));
    if (streamSession != null) {
      String streamToken = streamSession.getStreamToken();
      Session session = sessionCache.get(streamToken);
      if (session != null) {
        ModelProvider modelProvider = session.getModelProvider();
        if (modelProvider != null) {
          invalidateSession(modelProvider, session);
        } else {
          Logger.w(TAG, "Session didn't have a ModelProvider %s", streamToken);
        }
      } else {
        Logger.w(TAG, "TriggerRefresh didn't find session %s", streamToken);
      }
    }
  }

  @Override
  public void getStreamFeatures(
      List<String> contentIds, Consumer<Result<List<PayloadWithId>>> consumer) {
    threadUtils.checkNotMainThread();
    List<PayloadWithId> results = new ArrayList<>();
    List<String> cacheMisses = new ArrayList<>();
    int contentSize = contentCache.size();
    for (String contentId : contentIds) {
      StreamPayload payload = contentCache.get(contentId);
      if (payload != null) {
        results.add(new PayloadWithId(contentId, payload));
      } else {
        cacheMisses.add(contentId);
      }
    }

    if (!cacheMisses.isEmpty()) {
      Result<List<PayloadWithId>> contentResult = store.getPayloads(cacheMisses);
      if (contentResult.isSuccessful()) {
        results.addAll(contentResult.getValue());
      } else {
        // since we couldn't populate the content, switch to ephemeral mode
        switchToEphemeralMode("Unable to get the payloads in getStreamFeatures");
        consumer.accept(Result.failure());
      }
    }
    Logger.i(
        TAG,
        "Caching getStreamFeatures - items %s, cache misses %s, cache size %s",
        contentIds.size(),
        cacheMisses.size(),
        contentSize);
    consumer.accept(Result.success(results));
  }

  @Override
  /*@Nullable*/
  public StreamSharedState getSharedState(ContentId contentId) {
    threadUtils.checkMainThread();
    String sharedStateId = protocolAdapter.getStreamContentId(contentId);
    StreamSharedState state = sharedStateCache.get(sharedStateId);
    if (state == null) {
      Logger.e(TAG, "Shared State [%s] was not found", sharedStateId);
    }
    return state;
  }

  @Override
  public Consumer<Result<List<StreamDataOperation>>> getUpdateConsumer(
      MutationContext mutationContext) {
    return new CommitterTask("updateConsumer", mutationContext);
  }

  @Override
  public void runTask(String task, Runnable runnable) {
    taskQueue.execute(task, TaskType.USER_FACING, runnable);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("commitCount").value(commitCount);
    dumper.forKey("newSessionCount").value(newSessionCount).compactPrevious();
    dumper.forKey("existingSessionCount").value(existingSessionCount).compactPrevious();
    dumper.forKey("handleTokenCount").value(handleTokenCount).compactPrevious();
    dumper.forKey("sharedStateCount").value(sharedStateCache.size());
    dumper.dump(contentCache);
    dumper.dump(taskQueue);
    dumper.dump(sessionCache);
  }

  /** This runs as a task on the executor thread and also as part of a SessionMutation commit. */
  @VisibleForTesting
  void resetHead(/*@Nullable*/ StreamSession streamSession) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Collection<Session> currentSessions = sessionCache.getSessions();

    // If we have old sessions and we received a clear head, let's invalidate the session that
    // initiated the clear.
    store.clearHead();
    for (Session session : currentSessions) {
      ModelProvider modelProvider = session.getModelProvider();
      if (modelProvider != null
          && session.invalidateOnResetHead()
          && shouldInvalidateSession(streamSession, modelProvider)) {
        invalidateSession(modelProvider, session);
      }
    }
    timeTracker.stop("task", "resetHead");
  }

  /**
   * This will determine if the ModelProvider (session) should be invalidated.
   *
   * <ol>
   *   <li>ModelProvider should be READY
   *   <li>Clearing head was initiated externally, not from an existing session
   *   <li>Clearing head was initiated by the ModelProvider
   *   <li>The ModelProvider doesn't yet have a session, so we'll create the session from the new
   *       $HEAD
   * </ol>
   */
  private boolean shouldInvalidateSession(
      /*@Nullable*/ StreamSession streamSession, ModelProvider modelProvider) {
    // if the model provider isn't READY, don't invalidate the session
    if (modelProvider.getCurrentState() != State.READY) {
      return false;
    }
    // Clear was done outside of any session.
    if (streamSession == null) {
      return true;
    }
    // Invalidate if the ModelProvider doesn't yet have a session or if it matches the session
    // which initiated the request clearing $HEAD.
    String sessionToken = modelProvider.getSessionToken();
    return (sessionToken == null || sessionToken.equals(streamSession.getStreamToken()));
  }

  private void invalidateSession(ModelProvider modelProvider, Session session) {
    threadUtils.checkNotMainThread();
    Logger.i(TAG, "Invalidate session %s", session.getStreamSession().getStreamToken());
    modelProvider.invalidate();
    store.removeSession(session.getStreamSession());
    sessionCache.remove(session.getStreamSession().getStreamToken());
  }

  /**
   * This class will process a List of {@link StreamDataOperation}. The {@link MutationContext} is
   * stored here and contains the context which made the request resulting in the List of data
   * operations.
   */
  private class CommitterTask implements Consumer<Result<List<StreamDataOperation>>> {
    private final List<StreamStructure> streamStructures = new ArrayList<>();
    private final MutationContext mutationContext;
    private final String task;

    private boolean clearedHead = false;
    private List<StreamDataOperation> dataOperations;

    CommitterTask(String task, MutationContext mutationContext) {
      this.task = task;
      this.mutationContext = mutationContext;
    }

    @Override
    public void accept(Result<List<StreamDataOperation>> updateResults) {
      if (!updateResults.isSuccessful()) {
        if (mutationContext.getContinuationToken() != null) {
          Logger.e(
              TAG, "Error found with a token request %s", mutationContext.getContinuationToken());
          Session session = null;
          StreamSession streamSession = mutationContext.getRequestingSession();
          String sessionToken = (streamSession != null) ? streamSession.getStreamToken() : null;
          if (sessionToken != null) {
            session = sessionCache.get(sessionToken);
          }
          if (session != null) {
            notifySessionError(session);
          }
        } else {
          Logger.e(TAG, "Unexpected Update error found, the update is being ignored");
        }
        return;
      }
      dataOperations = updateResults.getValue();
      for (StreamDataOperation operation : dataOperations) {
        if (operation.getStreamStructure().getOperation() == Operation.CLEAR_ALL) {
          clearedHead = true;
          break;
        }
      }
      taskQueue.execute(
          "sessionManagerMutation:" + task + ":clearHead:" + clearedHead,
          clearedHead ? TaskType.HEAD_RESET : TaskType.USER_FACING,
          this::commitTask);
    }

    private void commitTask() {
      contentCache.startMutation();
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      commitContent();
      // TODO: If we are currently delaying creation and we didn't clear head, then this
      // is an update.  Should we delay commitSessionUpdates until head is population?
      commitSessionUpdates();
      commitCount++;
      contentCache.finishMutation();
      timeTracker.stop(
          "task", "sessionMutation.commitTask:" + task, "mutations", streamStructures.size());
    }

    private void commitContent() {
      threadUtils.checkNotMainThread();
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      ContentMutation contentMutation = store.editContent();
      SemanticPropertiesMutation semanticPropertiesMutation = store.editSemanticProperties();
      for (StreamDataOperation dataOperation : dataOperations) {
        Operation operation = dataOperation.getStreamStructure().getOperation();
        if (operation == Operation.CLEAR_ALL) {
          streamStructures.add(dataOperation.getStreamStructure());
          resetHead((mutationContext != null) ? mutationContext.getRequestingSession() : null);
          continue;
        }
        if (operation == Operation.UPDATE_OR_APPEND) {
          if (!validDataOperation(dataOperation)) {
            continue;
          }
          StreamPayload payload = dataOperation.getStreamPayload();
          String contentId = dataOperation.getStreamStructure().getContentId();
          contentCache.put(contentId, payload);
          if (payload.hasStreamSharedState()) {
            contentMutation.add(contentId, payload);
            // cache the shared state here
            sharedStateCache.put(
                dataOperation.getStreamStructure().getContentId(), payload.getStreamSharedState());
          } else if (payload.hasStreamFeature() || payload.hasStreamToken()) {
            // don't add StreamSharedState to the metadata list stored for sessions
            streamStructures.add(dataOperation.getStreamStructure());
            contentMutation.add(contentId, payload);
          } else {
            Logger.e(TAG, "Unsupported UPDATE_OR_APPEND payload");
          }
          continue;
        } else if (operation == Operation.REMOVE) {
          // We don't update the content for REMOVED items, content will be garbage collected.
          // This is required because a session may not remove the item.
          // Add the Remove to the structure changes
          streamStructures.add(dataOperation.getStreamStructure());
          continue;
        }
        if (dataOperation.getStreamPayload().hasSemanticData()) {
          String contentId = dataOperation.getStreamStructure().getContentId();
          semanticPropertiesMutation.add(
              contentId, dataOperation.getStreamPayload().getSemanticData());
          continue;
        }
        Logger.e(
            TAG, "Unsupported Mutation: %s", dataOperation.getStreamStructure().getOperation());
      }
      taskQueue.execute("contentMutation", TaskType.BACKGROUND, contentMutation::commit);
      // TODO: currently there is a small window in which an action could occur and the
      // request be sent to the server before this is written, fix
      taskQueue.execute(
          "semanticPropertiesMutation", TaskType.BACKGROUND, semanticPropertiesMutation::commit);
      timeTracker.stop("", "contentUpdate", "items", dataOperations.size());
    }

    private void commitSessionUpdates() {
      threadUtils.checkNotMainThread();
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      /*@Nullable*/
      StreamToken mutationSourceToken =
          (mutationContext != null) ? mutationContext.getContinuationToken() : null;
      // For sessions we want to add the remove operation if the mutation source was a
      // continuation token.
      if (mutationSourceToken != null) {
        StreamStructure removeOperation = addTokenRemoveOperation(mutationSourceToken);
        if (removeOperation != null) {
          streamStructures.add(0, removeOperation);
        }
      }
      Collection<Session> updates = sessionCache.getSessions();
      HeadSessionImpl head = Validators.checkNotNull(sessionCache.getHead());
      for (Session session : updates) {
        if (clearedHead && session == head) {
          session.updateSession(clearedHead, streamStructures, mutationSourceToken);
          long updateTime = clock.currentTimeMillis();
          Validators.checkNotNull(head).updateAccessTime(updateTime);
          mainThreadRunner.execute(TAG, () -> schedulerApi.onReceiveNewContent(updateTime));
          Logger.i(
              TAG,
              "Cleared Head, new creation time %s",
              StringFormattingUtils.formatLogDate(updateTime));
          continue;
        }
        Logger.i(TAG, "Update Session %s", session.getStreamSession().getStreamToken());
        session.updateSession(clearedHead, streamStructures, mutationSourceToken);
      }
      timeTracker.stop("", "sessionUpdate", "sessions", updates.size());
    }

    private boolean validDataOperation(StreamDataOperation dataOperation) {
      if (!dataOperation.hasStreamPayload() || !dataOperation.hasStreamStructure()) {
        Logger.e(TAG, "Invalid StreamDataOperation - payload or streamStructure not defined");
        return false;
      }
      String contentId = dataOperation.getStreamStructure().getContentId();
      if (TextUtils.isEmpty(contentId)) {
        Logger.i(TAG, "Invalid StreamDataOperation - No ID Found");
        return false;
      }
      return true;
    }

    /*@Nullable*/
    private StreamStructure addTokenRemoveOperation(StreamToken token) {
      return StreamStructure.newBuilder()
          .setContentId(token.getContentId())
          .setParentContentId(token.getParentId())
          .setOperation(Operation.REMOVE)
          .build();
    }
  }

  // This is only used in tests to verify the contents of the shared state cache.
  Map<String, StreamSharedState> getSharedStateCacheForTest() {
    return new HashMap<>(sharedStateCache);
  }

  // This is only used in tests to access a copy of the session state
  Map<String, Session> getSessionsMapForTest() {
    return sessionCache.getSessionsMapForTest();
  }

  private String getRequestBehavior(@RequestBehavior int behavior) {
    switch (behavior) {
      case RequestBehavior.REQUEST_WITH_WAIT:
        return "REQUEST_WITH_WAIT";
      case RequestBehavior.REQUEST_WITH_CONTENT:
        return "REQUEST_WITH_CONTENT";
      case RequestBehavior.REQUEST_WITH_TIMEOUT:
        return "REQUEST_WITH_TIMEOUT";
      case RequestBehavior.NO_REQUEST_WITH_WAIT:
        return "NO_REQUEST_WITH_WAIT";
      case RequestBehavior.NO_REQUEST_WITH_CONTENT:
        return "NO_REQUEST_WITH_CONTENT";
      case RequestBehavior.NO_REQUEST_WITH_TIMEOUT:
        return "NO_REQUEST_WITH_TIMEOUT";
      case RequestBehavior.UNKNOWN:
      default:
        return "UNKNOWN";
    }
  }

  @VisibleForTesting
  public boolean isDelayed() {
    return taskQueue.isDelayed();
  }
}
