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

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.ContentCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.InitializableSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSessions;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.GuardedBy;

/** Prototype implementation of the SessionManager. All state is kept in memory. */
public class FeedSessionManager implements SessionManager, Dumpable {

  private static final String TAG = "FeedSessionManager";

  @VisibleForTesting static final String STREAM_SESSION_CONTENT_ID = "FSM::Sessions::0";
  @VisibleForTesting static final long DEFAULT_LIFETIME_MS = 3_600_000; // 1 Hour

  // Used to synchronize the stored data
  private final Object lock = new Object();

  // For the Shared State we will always cache them in the Session Manager
  @GuardedBy("lock")
  private final Map<String, StreamSharedState> sharedStateCache = new HashMap<>();

  @GuardedBy("lock")
  private final Map<String, Session> sessions = new HashMap<>();

  @GuardedBy("lock")
  private final Map<String, StreamDataOperation> tokenMap = new HashMap<>();

  @GuardedBy("lock")
  private final ContentCache contentCache;

  private final SessionFactory sessionFactory;
  private final ExecutorService executor;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final ProtocolAdapter protocolAdapter;
  private final RequestManager requestManager;
  private final Clock clock;
  private final long lifetimeMs;

  // operation counts for the dumper
  private int commitCount = 0;
  private int newSessionCount = 0;
  private int existingSessionCount = 0;
  private int unboundSessionCount = 0;
  private int handleTokenCount = 0;
  private int expiredSessionsCleared = 0;

  // When we invalidate head, we will delay creation of any new sessions until head is populated.
  // In order to do that, the getNewSession() method will queue the task to create and populate
  // the new session on the delayedRunnables List.  Once we process the first mutation on head
  // we will run these tasks.
  @GuardedBy("lock")
  private boolean invalidatedHead;

  @GuardedBy("lock")
  private final List<Runnable> delayedRunnables = new ArrayList<>();

  public FeedSessionManager(
      ExecutorService executor,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      ProtocolAdapter protocolAdapter,
      RequestManager requestManager,
      Configuration configuration,
      Clock clock) {
    this.executor = executor;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.protocolAdapter = protocolAdapter;
    this.requestManager = requestManager;
    this.clock = clock;

    lifetimeMs =
        configuration.getValueOrDefault(ConfigKey.SESSION_LIFETIME_MS, DEFAULT_LIFETIME_MS);
    sessionFactory = new SessionFactory(store, executor, timingUtils, threadUtils);
    contentCache = new ContentCache();

    // Suppress /*@UnderInitialization*/ warning
    @SuppressWarnings("nullness")
    Runnable initializationTask = this::initializationTask;
    executor.execute(initializationTask);
  }

  // Task which initializes the Session Manager.  This must be the first task run on the
  // Session Manager thread so it's complete before we create any sessions.
  private void initializationTask() {
    Logger.i(TAG, "Task: Initialization");
    timingUtils.pinThread(Thread.currentThread(), "SessionManager");
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    // Initialize the Shared States cached here.
    List<StreamSharedState> sharedStates = store.getSharedStates();
    synchronized (lock) {
      for (StreamSharedState sharedState : sharedStates) {
        sharedStateCache.put(sharedState.getContentId(), sharedState);
      }
    }

    // create the head session from the data in the Store
    HeadSessionImpl head = sessionFactory.getHeadSession();
    synchronized (lock) {
      sessions.put(head.getStreamSession().getStreamToken(), head);
    }
    head.updateAccessTime(clock.currentTimeMillis());
    List<StreamStructure> streamStructures = store.getStreamStructures(head.getStreamSession());
    head.initializeSession(streamStructures);
    initializePersistedSessions();
    timeTracker.stop("task", "Initialization");
  }

