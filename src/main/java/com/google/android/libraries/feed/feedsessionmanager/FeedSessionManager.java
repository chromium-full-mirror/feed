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

import android.text.TextUtils;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Committer;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.Supplier;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.ContentCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.InitializableSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl.Change;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
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
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.GuardedBy;

/** Prototype implementation of the SessionManager. All state is kept in memory. */
public class FeedSessionManager implements SessionManager, Dumpable {

  private static final String TAG = "FeedSessionManager";

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
  private final Supplier<ProtocolAdapter> protocolAdapterSupplier;
  private final Supplier<RequestManager> requestManagerSupplier;
  private final boolean optimisticWrites;

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
      Supplier<ProtocolAdapter> protocolAdapterSupplier,
      Supplier<RequestManager> requestManagerSupplier,
      Configuration configuration) {
    this.executor = executor;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.protocolAdapterSupplier = protocolAdapterSupplier;
    this.requestManagerSupplier = requestManagerSupplier;

    optimisticWrites = configuration.getValueOrDefault(ConfigKey.OPTIMISTIC_SESSION_WRITES, false);
    sessionFactory = new SessionFactory(store, executor, timingUtils, threadUtils, configuration);
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
    List<StreamStructure> streamStructures = store.getStreamStructures(head.getStreamSession());
    head.initializeSession(streamStructures);
    timeTracker.stop("task", "Initialization");
  }

  @Override
  public void getNewSession(ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    InitializableSession session = sessionFactory.getSession(modelProvider);

    // Task which populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    executor.execute(
        () -> {
          Logger.i(TAG, "Task: Create/Populate New Session");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

          StreamSession streamSession = store.createNewSession();
          session.populateModelProvider(streamSession, store.getStreamStructures(streamSession));
          synchronized (lock) {
            sessions.put(session.getStreamSession().getStreamToken(), session);
          }
          timeTracker.stop("task", "Create/Populate New Session");
          newSessionCount++;
        });
  }

  @Override
  public void getExistingSession(String sessionToken, ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    InitializableSession session = sessionFactory.getSession(modelProvider);
    // TODO: We need to deal with all sessions, some may be stored on disk an not yet
    // loaded.
    Session existingSession;
    synchronized (lock) {
      existingSession = sessions.get(sessionToken);
    }
    if (existingSession != null) {
      ModelProvider existingModelProvider = existingSession.getModelProvider();
      if (existingModelProvider != null) {
        existingModelProvider.invalidate();
      }
    } else {
      Logger.w(
          TAG, "Didn't find an existing Session called %s, creating new Session", sessionToken);
      getNewSession(modelProvider);
      return;
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
  }

  @Override
  public void invalidateHead() {
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
    requestManagerSupplier
        .get()
        .loadMore(
            new MutationContext.Builder()
                .setContinuationToken(streamToken)
                .setRequestingSession(streamSession)
                .build());
  }

  @Override
  public void forceRefresh(/*@Nullable*/ StreamSession streamSession) {
    if (streamSession == null) {
      // TODO: see [INTERNAL LINK]  We create a new session and then call forceRefresh on
      // it before we have even populated the session in Audrey2.
      Logger.w(TAG, "Found a null streamSession in forceRefresh");
    }
    requestManagerSupplier.get().triggerRefresh(RequestReason.MANUAL_REFRESH);
  }

  @Override
  public void getStreamFeatures(List<String> contentIds, Consumer<List<PayloadWithId>> consumer) {
    if (optimisticWrites) {
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
    } else {
      consumer.accept(store.getPayloads(contentIds));
    }
  }

  @Override
  /*@Nullable*/
  public StreamSharedState getSharedState(ContentId contentId) {
    threadUtils.checkMainThread();
    String sharedStateId = protocolAdapterSupplier.get().getStreamContentId(contentId);
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
  public SessionMutation edit() {
    return new SessionMutationImpl(new CommitterTask());
  }

  @Override
  public void dump(Dumper dumper) {
    synchronized (lock) {
      dumper.title(TAG);
      dumper.forKey("commitCount").value(commitCount);
      dumper.forKey("newSessionCount").value(newSessionCount).compactPrevious();
      dumper.forKey("existingSessionCount").value(existingSessionCount).compactPrevious();
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

  /** This runs as a task on the executor thread and also as part of a SessionMutation commit. */
  private void resetHead(/*@Nullable*/ StreamSession streamSession) {
    Logger.i(TAG, "Task: resetHead");
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Collection<Session> currentSessions;
    synchronized (lock) {
      currentSessions = sessions.values();
    }

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
      Logger.i(TAG, "Clearing %s sessions", sessionsRemoved.size());
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
   * This class will commit a session mutation. This will queue the actual work onto the Session
   * Manager thread. We will update the content first followed by updates to each session.
   */
  private class CommitterTask implements Committer<Void, Change> {
    private Change change;
    private final List<StreamStructure> streamStructures = new ArrayList<>();

    @Override
    public Void commit(Change change) {
      this.change = change;
      executor.execute(this::commitTask);
      return null;
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
      for (StreamDataOperation dataOperation : change.dataOperations) {
        Operation operation = dataOperation.getStreamStructure().getOperation();
        if (operation == Operation.CLEAR_ALL) {
          streamStructures.add(dataOperation.getStreamStructure());
          resetHead(
              (change.mutationContext != null)
                  ? change.mutationContext.getRequestingSession()
                  : null);
          continue;
        }
        if (operation == Operation.UPDATE_OR_APPEND) {
          if (!validDataOperation(dataOperation)) {
            continue;
          }
          StreamPayload payload = dataOperation.getStreamPayload();
          String contentId = dataOperation.getStreamStructure().getContentId();
          if (optimisticWrites) {
            // TODO: When optimisticWrites is the default move synchronized outside the
            // for loop
            synchronized (lock) {
              contentCache.put(contentId, payload);
            }
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
      if (optimisticWrites) {
        executor.execute(contentMutation::commit);
        // TODO: currently there is a small window in which an action could occur and the
        // request be sent to the server before this is written, fix
        executor.execute(semanticPropertiesMutation::commit);
      } else {
        contentMutation.commit();
        semanticPropertiesMutation.commit();
      }
      timeTracker.stop("", "contentUpdate", "items", change.dataOperations.size());
    }

    private void commitSessionUpdates() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      ArrayList<Session> updates;
      /*@Nullable*/
      StreamToken mutationSourceToken =
          (change.mutationContext != null) ? change.mutationContext.getContinuationToken() : null;
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
        session.updateSession(streamStructures, mutationSourceToken);
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
}
