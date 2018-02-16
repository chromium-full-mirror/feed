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
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelMutator;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Implementation of the {@link ModelMutator}. When the mutation is committed updating the Model
 * Provider is done through the {@link ModelMutatorCommitter}.
 */
public class ModelMutatorImpl implements ModelMutator {

  /** Callback method for committing the mutation */
  public interface ModelMutatorCommitter {
    /** Called to commit the mutation */
    void commit(List<ChildMutation> streamChildList, StreamToken mutationSourceToken);
  }

  public static class ChildMutation {
    public final StreamStructure.Operation operation;
    @Nullable public final ModelChild addChild;
    @Nullable public final StreamStructure removeChild;

    ChildMutation(
        StreamStructure.Operation operation, ModelChild addChild, StreamStructure removeChild) {
      this.operation = operation;
      this.addChild = addChild;
      this.removeChild = removeChild;
    }
  }

  private final ModelMutatorCommitter committer;
  private final ModelChildFactory modelChildFactory;
  private final ModelChildBinder modelChildBinder;

  @VisibleForTesting final List<ChildMutation> streamChildList = new ArrayList<>();
  private StreamToken mutationSourceToken;

  public ModelMutatorImpl(
      ModelMutatorCommitter committer,
      ModelChildFactory modelChildFactory,
      ModelChildBinder modelChildBinder) {
    this.committer = committer;
    this.modelChildFactory = modelChildFactory;
    this.modelChildBinder = modelChildBinder;
  }

  @Override
  public ModelMutator addChild(StreamStructure streamStructure) {
    StreamContentId parentId =
        streamStructure.hasParentContentId() ? streamStructure.getParentContentId() : null;
    streamChildList.add(
        new ChildMutation(
            Operation.UPDATE_OR_APPEND,
            modelChildFactory.createUnboundChild(streamStructure.getContentId(), parentId),
            null));
    return this;
  }

  @Override
  public ModelMutator removeChild(StreamStructure streamStructure) {
    streamChildList.add(new ChildMutation(Operation.REMOVE, null, streamStructure));
    return this;
  }

  @Override
  public ModelMutator setMutationSourceToken(StreamToken mutationSourceToken) {
    this.mutationSourceToken = mutationSourceToken;
    return this;
  }

  @Override
  public void commit() {
    // The binder runs to bind the payloads into the newly created ModelChild instances
    // and updates the model.
    // TODO: This assumes we update the full model instead of partially updating the
    // payloads.
    modelChildBinder.bindAndCommit(committer, streamChildList, mutationSourceToken);
  }
}