  @VisibleForTesting
  void initializePersistedSessions() {
    StreamSessions persistedSessions = getPersistedSessions();
    if (persistedSessions == null) {
      return;
    }

    boolean cleanupSessions = false;
    List<StreamSession> sessionList = persistedSessions.getStreamSessionsList();
    for (StreamSession session : sessionList) {
      if (!isSessionAlive(session)) {
        Logger.i(TAG, "Found expired session %s", session.getStreamToken());
        cleanupSessions = true;
        continue;
      }
      synchronized (lock) {
        if (sessions.containsKey(session.getStreamToken())) {
          // Don't replace sessions already found in sessions
          continue;
        }
        // Unbound sessions are sessions that are able to be created through restore
        InitializableSession unboundSession = sessionFactory.getSession();
        unboundSession.setStreamSession(session);
        sessions.put(session.getStreamToken(), unboundSession);

        // Task which populates the newly created unbound session.
        executor.execute(
            () -> {
              Logger.i(TAG, "Task: createUnboundSession");
              ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
              unboundSession.populateModelProvider(session, store.getStreamStructures(session));
              timeTracker.stop("task", "createUnboundSession");
              unboundSessionCount++;
            });
      }
    }

    if (cleanupSessions) {
      // Queue up a task to clear the session journals.
      executor.execute(this::cleanupSessionJournals);
    }
    Set<String> reservedContentIds = new HashSet<>();
    reservedContentIds.add(STREAM_SESSION_CONTENT_ID);
    Runnable gcRunnable = store.triggerContentGc(reservedContentIds, accessibleContentSupplier);
    synchronized (lock) {
      if (invalidatedHead) {
        delayedRunnables.add(gcRunnable);
      } else {
        executor.execute(gcRunnable);
      }
    }
  }

  private Supplier<Set<String>> accessibleContentSupplier =
      () -> {
        Set<String> accessibleContent = new HashSet<>();
        synchronized (lock) {
          for (Session session : sessions.values()) {
            accessibleContent.addAll(session.getContentInSession());
          }
        }
        return accessibleContent;
      };

  /**
   * Remove all session journals which are not currently found in {@code sessions} which contains
   * all of the known sessions. This is a garbage collection task.
   */
  @VisibleForTesting
  void cleanupSessionJournals() {
    Logger.i(TAG, "Task: cleanupSessionJournals");
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    int sessionCleared = expiredSessionsCleared;

    List<StreamSession> storedSessions = store.getAllSessions();
    synchronized (lock) {
      for (StreamSession session : storedSessions) {
        if (!sessions.containsKey(session.getStreamToken())) {
          store.removeSession(session);
          expiredSessionsCleared++;
        }
      }
    }
    timeTracker.stop(
        "task",
        "cleanupSessionJournals",
        "sessionsCleared",
        expiredSessionsCleared - sessionCleared);
  }

  /** Returns the persisted {@link StreamSessions} or {@code null} if it's not found. */
  /*@Nullable*/
  @VisibleForTesting
  StreamSessions getPersistedSessions() {
    List<String> sessionIds = new ArrayList<>();
    sessionIds.add(STREAM_SESSION_CONTENT_ID);
    List<PayloadWithId> sessionPayload = store.getPayloads(sessionIds);

    if (sessionPayload.isEmpty()) {
      Logger.w(TAG, "Persisted Sessions were not found");
      return null;
    }
    StreamPayload payload = sessionPayload.get(0).payload;
    if (!payload.hasStreamSessions()) {
      Logger.e(TAG, "Persisted Sessions StreamSessions was not set");
      return null;
    }
    return payload.getStreamSessions();
  }

  @Override
  public void getNewSession(ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    InitializableSession session = sessionFactory.getSession();
    session.bindModelProvider(modelProvider);

    // Task which creates and populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    Runnable populateSession =
        () -> {
          Logger.i(TAG, "Task: Create/Populate New Session");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

          StreamSession streamSession = store.createNewSession();
          StreamSession recordedSession =
              streamSession.toBuilder().setLastAccessed(clock.currentTimeMillis()).build();

          session.populateModelProvider(recordedSession, store.getStreamStructures(streamSession));
          synchronized (lock) {
            sessions.put(session.getStreamSession().getStreamToken(), session);
          }
          updateStoredSessions();

          timeTracker.stop("task", "Create/Populate New Session");
          newSessionCount++;
        };
    synchronized (lock) {
      if (invalidatedHead) {
        // don't create and populate the session until $HEAD has been updated, push to front of
        // the queue
        delayedRunnables.add(0, populateSession);
      } else {
        executor.execute(populateSession);
      }
    }
  }

