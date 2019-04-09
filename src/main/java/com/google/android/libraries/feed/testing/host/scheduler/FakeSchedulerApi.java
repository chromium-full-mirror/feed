// Copyright 2019 The Feed Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License") {}
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

package com.google.android.libraries.feed.testing.host.scheduler;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;

/** Fake implementation of {@link SchedulerApi}. */
public final class FakeSchedulerApi implements SchedulerApi {
  private final ThreadUtils threadUtils;
  @RequestBehavior private int requestBehavior = RequestBehavior.NO_REQUEST_WITH_CONTENT;

  public FakeSchedulerApi(ThreadUtils threadUtils) {
    this.threadUtils = threadUtils;
  }

  @Override
  public void onReceiveNewContent(long contentCreationDateTimeMs) {}

  @Override
  public void onRequestError(int networkResponseCode) {}

  @Override
  @RequestBehavior
  public int shouldSessionRequestData(SessionManagerState sessionManagerState) {
    threadUtils.checkMainThread();
    return requestBehavior;
  }

  /** Sets the result returned from {@link shouldSessionRequestData(SessionManagerState)}. */
  public FakeSchedulerApi setRequestBehavior(@RequestBehavior int requestBehavior) {
    this.requestBehavior = requestBehavior;
    return this;
  }
}
