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
import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelMutator;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.sessionmanager.Session;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults.Status;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Implementation of {@link Session} and {@link ManagedSession}. */
public class SessionImpl implements FeedSession, Dumpable {
  private static final String TAG = "SessionImpl";

  private final ModelProvider modelProvider;
  private final Store store;
  private final InternalProtocolUtils protocolUtils;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;

  @VisibleForTesting final Set<String> contentInSession = new HashSet<>();

  private StreamSession streamSession;

  // operation counts for the dumper
  private int updateCount = 0;
  private int storeMutationFailures = 0;

  SessionImpl(
      ModelProviderFactory modelProviderFactory,
      Store store,
      TimingUtils timingUtils,
      InternalProtocolUtils protocolUtils,
      ThreadUtils threadUtils) {
    this.store = store;
    this.timingUtils = timingUtils;
    this.protocolUtils = protocolUtils;
    this.threadUtils = threadUtils;
    this.modelProvider = modelProviderFactory.create();
  }

  @Override
  public ModelProvider getModelProvider() {
    return modelProvider;
  }

  @Override
  public void populateModelProvider(List<StreamStructure> head) {
    Validators.checkNotNull(streamSession);
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

    ModelMutator modelMutator = modelProvider.edit();

    // Walk through head and add all the DataOperations to the session.
    for (StreamStructure streamStructure : head) {
      if (streamStructure.getOperation() == Operation.UPDATE_OR_APPEND) {
        String contentKey = protocolUtils.getContentIdKey(streamStructure.getContentId());
        if (contentInSession.contains(contentKey)) {
          Logger.w(
              TAG,
              "Updated operation stored in $HEAD, Item %s",
              protocolUtils.printContentId(streamStructure.getContentId()));
          continue;
        } else {
          contentInSession.add(contentKey);
          modelMutator.addChild(streamStructure);
        }
        continue;
      }
      Logger.e(TAG, "unsupported StreamDataOperation: %s", streamStructure.getOperation());
    }
    modelMutator.commit();
    timeTracker.stop("populateSession", streamSession.getSessionId(), "operations", head.size());
  }

  @Override
  public void updateSession(
      List<StreamStructure> streamStructures, @Nullable StreamToken mutationSourceToken) {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Validators.checkNotNull(streamSession);
    updateCount++;

    if (mutationSourceToken != null) {
      // Make sure that mutationSourceToken is a token in this session, if not, ignore it
      StreamContentId contentId = mutationSourceToken.getContentId();
      if (contentId == null) {
        // TODO: What do we do if there isnt' a ContentId?
        Logger.e(TAG, "Found a StreamToken without a ContentId");
      }
      if (!contentInSession.contains(protocolUtils.getContentIdKey(contentId))) {
        Logger.i(
            TAG,
            "Session without a Continuation Token %s, ignoring update",
            protocolUtils.getContentIdKey(contentId));
        return;
      }
    }

    ModelMutator modelMutator = modelProvider.edit();
    if (mutationSourceToken != null) {
      modelMutator.setMutationSourceToken(mutationSourceToken);
    }

    int updateCnt = 0;
    int addFeatureCnt = 0;
    SessionMutation sessionMutation = store.editSession(streamSession);
    for (StreamStructure streamStructure : streamStructures) {
      String contentKey = protocolUtils.getContentIdKey(streamStructure.getContentId());
      if (streamStructure.getOperation() == Operation.UPDATE_OR_APPEND) {
        if (contentInSession.contains(contentKey)) {
          // this is an update operation so we can ignore it
          Logger.i(TAG, "Found update to content in session");
          modelMutator.updateChild(streamStructure);
          updateCnt++;
        } else {
          sessionMutation.add(streamStructure);
          contentInSession.add(contentKey);
          modelMutator.addChild(streamStructure);
          addFeatureCnt++;
        }
      } else if (streamStructure.getOperation() == Operation.REMOVE) {
        Logger.i(TAG, "Removing Item from session %s", streamSession.getSessionId());
        sessionMutation.add(streamStructure);
        contentInSession.remove(contentKey);
        modelMutator.removeChild(streamStructure);
      } else if (streamStructure.getOperation() == Operation.CLEAR_ALL) {
        // Sessions are never cleared.
        // TODO: Defined the behavior for sessions when $HEAD is cleared
        Logger.i(TAG, "CLEAR_ALL not support on this session type");
      } else {
        Logger.e(TAG, "Unknown operation, ignoring: %s", streamStructure.getOperation());
      }
    }
    // Commit the Model Provider mutation after the store is updated.
    SessionMutationResults result = sessionMutation.commit();
    if (result.getStatus() == Status.SUCCESS) {
      modelMutator.commit();
      timeTracker.stop(
          "updateSession",
          streamSession.getSessionId(),
          "features",
          addFeatureCnt,
          "updates",
          updateCnt);
    } else {
      // TODO: What do we do, should we reverse the content added to the session?
      storeMutationFailures++;
      timeTracker.stop("updateSessionFailure", streamSession.getSessionId());
      Logger.e(TAG, "Session %s session mutation failed", streamSession.getSessionId());
    }
  }

  @Override
  public StreamSession getStreamSession() {
    return Validators.checkNotNull(streamSession);
  }

  @Override
  public void setStreamSession(StreamSession streamSession) {
    this.streamSession = streamSession;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("sessionName").value(streamSession.getSessionId());
    dumper.forKey("contentInSession").value(contentInSession.size()).compactPrevious();
    dumper.forKey("updateCount").value(updateCount).compactPrevious();
    dumper.forKey("storeMutationFailures").value(storeMutationFailures).compactPrevious();
    if (modelProvider instanceof Dumpable) {
      dumper.dump((Dumpable) modelProvider);
    }
  }
}
