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

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;

/**
 * This is the internal representation of a ModelChild used by {@link
 * com.google.android.libraries.feed.feedmodelprovider.FeedModelProvider}.
 */
public interface UpdatableModelChild extends ModelChild {
  String getContentIdAsKey();

  /** If the {@code ModelChild} is a Feature, this will update it. */
  void updateFeature(StreamPayload payload);

  /**
   * Bind a {@link ModelFeature} to the child. This can only be called when if the ModelChild is
   * currently {@code UNBOUND}, otherwise it throws an {@link IllegalStateException}.
   */
  void bindFeature(UpdatableModelFeature modelFeature);

  /**
   * Bind a {@link ModelToken} to the child. This can only be called when if the ModelChild is
   * currently {@code UNBOUND}, otherwise it throws an {@link IllegalStateException}.
   */
  void bindToken(UpdatableModelToken modelToken);

  /** Return a the ModelToken as an UpdatableModelToken */
  UpdatableModelToken getUpdatableModelToken();
}
