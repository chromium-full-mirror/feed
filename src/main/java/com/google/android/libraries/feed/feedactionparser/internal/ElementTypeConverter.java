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

package com.google.android.libraries.feed.feedactionparser.internal;

import com.google.android.libraries.feed.host.logging.ElementType;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata;

/** Utility class to convert a {@link FeedActionMetadata.ElementType} to {@link ElementType}. */
public final class ElementTypeConverter {
  private ElementTypeConverter() {}

  @ElementType
  public static int convert(FeedActionMetadata.ElementType type) {
    // LINT.IfChange
    switch (type) {
      case CARD_LARGE_IMAGE:
        return ElementType.CARD_LARGE_IMAGE;
      case CARD_SMALL_IMAGE:
        return ElementType.CARD_SMALL_IMAGE;
      case INTEREST_HEADER:
        return ElementType.INTEREST_HEADER;
      case TOOLTIP:
        return ElementType.TOOLTIP;
      default:
        return ElementType.UNKNOWN;
    }
    // LINT.ThenChange
  }
}
