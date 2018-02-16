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

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ChildMutation;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ModelMutatorCommitter;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamPayloadWithId;
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
  private final InternalProtocolUtils internalProtocolUtils;

  public ModelChildBinder(
      SessionManager sessionManager,
      CursorProvider cursorProvider,
      TimingUtils timingUtils,
      InternalProtocolUtils internalProtocolUtils) {
    this.sessionManager = sessionManager;
    this.cursorProvider = cursorProvider;
    this.timingUtils = timingUtils;
    this.internalProtocolUtils = internalProtocolUtils;
  }

  void bindAndCommit(
      ModelMutatorCommitter committer,
      List<ChildMutation> streamChildList,
      StreamToken mutationSourceToken) {
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Map<String, ModelChild> unboundChildren = new HashMap<>();
    List<StreamContentId> contentIds = new ArrayList<>();
    for (ChildMutation childMutation : streamChildList) {
      if (childMutation.operation == Operation.UPDATE_OR_APPEND) {
        Validators.checkNotNull(childMutation.addChild);
        if (childMutation.addChild.getType() == Type.UNBOUND) {
          String key = internalProtocolUtils.getContentIdKey(childMutation.addChild.getContentId());
          unboundChildren.put(key, childMutation.addChild);
          contentIds.add(childMutation.addChild.getContentId());
        }
      }
    }
    sessionManager.getStreamFeatures(
        contentIds,
        payloads -> {
          for (StreamPayloadWithId childPayload : payloads) {
            String key = internalProtocolUtils.getContentIdKey(childPayload.getContentId());
            ModelChild child = unboundChildren.get(key);
            StreamPayload payload = childPayload.getPayload();
            if (child != null && payload != null) {
              if (payload.hasStreamFeature()) {
                child.bindFeature(new ModelFeatureImpl(payload.getStreamFeature(), cursorProvider));
              } else if (payload.hasStreamToken()) {
                child.bindToken(new ModelTokenImpl(payload.getStreamToken()));
              }
            } else {
              if (child == null) {
                Logger.e(TAG, "Didn't find a child to set a Payload on");
              } else {
                Logger.e(TAG, "Payload not provided in the StreamPayloadWithId");
              }
            }
          }
          timeTracker.stop("", "bindingChildren", "unboundChildren", unboundChildren.size());
          committer.commit(streamChildList, mutationSourceToken);
        });
  }
}
