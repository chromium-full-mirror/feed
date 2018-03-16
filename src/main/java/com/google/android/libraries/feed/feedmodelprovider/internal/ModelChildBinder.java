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
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.Change;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bind the {@link StreamPayload} objects to the currently unbound {@link ModelChild objects}. */
public class ModelChildBinder {
  private static final String TAG = "ModelChildBinder";

  private final SessionManager sessionManager;
  private final CursorProvider cursorProvider;
  private final TimingUtils timingUtils;

  /**
   * After binding the children, the {@link #bindChildren(Consumer, Change)} will call the {@link
   * Consumer<>} with the {@code BindingResults}.
   */
  public static class BindingResults {
    public final List<ChildStructureMutation> streamChildList;
    public final List<ChildBinding> updates;
    public final StreamToken mutationSourceToken;

    private BindingResults(
        List<ChildStructureMutation> streamChildList,
        List<ChildBinding> updates,
        StreamToken mutationSourceToken) {
      this.streamChildList = Collections.unmodifiableList(streamChildList);
      this.updates = Collections.unmodifiableList(updates);
      this.mutationSourceToken = mutationSourceToken;
    }
  }

  public ModelChildBinder(
      SessionManager sessionManager, CursorProvider cursorProvider, TimingUtils timingUtils) {
    this.sessionManager = sessionManager;
    this.cursorProvider = cursorProvider;
    this.timingUtils = timingUtils;
  }

  public void bindChildren(Consumer<BindingResults> consumer, Change change) {

    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    Map<String, UpdatableModelChild> unboundChildren = new HashMap<>();
    List<String> contentIds = new ArrayList<>();
    for (ChildStructureMutation childStructureMutation : change.streamChildList) {
      if (childStructureMutation.operation == Operation.UPDATE_OR_APPEND) {
        Validators.checkNotNull(childStructureMutation.addChild);
        if (childStructureMutation.addChild.getType() == Type.UNBOUND) {
          String key = childStructureMutation.addChild.getContentIdAsKey();
          unboundChildren.put(key, childStructureMutation.addChild);
          contentIds.add(childStructureMutation.addChild.getContentId());
        }
      }
    }
    Map<String, StreamStructure> updateChildren = new HashMap<>();
    for (StreamStructure updateChild : change.updateList) {
      contentIds.add(updateChild.getContentId());
      updateChildren.put(updateChild.getContentId(), updateChild);
    }
    sessionManager.getStreamFeatures(
        contentIds,
        payloads -> {
          List<ChildBinding> updates = new ArrayList<>();
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
                updates.add(new ChildBinding(structure, childPayload.payload));
              }
            }
          }
          BindingResults bindingResults =
              new BindingResults(change.streamChildList, updates, change.mutationSourceToken);
          timeTracker.stop("", "bindingChildren", "unboundChildren", unboundChildren.size());
          consumer.accept(bindingResults);
        });
  }
}
