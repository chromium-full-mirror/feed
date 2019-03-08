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

package com.google.android.libraries.feed.host.logging;

import android.support.annotation.IntDef;

/**
 * IntDef representing the different types of Elements.
 *
 * <p>When adding new values, the value of {@link ElementType#NEXT_VALUE} should be used and
 * incremented. When removing values, {@link ElementType#NEXT_VALUE} should not be changed, and
 * those values should not be reused.
 */
@IntDef({
  ElementType.UNKNOWN,
  ElementType.CARD_LARGE_IMAGE,
  ElementType.CARD_SMALL_IMAGE,
  ElementType.INTEREST_HEADER,
  ElementType.TOOLTIP,
  ElementType.NEXT_VALUE,
})
// LINT.IfChange
public @interface ElementType {
  int UNKNOWN = 0;
  int CARD_LARGE_IMAGE = 1;
  int CARD_SMALL_IMAGE = 2;
  int INTEREST_HEADER = 3;
  int TOOLTIP = 4;
  int NEXT_VALUE = 5;
}
// LINT.ThenChange
