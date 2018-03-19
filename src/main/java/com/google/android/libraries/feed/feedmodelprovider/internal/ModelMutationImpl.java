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

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.modelprovider.ModelMutation;
import com.google.android.libraries.feed.common.Committer;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link ModelMutation}. When the mutation is committed updating the Model
 */
public class ModelMutationImpl implements ModelMutation {

  /** The data representing the Change to the model. */
  public static class Change {
    final List<ChildStructureMutation> streamChildList = new ArrayList<>();
    final List<StreamStructure> updateList = new ArrayList<>();
    public StreamToken mutationSourceToken;
  }

  private final Committer<Void, Change> committer;
  @VisibleForTesting final Change change = new Change();

  public ModelMutationImpl(Committer<Void, Change> committer) {
    this.committer = committer;
  }

  @Override
  public ModelMutation addChild(StreamStructure streamStructure) {
    change.streamChildList.add(
        new ChildStructureMutation(
            Operation.UPDATE_OR_APPEND,
            new UpdatableModelChild(
                streamStructure.getContentId(), streamStructure.getParentContentId()),
            null));
    return this;
  }

  @Override
  public ModelMutation removeChild(StreamStructure streamStructure) {
    change.streamChildList.add(new ChildStructureMutation(Operation.REMOVE, null, streamStructure));
    return this;
  }

  @Override
  public ModelMutation updateChild(StreamStructure updateChild) {
    change.updateList.add(updateChild);
    return this;
  }

  @Override
  public ModelMutation setMutationSourceToken(StreamToken mutationSourceToken) {
    change.mutationSourceToken = mutationSourceToken;
    return this;
  }

  @Override
  public void commit() {
    committer.commit(change);
  }
}
