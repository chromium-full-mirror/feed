// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.feedsessionmanager.internal.testing;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.modelprovider.ModelMutation;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.ArrayList;
import java.util.List;

/** Fake ModelMutation for tests. */
public final class FakeModelMutation implements ModelMutation {
  public final List<StreamStructure> addedChildren = new ArrayList<>();
  final List<StreamStructure> removedChildren = new ArrayList<>();
  final List<StreamStructure> updateChildren = new ArrayList<>();
  MutationContext mutationContext;
  boolean commitCalled = false;

  @Override
  public ModelMutation addChild(StreamStructure streamStructure) {
    addedChildren.add(streamStructure);
    return this;
  }

  @Override
  public ModelMutation removeChild(StreamStructure streamStructure) {
    removedChildren.add(streamStructure);
    return this;
  }

  @Override
  public ModelMutation updateChild(StreamStructure streamStructure) {
    updateChildren.add(streamStructure);
    return this;
  }

  @Override
  public ModelMutation setMutationContext(MutationContext mutationContext) {
    this.mutationContext = mutationContext;
    return this;
  }

  @Override
  public ModelMutation setSessionId(String sessionId) {
    return this;
  }

  @Override
  public ModelMutation hasCachedBindings(boolean cachedBindings) {
    return this;
  }

  @Override
  public void commit() {
    commitCalled = true;
  }

  void clearCommit() {
    commitCalled = false;
  }
}
