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

package com.google.android.libraries.feed.feedmodelprovider.internal;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ChildMutation;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ChildUpdateMutation;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ModelMutatorCommitter;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bind the {@link StreamPayload} objects to the currently unbound {@link ModelChild objects}. */
public class ModelChildBinder {
  private static final String TAG = "ModelChildBinder";

  private final SessionManager sessionManager;
  private final CursorProvider cursorProvider;
  private final TimingUtils timingUtils;

  public ModelChildBinder(
      SessionManager sessionManager, CursorProvider cursorProvider, TimingUtils timingUtils) {
    this.sessionManager = sessionManager;
    this.cursorProvider = cursorProvider;
    this.timingUtils = timingUtils;
  }

  void bindAndCommit(
      ModelMutatorCommitter committer,
      List<ChildMutation> streamChildrenList,
      List<StreamStructure> updateChildrenList,
      StreamToken mutationSourceToken) {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Map<String, UpdatableModelChild> unboundChildren = new HashMap<>();
    List<String> contentIds = new ArrayList<>();
    for (ChildMutation childMutation : streamChildrenList) {
      if (childMutation.operation == Operation.UPDATE_OR_APPEND) {
        Validators.checkNotNull(childMutation.addChild);
        if (childMutation.addChild.getType() == Type.UNBOUND) {
          String key = childMutation.addChild.getContentIdAsKey();
          unboundChildren.put(key, childMutation.addChild);
          contentIds.add(childMutation.addChild.getContentId());
        }
      }
    }
    Map<String, StreamStructure> updateChildren = new HashMap<>();
    for (StreamStructure updateChild : updateChildrenList) {
      contentIds.add(updateChild.getContentId());
      updateChildren.put(updateChild.getContentId(), updateChild);
    }
    sessionManager.getStreamFeatures(
        contentIds,
        payloads -> {
          List<ChildUpdateMutation> updates = new ArrayList<>();
          for (PayloadWithId childPayload : payloads) {
            String key = childPayload.contentId;
            UpdatableModelChild child = unboundChildren.get(key);
            if (child != null) {
              StreamPayload payload = childPayload.payload;
              if (payload != null) {
                if (payload.hasStreamFeature()) {
                  child.bindFeature(
                      new UpdatableModelFeature(payload.getStreamFeature(), cursorProvider));
                } else if (payload.hasStreamToken()) {
                  child.bindToken(new UpdatableModelToken(payload.getStreamToken()));
                }
              } else {
                Logger.e(TAG, "Payload not provided in the PayloadWithId");
              }
            } else {
              Logger.i(TAG, "Found Updated Content %s", key);
              StreamStructure structure = updateChildren.get(key);
              if (structure != null) {
                updates.add(new ChildUpdateMutation(structure, childPayload.payload));
              }
            }
          }
          timeTracker.stop("", "bindingChildren", "unboundChildren", unboundChildren.size());
          committer.commit(streamChildrenList, updates, mutationSourceToken);
        });
  }
}
