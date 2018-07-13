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

package com.google.android.libraries.feed.basicstream.internal;

import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.common.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/** Tracks the {@code contentId} of the deepest viewed content. */
public class DeepestContentTracker implements ViewDepthProvider {

  private static final String TAG = "DeepestContentTracker";

  private final List</*@Nullable*/ String> contentIds = new ArrayList<>();

  void updateDeepestContentTracker(int contentPosition, /*@Nullable*/ String contentId) {
    // Do not add contentId if already present.
    if (contentPosition < contentIds.size()) {
      if (contentId != null && !contentId.equals(contentIds.get(contentPosition))) {
        Logger.wtf(
            TAG,
            "Adding contentId %s at %d when position is already being tracked.",
            contentId,
            contentPosition);
      }
      return;
    }

    contentIds.add(contentId);
  }

  void removeContentId(int contentPosition) {
    if (contentPosition >= contentIds.size()) {
      return;
    }

    contentIds.remove(contentPosition);
  }

  public void reset() {
    contentIds.clear();
  }

  @Override
  /*@Nullable*/
  public String getChildViewDepth() {
    if (contentIds.isEmpty()) {
      return null;
    }

    int i = contentIds.size() - 1;
    while (contentIds.get(i) == null && i > 0) {
      i--;
    }

    return contentIds.get(i);
  }
}
