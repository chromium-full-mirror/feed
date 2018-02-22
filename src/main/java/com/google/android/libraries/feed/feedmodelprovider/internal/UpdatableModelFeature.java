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

import com.google.android.libraries.feed.api.modelprovider.ModelChangeObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import java.util.List;

/**
 * This is the internal representation of a {@link ModelFeature} used by {@link
 * com.google.android.libraries.feed.feedmodelprovider.FeedModelProvider}.
 */
public interface UpdatableModelFeature extends ModelFeature {
  /** Set the {@link StreamFeature} value for this {@link ModelFeature}. */
  void setFeatureValue(StreamFeature streamFeature);

  /** Returns a list of {@link ModelChangeObserver} to notify when a change is made. */
  List<ModelChangeObserver> getObserversToNotify();
}
