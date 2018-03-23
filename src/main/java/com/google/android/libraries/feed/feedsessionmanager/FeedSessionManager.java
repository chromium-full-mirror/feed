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
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Committer;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.InitializableSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl.Change;
import com.google.android.libraries.feed.host.common.Callback;
import com.google.android.libraries.feed.host.config.ConfigManager;
import com.google.android.libraries.feed.host.config.ConfigManager.ConfigValues;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
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
  private final Map<String, StreamPayload> contentCache = new HashMap<>();

  private final SessionFactory sessionFactory;
  private final ExecutorService sessionManagerExecutor;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final boolean optimisticWrites;

  private ProtocolAdapter protocolAdapter;
  private RequestManager requestManager;

  // operation counts for the dumper
  private int commitCount = 0;
  private int newSessionCount = 0;
  private int existingSessionCount = 0;
  private int handleTokenCount = 0;

  public FeedSessionManager(
      ExecutorService sessionManagerExecutor,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      ConfigManager configManager) {
    this.sessionManagerExecutor = sessionManagerExecutor;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;

    optimisticWrites = configManager.getValue(ConfigValues.OPTIMISTIC_SESSION_WRITES, false);
    sessionFactory =
        new SessionFactory(store, sessionManagerExecutor, timingUtils, threadUtils, configManager);
    sessionManagerExecutor.execute(this::initializationTask);
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
    Session head = sessionFactory.getHeadSession();
    synchronized (lock) {
      sessions.put(head.getStreamSession().getStreamToken(), head);
    }
    List<StreamStructure> structureMetadata = store.getStreamStructures(head.getStreamSession());
    synchronized (lock) {
      head.updateSession(structureMetadata, null);
    }
    timeTracker.stop("task", "Initialization");
  }

  public void setProtocolAdapter(ProtocolAdapter protocolAdapter) {
    this.protocolAdapter = protocolAdapter;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  @Override
  public void getNewSession(ModelProvider modelProvider) {
    threadUtils.checkMainThread();
    InitializableSession session = sessionFactory.getSession(modelProvider);

    // Task which populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    sessionManagerExecutor.execute(
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
    sessionManagerExecutor.execute(
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
  public void handleToken(StreamToken streamToken) {
    // TODO: This should only create the request once.
    Logger.i(TAG, "HandleToken %s", streamToken.getContentId());
    threadUtils.checkMainThread();
    sessionManagerExecutor.execute(
        () -> {
          Logger.i(TAG, "Task: handleToken");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
          requestManager.loadMore(streamToken.getNextPageToken());
          timeTracker.stop("task", "handleToken");
          handleTokenCount++;
        });
  }

  @Override
  public void getStreamFeatures(List<String> contentIds, Callback<List<PayloadWithId>> callback) {
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
      callback.handleResult(results);
    } else {
      callback.handleResult(store.getPayloads(contentIds));
    }
  }

  @Override
  @Nullable
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
      for (Session session : sessions.values()) {
        if (session instanceof Dumpable) {
          dumper.dump((Dumpable) session);
        } else {
          dumper.forKey("session").value("Session Not Supporting Dumper");
        }
      }
    }
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
      sessionManagerExecutor.execute(this::commitTask);
      return null;
    }

    private void commitTask() {
      Logger.i(TAG, "Task: Mutate FeedSessionManager");
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      commitContent();
      commitSessionUpdates();
      commitCount++;
      timeTracker.stop("task", "Mutate FeedSessionManager", "mutations", streamStructures.size());
    }

    private void commitContent() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      ContentMutation contentMutation = store.editContent();
      for (StreamDataOperation dataOperation : change.dataOperations) {
        Operation operation = dataOperation.getStreamStructure().getOperation();
        if (operation == Operation.CLEAR_ALL) {
          streamStructures.add(dataOperation.getStreamStructure());
          resetHead();
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
        Logger.e(
            TAG, "Unsupported Mutation: %s", dataOperation.getStreamStructure().getOperation());
      }
      if (optimisticWrites) {
        sessionManagerExecutor.execute(contentMutation::commit);
      } else {
        contentMutation.commit();
      }
      timeTracker.stop("", "contentUpdate", "items", change.dataOperations.size());
    }

    private void commitSessionUpdates() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      synchronized (lock) {
        // For sessions we want to add the remove operation if the mutation source was a
        // continuation token.
        if (change.mutationSourceToken != null) {
          StreamStructure removeOperation = addTokenRemoveOperation(change.mutationSourceToken);
          if (removeOperation != null) {
            streamStructures.add(0, removeOperation);
          }
        }
        for (Session session : sessions.values()) {
          session.updateSession(streamStructures, change.mutationSourceToken);
        }
        timeTracker.stop("", "sessionUpdate", "sessions", sessions.size());
      }
    }

    private void resetHead() {
      synchronized (lock) {
        List<String> sessionsRemoved = new ArrayList<>();
        // TODO: Think through how this should work.  Only $HEAD is cleared
        // Do we still invalidate everything?
        store.clearHead();
        for (Session session : sessions.values()) {
          ModelProvider modelProvider = session.getModelProvider();
          if (modelProvider != null) {
            modelProvider.invalidate();
            store.removeSession(session.getStreamSession());
            sessionsRemoved.add(session.getStreamSession().getStreamToken());
          }
        }
        sessions.keySet().removeAll(sessionsRemoved);
      }
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

    @Nullable
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
