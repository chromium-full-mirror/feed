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
import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults.Status;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Implementation of {@link ManagedSession} for $HEAD. This class doesn't support a ModelProvider.
 */
public class HeadSessionImpl implements ManagedSession, Dumpable {
  private static final String TAG = "HeadSessionImpl";

  private final StreamSession streamSession;
  private final Store store;
  private final InternalProtocolUtils protocolUtils;
  private final TimingUtils timingUtils;

  @VisibleForTesting final Set<String> contentInSession = new HashSet<>();

  // operation counts for the dumper
  private int updateCount = 0;
  private int storeMutationFailures = 0;

  HeadSessionImpl(Store store, TimingUtils timingUtils, InternalProtocolUtils protocolUtils) {
    this.streamSession = store.getHeadSession();
    this.store = store;
    this.timingUtils = timingUtils;
    this.protocolUtils = protocolUtils;
  }

  @Override
  public void updateSession(
      List<StreamStructure> streamStructures, @Nullable StreamToken mutationSourceToken) {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
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

    int updateCnt = 0;
    int addFeatureCnt = 0;
    boolean cleared = false;
    SessionMutation sessionMutation = store.editSession(streamSession);
    for (StreamStructure streamStructure : streamStructures) {
      if (streamStructure.getOperation() == Operation.UPDATE_OR_APPEND) {
        String contentKey = protocolUtils.getContentIdKey(streamStructure.getContentId());
        if (contentInSession.contains(contentKey)) {
          // this is an update operation so we can ignore it
          updateCnt++;
          continue;
        }

        sessionMutation.add(streamStructure);
        contentInSession.add(contentKey);
        addFeatureCnt++;
        continue;
      } else if (streamStructure.getOperation() == Operation.REMOVE) {
        Logger.i(TAG, "Removing Item from HEAD");
        String contentKey = protocolUtils.getContentIdKey(streamStructure.getContentId());
        if (contentInSession.contains(contentKey)) {
          sessionMutation.add(streamStructure);
          contentInSession.remove(contentKey);
        } else {
          Logger.w(TAG, "Remove operation content not found in $HEAD");
        }
        continue;
      } else if (streamStructure.getOperation() == Operation.CLEAR_ALL) {
        contentInSession.clear();
        cleared = true;
        continue;
      }
      Logger.e(TAG, "Unknown operation, ignoring: %s", streamStructure.getOperation());
    }
    SessionMutationResults result = sessionMutation.commit();
    if (result.getStatus() == Status.SUCCESS) {
      timeTracker.stop(
          "updateSession",
          streamSession.streamToken,
          "cleared",
          cleared,
          "features",
          addFeatureCnt,
          "updates",
          updateCnt);
    } else {
      timeTracker.stop("updateSessionFailure", streamSession.streamToken);
      storeMutationFailures++;
      // TODO: What do we do if this fails?
      Logger.e(TAG, "$HEAD session mutation failed");
    }
  }

  @Override
  public StreamSession getStreamSession() {
    return streamSession;
  }

  // This is called only in tests to acess the content in the session.
  public Set<String> getContentInSessionForTest() {
    return new HashSet<>(contentInSession);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("sessionName").value(streamSession.streamToken);
    dumper.forKey("contentInSession").value(contentInSession.size()).compactPrevious();
    dumper.forKey("updateCount").value(updateCount).compactPrevious();
    dumper.forKey("storeMutationFailures").value(storeMutationFailures).compactPrevious();
  }
}
