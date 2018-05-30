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
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import java.util.concurrent.TimeUnit;

/**
 * Concrete impl of {@link SchedulerApi}. The implementation will timeout session creation when a
 * request is running too long and will show existing content.
 */
public class TimeoutScheduler implements SchedulerApi {
  private static final String TAG = "TimeoutScheduler";

  // TODO: What should we use for the defaults?
  private static final long TIMEOUT_REQUEST_WITH_WAIT_MS = TimeUnit.HOURS.toMillis(8);
  private static final long TIMEOUT_REQUEST_WITH_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);

  private final ThreadUtils threadUtils;
  private final Clock clock;
  private final boolean useTimeScheduler;
  private final long requestWithWaitMs;
  private final long requestWithTimeoutMs;

  public TimeoutScheduler(ThreadUtils threadUtils, Clock clock, Configuration configuration) {
    this.threadUtils = threadUtils;
    this.clock = clock;
    useTimeScheduler = configuration.getValueOrDefault(ConfigKey.USE_TIMEOUT_SCHEDULER, false);
    requestWithWaitMs =
        configuration.getValueOrDefault(
            ConfigKey.TIMEOUT_REQUEST_WITH_WAIT_MS, TIMEOUT_REQUEST_WITH_WAIT_MS);
    requestWithTimeoutMs =
        configuration.getValueOrDefault(
            ConfigKey.TIMEOUT_REQUEST_WITH_TIMEOUT_MS, TIMEOUT_REQUEST_WITH_TIMEOUT_MS);
  }

  @Override
  @RequestBehavior
  public int shouldSessionRequestData(SessionManagerState sessionManagerState) {
    threadUtils.checkMainThread();
    if (!useTimeScheduler) {
      Logger.i(TAG, "shouldSessionRequestData - disabled");
      return RequestBehavior.NO_REQUEST_WITH_CONTENT;
    }
    Logger.i(
        TAG,
        "shouldSessionRequestData, content %s, contentCreationMs %s, request %s",
        sessionManagerState.hasContent,
        StringFormattingUtils.formatLogDate(sessionManagerState.contentCreationDateTimeMs),
        sessionManagerState.hasOutstandingRequest);

    if (!sessionManagerState.hasContent) {
      // No Content
      Logger.i(TAG, "shouldSessionRequestData - No Content found: REQUEST_WITH_WAIT");
      return RequestBehavior.REQUEST_WITH_WAIT;
    }
    long deltaTime = clock.currentTimeMillis() - sessionManagerState.contentCreationDateTimeMs;
    Logger.i(TAG, "Delta: [%s]", deltaTime);
    if (deltaTime > requestWithWaitMs) {
      Logger.i(
          TAG,
          "shouldSessionRequestData [%s | %s]: REQUEST_WITH_WAIT",
          deltaTime,
          requestWithWaitMs);
      return RequestBehavior.REQUEST_WITH_WAIT;
    }
    if (deltaTime > requestWithTimeoutMs) {
      Logger.i(
          TAG,
          "shouldSessionRequestData [%s | %s]: REQUEST_WITH_TIMEOUT",
          deltaTime,
          requestWithTimeoutMs);
      return RequestBehavior.REQUEST_WITH_TIMEOUT;
    }
    Logger.i(TAG, "Default: NO_REQUEST_WITH_CONTENT");
    return RequestBehavior.NO_REQUEST_WITH_CONTENT;
  }

  @Override
  public void onReceiveNewContent(long contentCreationDateTimeMs) {
    threadUtils.checkMainThread();
    Logger.i(
        TAG,
        "onReceiveNewContent %s",
        StringFormattingUtils.formatLogDate(contentCreationDateTimeMs));
  }

  @Override
  public void onRequestError(int networkResponseCode) {
    threadUtils.checkMainThread();
    // Do nothing
  }
}
