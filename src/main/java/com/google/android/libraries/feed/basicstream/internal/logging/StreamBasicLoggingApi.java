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

package com.google.android.libraries.feed.basicstream.internal.logging;

import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;

// TODO: Replace with host implementation once host api is finalized.
/** Stand in API to log basic events. */
public class StreamBasicLoggingApi implements BasicLoggingApi {
  public static final String TAG = "StreamBasicLoggingApi";

  public void onContentClicked() {
    Logger.i(TAG, "onContentClicked");
  }

  public void onOpenedWithNoContent() {
    Logger.i(TAG, "onOpenedWithNoContent");
  }
}
