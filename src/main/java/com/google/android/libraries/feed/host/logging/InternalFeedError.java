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

/** Represents internal errors of the Feed libraries. */
@IntDef({
  InternalFeedError.SWITCH_TO_EPHEMERAL,
  InternalFeedError.NO_URL_FOR_OPEN,
  InternalFeedError.FAILED_TO_RESTORE
})
public @interface InternalFeedError {

  /** Represents when memory fails so the Feed libraries switch to ephemeral mode. */
  int SWITCH_TO_EPHEMERAL = 0;

  /** Represents when opening a URL fails as there is no URL available. */
  int NO_URL_FOR_OPEN = 1;

  /** Represents a failure to restore. */
  int FAILED_TO_RESTORE = 2;
}
