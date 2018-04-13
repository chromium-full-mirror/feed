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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelMutation;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/** Implementation of a {@link Session}. */
public class SessionImpl implements InitializableSession, Dumpable {
  private static final String TAG = "SessionImpl";

  private final ModelProvider modelProvider;
  private final Store store;
  private final ExecutorService sessionManagerExecutor;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final boolean optimisticWrites;

  @VisibleForTesting final Set<String> contentInSession = new HashSet<>();

  private StreamSession streamSession;

  // operation counts for the dumper
  private int updateCount = 0;
  private int storeMutationFailures = 0;

  SessionImpl(
      Store store,
      ModelProvider modelProvider,
      ExecutorService sessionManagerExecutor,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      Configuration configuration) {
    this.store = store;
    this.modelProvider = modelProvider;
    this.sessionManagerExecutor = sessionManagerExecutor;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    optimisticWrites = configuration.getValueOrDefault(ConfigKey.OPTIMISTIC_SESSION_WRITES, false);
  }

  @Override
  public ModelProvider getModelProvider() {
    return modelProvider;
  }

  @Override
  public void populateModelProvider(StreamSession streamSession, List<StreamStructure> head) {
    this.streamSession = streamSession;
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

    ModelMutation modelMutation = modelProvider.edit();

    // Walk through head and add all the DataOperations to the session.
    for (StreamStructure streamStructure : head) {
      if (streamStructure.getOperation() == Operation.UPDATE_OR_APPEND) {
        if (contentInSession.contains(streamStructure.getContentId())) {
          Logger.w(
              TAG, "Updated operation stored in $HEAD, Item %s", streamStructure.getContentId());
          continue;
        } else {
          contentInSession.add(streamStructure.getContentId());
          modelMutation.addChild(streamStructure);
        }
        continue;
      }
      Logger.e(TAG, "unsupported StreamDataOperation: %s", streamStructure.getOperation());
    }
    modelMutation.setStreamSession(streamSession);
    modelMutation.commit();
    timeTracker.stop("populateSession", streamSession.getStreamToken(), "operations", head.size());
  }

  @Override
  public void updateSession(
      List<StreamStructure> streamStructures, /*@Nullable*/ StreamToken mutationSourceToken) {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Validators.checkNotNull(streamSession);
    updateCount++;

    if (mutationSourceToken != null) {
      // Make sure that mutationSourceToken is a token in this session, if not, ignore it
      String contentId = mutationSourceToken.getContentId();
      if (contentId == null) {
        // TODO: What do we do if there isnt' a ContentId?
        Logger.e(TAG, "Found a StreamToken without a ContentId");
      }
      if (!contentInSession.contains(contentId)) {
        Logger.i(TAG, "Session without a Continuation Token %s, ignoring update", contentId);
        return;
      }
    }

    ModelMutation modelMutation = modelProvider.edit();
    if (mutationSourceToken != null) {
      modelMutation.setMutationSourceToken(mutationSourceToken);
    }

    int updateCnt = 0;
    int addFeatureCnt = 0;
    SessionMutation sessionMutation = store.editSession(streamSession);
    for (StreamStructure streamStructure : streamStructures) {
      String contentKey = streamStructure.getContentId();
      if (streamStructure.getOperation() == Operation.UPDATE_OR_APPEND) {
        if (contentInSession.contains(contentKey)) {
          // this is an update operation so we can ignore it
          Logger.i(TAG, "Found update to content in session");
          modelMutation.updateChild(streamStructure);
          updateCnt++;
        } else {
          sessionMutation.add(streamStructure);
          contentInSession.add(contentKey);
          modelMutation.addChild(streamStructure);
          addFeatureCnt++;
        }
      } else if (streamStructure.getOperation() == Operation.REMOVE) {
        Logger.i(TAG, "Removing Item from session %s", streamSession.getStreamToken());
        sessionMutation.add(streamStructure);
        contentInSession.remove(contentKey);
        modelMutation.removeChild(streamStructure);
      } else if (streamStructure.getOperation() == Operation.CLEAR_ALL) {
        // Sessions are never cleared.
        // TODO: Defined the behavior for sessions when $HEAD is cleared
        Logger.i(TAG, "CLEAR_ALL not support on this session type");
      } else {
        Logger.e(TAG, "Unknown operation, ignoring: %s", streamStructure.getOperation());
      }
    }
    // Commit the Model Provider mutation after the store is updated.
    boolean success;
    if (optimisticWrites) {
      sessionManagerExecutor.execute(sessionMutation::commit);
      success = true;
    } else {
      success = sessionMutation.commit();
    }
    // TODO: When we only support optimistic writes remove the if statement
    if (success) {
      modelMutation.commit();
      timeTracker.stop(
          "updateSession",
          streamSession.getStreamToken(),
          "features",
          addFeatureCnt,
          "updates",
          updateCnt);
    } else {
      // TODO: What do we do, should we reverse the content added to the session?
      storeMutationFailures++;
      timeTracker.stop("updateSessionFailure", streamSession.getStreamToken());
      Logger.e(TAG, "Session %s session mutation failed", streamSession.getStreamToken());
    }
  }

  @Override
  public StreamSession getStreamSession() {
    return Validators.checkNotNull(streamSession);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("sessionName").value(streamSession.getStreamToken());
    dumper.forKey("contentInSession").value(contentInSession.size()).compactPrevious();
    dumper.forKey("updateCount").value(updateCount).compactPrevious();
    dumper.forKey("storeMutationFailures").value(storeMutationFailures).compactPrevious();
    if (modelProvider instanceof Dumpable) {
      dumper.dump((Dumpable) modelProvider);
    }
  }
}
