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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.List;

/** SessionImpl which implements the TimeoutScheduler specific behaviors need within the session. */
// TODO: Should TimeoutSessionImpl and SessionImpl extend an abstract base class instead of
// doing it this way?
public class TimeoutSessionImpl extends SessionImpl {
  private static final String TAG = "TimeoutSessionImpl";

  TimeoutSessionImpl(
      Store store, TaskQueue taskQueue, TimingUtils timingUtils, ThreadUtils threadUtils) {
    super(store, taskQueue, timingUtils, threadUtils);
    Logger.i(TAG, "Using TimeoutSessionImpl");
  }

  @Override
  public void updateSession(
      boolean clearHead,
      List<StreamStructure> streamStructures,
      /*@Nullable*/ StreamToken mutationSourceToken) {
    Validators.checkNotNull(streamSession);
    updateCount++;
    updateSessionInternal(streamStructures, mutationSourceToken);
  }

  @Override
  public boolean invalidateOnResetHead() {
    return false;
  }
}
