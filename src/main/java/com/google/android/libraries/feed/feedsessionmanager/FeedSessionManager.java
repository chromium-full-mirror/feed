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

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.FeedSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.ManagedSession;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionMutationImpl.SessionMutationCommitter;
import com.google.android.libraries.feed.host.common.Callback;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
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
  private final Map<String, ManagedSession> sessions = new HashMap<>();

  @GuardedBy("lock")
  private final Map<String, StreamDataOperation> tokenMap = new HashMap<>();

  private final SessionFactory sessionFactory;
  private final ExecutorService sessionManagerExecutor;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;

  private Thread executorThread = null;
  private ProtocolAdapter protocolAdapter;
  private RequestManager requestManager;

  // operation counts for the dumper
  private int commitCount = 0;
  private int newSessionCount = 0;
  private int handleTokenCount = 0;

  public FeedSessionManager(
      ExecutorService sessionManagerExecutor,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils) {
    this.sessionManagerExecutor = sessionManagerExecutor;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;

    sessionFactory = new SessionFactory(store, timingUtils, threadUtils);
    sessionManagerExecutor.execute(
        () -> timingUtils.pinThread(Thread.currentThread(), "SessionManager"));
    sessionManagerExecutor.execute(
        () -> {
          executorThread = Thread.currentThread();
          timingUtils.pinThread(executorThread, "SessionManager");
        });
    sessionManagerExecutor.execute(this::initializationTask);
  }

  // Task which initializes the Session Manager.  This must be the first task run on the
  // Session Manager thread so it's complete before we create any sessions.
  private void initializationTask() {
    Logger.i(TAG, "Task: Initialization");
    threadUtils.checkSpecificThread(executorThread);
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    // Initialize the Shared States cached here.
    List<StreamSharedState> sharedStates = store.getSharedStates();
    synchronized (lock) {
      for (StreamSharedState sharedState : sharedStates) {
        sharedStateCache.put(sharedState.getContentId(), sharedState);
      }
    }

    // create the head session from the data in the Store
    ManagedSession head = sessionFactory.getHeadSession();
    synchronized (lock) {
      sessions.put(head.getStreamSession().streamToken, head);
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
    FeedSession session = sessionFactory.getSession(modelProvider);

    // Task which populates the newly created session.  This must be done
    // on the Session Manager thread so it atomic with the mutations.
    sessionManagerExecutor.execute(
        () -> {
          Logger.i(TAG, "Task: New Session");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
          threadUtils.checkSpecificThread(executorThread);

          StreamSession streamSession = store.createNewSession();
          session.setStreamSession(streamSession);
          session.populateModelProvider(store.getStreamStructures(streamSession));
          synchronized (lock) {
            sessions.put(session.getStreamSession().streamToken, session);
          }
          timeTracker.stop("task", "New Session");
          newSessionCount++;
        });
  }

  @Override
  public void handleToken(StreamToken streamToken) {
    // TODO: This should only create the request once.
    Logger.i(TAG, "HandleToken %s", streamToken.getContentId());
    threadUtils.checkMainThread();
    handleTokenCount++;
    @SuppressLint("StaticFieldLeak")
    AsyncTask<Void, Void, Void> task =
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            requestManager.loadMore(streamToken.getNextPageToken());
            return null;
          }
        };
    task.execute();
  }

  @Override
  public void getStreamFeatures(List<String> contentIds, Callback<List<PayloadWithId>> callback) {
    callback.handleResult(store.getPayloads(contentIds));
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
      dumper.forKey("handleTokenCount").value(handleTokenCount).compactPrevious();
      dumper.forKey("sharedStateCount").value(sharedStateCache.size());
      dumper.forKey("sessionCount").value(sessions.size()).compactPrevious();
      dumper.forKey("tokenCount").value(tokenMap.size()).compactPrevious();
      for (ManagedSession session : sessions.values()) {
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
  private class CommitterTask implements SessionMutationCommitter {
    @Nullable private StreamToken mutationSourceToken;
    private List<StreamDataOperation> dataOperations;
    private final List<StreamStructure> streamStructures = new ArrayList<>();

    @Override
    public void commit(
        List<StreamDataOperation> dataOperations, @Nullable StreamToken mutationSourceToken) {
      this.dataOperations = dataOperations;
      this.mutationSourceToken = mutationSourceToken;
      sessionManagerExecutor.execute(this::commitTask);
    }

    private void commitTask() {
      Logger.i(TAG, "Task: Commit Mutation");
      threadUtils.checkSpecificThread(executorThread);
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      commitContent();
      commitSessionUpdates();
      commitCount++;
      timeTracker.stop("task", "Commit Mutation");
    }

    private void commitContent() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      ContentMutation contentMutation = store.editContent();
      for (StreamDataOperation dataOperation : dataOperations) {
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
      contentMutation.commit();
      timeTracker.stop("", "contentUpdate", "items", dataOperations.size());
    }

    private void commitSessionUpdates() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      synchronized (lock) {
        // For sessions we want to add the remove operation if the mutation source was a
        // continuation token.
        if (mutationSourceToken != null) {
          StreamStructure removeOperation = addTokenRemoveOperation(mutationSourceToken);
          if (removeOperation != null) {
            streamStructures.add(0, removeOperation);
          }
        }
        for (ManagedSession session : sessions.values()) {
          session.updateSession(streamStructures, mutationSourceToken);
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
        for (ManagedSession managedSession : sessions.values()) {
          if (managedSession instanceof FeedSession) {
            FeedSession session = (FeedSession) managedSession;
            ModelProvider modelProvider = session.getModelProvider();
            modelProvider.invalidate();
            store.removeSession(session.getStreamSession());
            sessionsRemoved.add(session.getStreamSession().streamToken);
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
  Map<String, ManagedSession> getSessionsMapForTest() {
    synchronized (lock) {
      return new HashMap<>(sessions);
    }
  }
}
