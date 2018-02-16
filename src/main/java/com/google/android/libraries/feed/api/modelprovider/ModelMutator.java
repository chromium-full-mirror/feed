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

package com.google.android.libraries.feed.api.modelprovider;

import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;

/**
 * Mutator which will batch a set of atomic changes to the Model maintained by the ModelProvider.
 * Order is important, things will be stored in the order they are added to a parent feature.
 */
public interface ModelMutator {
  /** Add the structural information to the model as a child */
  ModelMutator addChild(StreamStructure streamStructure);

  /** Remove the model child defined by the structural information */
  ModelMutator removeChild(StreamStructure streamStructure);

  /** StreamToken which is the source of the response */
  ModelMutator setMutationSourceToken(StreamToken mutationSourceToken);

  /** Commits the pending changes to the store. */
  void commit();
}
