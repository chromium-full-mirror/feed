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

package com.google.android.libraries.feed.hostimpl.scheduler;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.logging.StringFormattingUtils;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;

/**
 * Concrete impl of {@link SchedulerApi}. The implementation will timeout session creation when a
 * request is running too long and will show existing content.
 */
public class TimeoutScheduler implements SchedulerApi {
  private static final String TAG = "FeedSchedulerApiImpl";
  private final ThreadUtils threadUtils;

  private long contentCreationMs;

  public TimeoutScheduler(ThreadUtils threadUtils) {
    this.threadUtils = threadUtils;
  }

  @Override
  @RequestBehavior
  public int shouldSessionRequestData(SessionManagerState sessionManagerState) {
    threadUtils.checkMainThread();
    Logger.i(
        TAG,
        "shouldSessionRequestData, content %s, contentCreationMs %s, request %s",
        sessionManagerState.hasContent,
        sessionManagerState.contentCreationDateTimeMs,
        sessionManagerState.hasOutstandingRequest);
    if (contentCreationMs == 0) {
      return RequestBehavior.REQUEST_WITH_WAIT;
      // No Content
    }
    return RequestBehavior.REQUEST_WITH_WAIT;
  }

  @Override
  public void onReceiveNewContent(long contentCreationDateTimeMs) {
    threadUtils.checkMainThread();
    Logger.i(
        TAG,
        "onReceiveNewContent %s",
        StringFormattingUtils.formatLogDate(contentCreationDateTimeMs));
    contentCreationMs = contentCreationDateTimeMs;
  }

  @Override
  public void onRequestError(int networkResponseCode) {
    threadUtils.checkMainThread();
    // Do nothing
  }
}