  @VisibleForTesting
  void updateStoredSessions() {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    StreamSessions.Builder sessionsBuilder = StreamSessions.newBuilder();
    synchronized (lock) {
      for (Session s : sessions.values()) {
        sessionsBuilder.addStreamSessions(s.getStreamSession());
      }
    }
    StreamSessions currentSessions = sessionsBuilder.build();
    StreamPayload payload = StreamPayload.newBuilder().setStreamSessions(currentSessions).build();
    ContentMutation contentMutation = store.editContent();
    contentMutation.add(STREAM_SESSION_CONTENT_ID, payload);
    contentMutation.commit();
    timeTracker.stop("task", "Update Stored Session Information");
  }

  @Override
  public boolean getExistingSession(String sessionToken, ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    Session existingSession;
    synchronized (lock) {
      existingSession = sessions.get(sessionToken);
    }
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
    executor.execute(
        () -> {
          Logger.i(TAG, "Task: CreateExistingSession");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

          session.populateModelProvider(streamSession, store.getStreamStructures(streamSession));
          synchronized (lock) {
            sessions.put(session.getStreamSession().getStreamToken(), session);
          }
          timeTracker.stop("task", "CreateExistingSession");
          existingSessionCount++;
        });
    return true;
  }

  @Override
  public void invalidateHead() {
    synchronized (lock) {
      invalidatedHead = true;
    }
    executor.execute(() -> resetHead(null));
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
            new MutationContext.Builder()
                .setContinuationToken(streamToken)
                .setRequestingSession(streamSession)
                .build()));
  }

  @Override
  public void triggerRefresh(/*@Nullable*/ StreamSession streamSession) {
    if (streamSession == null) {
      // TODO: see [INTERNAL LINK]  We create a new session and then call triggerRefresh on
      // it before we have even populated the session in Audrey2.
      Logger.w(TAG, "Found a null streamSession in triggerRefresh");
    }
    requestManager.triggerRefresh(
        RequestReason.MANUAL_REFRESH, new CommitterTask(MutationContext.EMPTY_CONTEXT));
  }

  @Override
  public void getStreamFeatures(List<String> contentIds, Consumer<List<PayloadWithId>> consumer) {
    List<PayloadWithId> results = new ArrayList<>();
    List<String> cacheMisses = new ArrayList<>();
    int contentSize;
    synchronized (lock) {
      contentSize = contentCache.size();
      for (String contentId : contentIds) {
        StreamPayload payload = contentCache.get(contentId);
        if (payload != null) {
          results.add(new PayloadWithId(contentId, payload));
        } else {
          cacheMisses.add(contentId);
        }
      }
    }

    if (!cacheMisses.isEmpty()) {
      List<PayloadWithId> content = store.getPayloads(cacheMisses);
      results.addAll(content);
    }
    Logger.i(
        TAG,
        "Caching getStreamFeatures - items %s, cache misses %s, cache size %s",
        contentIds.size(),
        cacheMisses.size(),
        contentSize);
    consumer.accept(results);
  }

  @Override
  /*@Nullable*/
  public StreamSharedState getSharedState(ContentId contentId) {
    threadUtils.checkMainThread();
    String sharedStateId = protocolAdapter.getStreamContentId(contentId);
    StreamSharedState state;
    synchronized (lock) {
      state = sharedStateCache.get(sharedStateId);
    }
    if (state == null) {
      Logger.e(TAG, "Shared State [%s] was not found", sharedStateId);
    }
    return state;
  }

  @Override
  public Consumer<Result<List<StreamDataOperation>>> getUpdateConsumer(
      MutationContext mutationContext) {
    return new CommitterTask(mutationContext);
  }

  @Override
  public void dump(Dumper dumper) {
    synchronized (lock) {
      dumper.title(TAG);
      dumper.forKey("commitCount").value(commitCount);
      dumper.forKey("newSessionCount").value(newSessionCount).compactPrevious();
      dumper.forKey("existingSessionCount").value(existingSessionCount).compactPrevious();
      dumper.forKey("unboundSessionCount").value(unboundSessionCount).compactPrevious();
      dumper.forKey("expiredSessionsCleared").value(expiredSessionsCleared).compactPrevious();
      dumper.forKey("handleTokenCount").value(handleTokenCount).compactPrevious();
      dumper.forKey("sharedStateCount").value(sharedStateCache.size());
      dumper.forKey("sessionCount").value(sessions.size()).compactPrevious();
      dumper.forKey("tokenCount").value(tokenMap.size()).compactPrevious();
      dumper.dump(contentCache);
      for (Session session : sessions.values()) {
        if (session instanceof Dumpable) {
          dumper.dump((Dumpable) session);
        } else {
          dumper.forKey("session").value("Session Not Supporting Dumper");
        }
      }
    }
  }

  @VisibleForTesting
  boolean isSessionAlive(StreamSession streamSession) {
    // Today HEAD will does not time out
    return (Store.HEAD.getStreamToken().equals(streamSession.getStreamToken()))
        || ((streamSession.getLastAccessed() + lifetimeMs) > clock.currentTimeMillis());
  }

  /** This runs as a task on the executor thread and also as part of a SessionMutation commit. */
  private void resetHead(/*@Nullable*/ StreamSession streamSession) {
    Logger.i(TAG, "Task: resetHead");
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Collection<Session> currentSessions;
    synchronized (lock) {
      currentSessions = sessions.values();
    }

    // If we have old sessions and we received a clear head, let's invalidate the session that
    // initiated the clear.
    List<String> sessionsRemoved = new ArrayList<>();
    store.clearHead();
    for (Session session : currentSessions) {
      ModelProvider modelProvider = session.getModelProvider();
      if (modelProvider != null && invalidateSession(streamSession, modelProvider)) {
        modelProvider.invalidate();
        store.removeSession(session.getStreamSession());
        sessionsRemoved.add(session.getStreamSession().getStreamToken());
      }
    }
    synchronized (lock) {
      sessions.keySet().removeAll(sessionsRemoved);
    }
    timeTracker.stop("task", "resetHead");
  }

  /**
   * This will determine if the ModelProvider (session) should be invalidated. There are three
   * situations which will cause a session to be invalidated:
   *
   * <ol>
   *   <li>Clearing head was initiated externally, not from an existing session
   *   <li>Clearing head was initiated by the ModelProvider
   *   <li>The ModelProvider doesn't yet have a session, so we'll create the session from the new
   *       $HEAD
   * </ol>
   */
  private boolean invalidateSession(
      /*@Nullable*/ StreamSession streamSession, ModelProvider modelProvider) {
    // Clear was done outside of any session.
    if (streamSession == null) {
      return true;
    }
    // Invalidate if the ModelProvider doesn't yet have a session or if it matches the session
    // which initiated the request clearing $HEAD.
    String sessionToken = modelProvider.getSessionToken();
    return (sessionToken == null || sessionToken.equals(streamSession.getStreamToken()));
  }

  /**
   * This class will process a List of {@link StreamDataOperation}. The {@link MutationContext} is
   * stored here and contains the context which made the request resulting in the List of data
   * operations.
   */
  private class CommitterTask implements Consumer<Result<List<StreamDataOperation>>> {
    private final List<StreamStructure> streamStructures = new ArrayList<>();
    private final MutationContext mutationContext;

    private boolean clearedHead = false;
    private List<StreamDataOperation> dataOperations;

    CommitterTask(MutationContext mutationContext) {
      this.mutationContext = mutationContext;
    }

    @Override
    public void accept(Result<List<StreamDataOperation>> updateResults) {
      if (!updateResults.isSuccessful()) {
        if (mutationContext.getContinuationToken() != null) {
          Logger.e(
              TAG, "Error found with a token request %s", mutationContext.getContinuationToken());
          // TODO: We need to route this issue through the UI for display
        } else {
          Logger.e(TAG, "Unexpected Update error found, the update is being ignored");
        }
        return;
      }
      dataOperations = updateResults.getValue();
      executor.execute(this::commitTask);
    }

    private void commitTask() {
      Logger.i(TAG, "Task: Mutate FeedSessionManager");
      synchronized (lock) {
        contentCache.startMutation();
      }
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      commitContent();
      commitSessionUpdates();
      commitCount++;
      synchronized (lock) {
        contentCache.finishMutation();
      }
      timeTracker.stop("task", "Mutate FeedSessionManager", "mutations", streamStructures.size());
    }

    private void commitContent() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      ContentMutation contentMutation = store.editContent();
      SemanticPropertiesMutation semanticPropertiesMutation = store.editSemanticProperties();
      for (StreamDataOperation dataOperation : dataOperations) {
        Operation operation = dataOperation.getStreamStructure().getOperation();
        if (operation == Operation.CLEAR_ALL) {
          clearedHead = true;
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
          // TODO: Should we move this outside of the loop?
          synchronized (lock) {
            contentCache.put(contentId, payload);
          }
          if (payload.hasStreamSharedState()) {
            contentMutation.add(contentId, payload);
            synchronized (lock) {
              // cache the shared state here
              sharedStateCache.put(
                  dataOperation.getStreamStructure().getContentId(),
                  payload.getStreamSharedState());
            }
          } else if (payload.hasStreamFeature() || payload.hasStreamToken()) {
            // don't add StreamSharedState to the metadata list stored for sessions
            streamStructures.add(dataOperation.getStreamStructure());
            contentMutation.add(contentId, payload);
            if (payload.hasStreamToken()) {
              // Track the tokens so we can create the remove operations in a future response
              synchronized (lock) {
                tokenMap.put(payload.getStreamToken().getContentId(), dataOperation);
              }
            }
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
      executor.execute(contentMutation::commit);
      // TODO: currently there is a small window in which an action could occur and the
      // request be sent to the server before this is written, fix
      executor.execute(semanticPropertiesMutation::commit);
      timeTracker.stop("", "contentUpdate", "items", dataOperations.size());
    }

    private void commitSessionUpdates() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      ArrayList<Session> updates;
      /*@Nullable*/
      StreamToken mutationSourceToken =
          (mutationContext != null) ? mutationContext.getContinuationToken() : null;
      synchronized (lock) {
        // For sessions we want to add the remove operation if the mutation source was a
        // continuation token.
        if (mutationSourceToken != null) {
          StreamStructure removeOperation = addTokenRemoveOperation(mutationSourceToken);
          if (removeOperation != null) {
            streamStructures.add(0, removeOperation);
          }
        }
        updates = new ArrayList<>(sessions.values());
      }
      for (Session session : updates) {
        // TODO: Add something to the interface to avoid the instanceof?
        if (clearedHead && !(session instanceof HeadSessionImpl)) {
          // if we cleared the head, all existing session will not be updated by this mutation
          continue;
        }
        Logger.i(TAG, "Update Session %s", session.getStreamSession().getStreamToken());
        session.updateSession(streamStructures, mutationSourceToken);
      }
      synchronized (lock) {
        if (invalidatedHead) {
          Logger.i(
              TAG, "Processing runnables created since invalidate %s", delayedRunnables.size());
          invalidatedHead = false;
          for (Runnable runnable : delayedRunnables) {
            executor.execute(runnable);
          }
          delayedRunnables.clear();
        }
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
      // We need to add a mutation to remove the stream token from the structure.
      String tokenId = token.getContentId();
      synchronized (lock) {
        StreamDataOperation dataOperation = tokenMap.get(tokenId);
        if (dataOperation != null) {
          tokenMap.remove(tokenId);
          StreamStructure.Builder structureBuilder = dataOperation.getStreamStructure().toBuilder();
          structureBuilder.setOperation(Operation.REMOVE);
          return structureBuilder.build();
        } else {
          Logger.e(TAG, "Unable to find StreamDataOperation for Token %s", tokenId);
          return null;
        }
      }
    }
  }

  // This is only used in tests to verify the contents of the shared state cache.
  Map<String, StreamSharedState> getSharedStateCacheForTest() {
    synchronized (lock) {
      return new HashMap<>(sharedStateCache);
    }
  }

  // This is only used in tests to access a copy of the session state
  Map<String, Session> getSessionsMapForTest() {
    synchronized (lock) {
      return new HashMap<>(sessions);
    }
  }

  boolean getInvalidateHeadStateForTest() {
    synchronized (lock) {
      return invalidatedHead;
    }
  }

  List<Runnable> getDelayedRunnables() {
    synchronized (lock) {
      return new ArrayList<>(delayedRunnables);
    }
  }

  void setSessionsForTest(Session... testSessions) {
    synchronized (lock) {
      for (Session session : testSessions) {
        sessions.put(session.getStreamSession().getStreamToken(), session);
      }
    }
  }
}
