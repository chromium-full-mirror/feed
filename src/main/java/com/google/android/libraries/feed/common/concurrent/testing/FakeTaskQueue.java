// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.common.concurrent.testing;

import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.testing.host.logging.FakeBasicLoggingApi;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * A fake {@link TaskQueue} implementation that uses a {@link FakeThreadUtils} instance to manage
 * the thread policy.
 */
public final class FakeTaskQueue extends TaskQueue {
  private final FakeThreadUtils fakeThreadUtils;

  /** A last-in, first-out stack of thread policies to restore. */
  private final Deque<Boolean> threadPolicies = new ArrayDeque<>();

  private boolean resetWasCalled = false;
  private boolean completeResetWasCalled = false;

  public FakeTaskQueue(Executor executor, Clock clock, FakeThreadUtils fakeThreadUtils) {
    super(
        new FakeBasicLoggingApi(),
        executor,
        FakeMainThreadRunner.runTasksImmediately(),
        clock,
        /* checkStarvation= */ false);
    this.fakeThreadUtils = fakeThreadUtils;
  }

  @Override
  protected void onPreTaskRun() {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    threadPolicies.push(policy);
  }

  @Override
  protected void onPostTaskRun() {
    boolean policy = threadPolicies.pop();
    fakeThreadUtils.enforceMainThread(policy);
  }

  @Override
  public void reset() {
    super.reset();
    resetWasCalled = true;
  }

  @Override
  public void completeReset() {
    super.completeReset();
    completeResetWasCalled = true;
  }

  public void resetCounts() {
    taskCount = 0;
    immediateTaskCount = 0;
    headInvalidateTaskCount = 0;
    headResetTaskCount = 0;
    userFacingTaskCount = 0;
    backgroundTaskCount = 0;
  }

  public int getBackgroundTaskCount() {
    return backgroundTaskCount;
  }

  public int getImmediateTaskCount() {
    return immediateTaskCount;
  }

  public int getUserFacingTaskCount() {
    return userFacingTaskCount;
  }

  public boolean resetWasCalled() {
    return resetWasCalled;
  }

  public boolean completeResetWasCalled() {
    return completeResetWasCalled;
  }
}
