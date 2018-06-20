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

/** Tracks the {@code contentId} of the deepest viewed content. */
public class DeepestContentTracker implements ViewDepthProvider {

  private static final String TAG = "DeepestContentTracker";
  private static final int NO_CONTENT_VIEWED = -1;

  /*@Nullable*/ private String deepestViewedContentId;

  private int deepestViewedContentPosition = NO_CONTENT_VIEWED;

  void updateDeepestContentTracker(int contentPosition, /*@Nullable*/ String contentId) {
    if (contentId == null) {
      return;
    }

    // Explicitly updating the deepestViewedContentId in the case where we update with the same
    // position. The reason for that is situation where the user sees the 5th card and then
    // dismisses the 4th card, bringing what was the 6th card into view. However, that card is now
    // at index 5.
    if (contentPosition >= deepestViewedContentPosition) {
      deepestViewedContentPosition = contentPosition;
      deepestViewedContentId = contentId;
    }
  }

  public void reset() {
    deepestViewedContentId = null;
    deepestViewedContentPosition = NO_CONTENT_VIEWED;
  }

  @Override
  /*@Nullable*/
  public String getChildViewDepth() {
    return deepestViewedContentId;
  }
}
