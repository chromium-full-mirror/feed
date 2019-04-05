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

package com.google.android.libraries.feed.common.concurrent.testing;

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import com.google.android.libraries.feed.common.concurrent.CancelableRunnableTask;
import com.google.android.libraries.feed.common.concurrent.CancelableTask;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A {@link MainThreadRunner} which listens to a {@link FakeClock} to determine when to execute
 * delayed tasks. This class can optionally execute tasks immediately and enforce thread checks.
 */
public final class ClockBackedFakeMainThreadRunner extends MainThreadRunner {

  private final FakeClock fakeClock;
  private final FakeThreadUtils fakeThreadUtils;
  private final List<Runnable> tasksToRun = new ArrayList<>();
  private final boolean shouldQueueTasks;

  // Suppressing use of Comparator, which is fine because this is only used in tests.
  @SuppressWarnings("AndroidJdkLibsChecker")
  private final PriorityQueue<TimedRunnable> delayedTasks =
      new PriorityQueue<>(Comparator.comparingLong(TimedRunnable::getExecutionTime));

  private int completedTaskCount = 0;

  public static ClockBackedFakeMainThreadRunner create(FakeClock fakeClock) {
    return new ClockBackedFakeMainThreadRunner(
        fakeClock,
        new FakeThreadUtils(/* enforceThreadChecks= */ false),
        /* shouldQueueTasks= */ false);
  }

  public static ClockBackedFakeMainThreadRunner runTasksImmediately() {
    return create(new FakeClock());
  }

  public static ClockBackedFakeMainThreadRunner runTasksImmediatelyWithThreadChecks(
      FakeThreadUtils fakeThreadUtils) {
    return new ClockBackedFakeMainThreadRunner(
        new FakeClock(), fakeThreadUtils, /* shouldQueueTasks= */ false);
  }

  public static ClockBackedFakeMainThreadRunner queueAllTasks() {
    return new ClockBackedFakeMainThreadRunner(
        new FakeClock(),
        new FakeThreadUtils(/* enforceThreadChecks= */ false),
        /* shouldQueueTasks= */ true);
  }

  private ClockBackedFakeMainThreadRunner(
      FakeClock fakeClock, FakeThreadUtils fakeThreadUtils, boolean shouldQueueTasks) {
    this.fakeClock = fakeClock;
    this.fakeThreadUtils = fakeThreadUtils;
    this.shouldQueueTasks = shouldQueueTasks;
    fakeClock.addClockUpdateListener(
        (newCurrentTime, newElapsedRealtime) -> runTasksBefore(newElapsedRealtime));
  }

  private void runTasksBefore(long newElapsedRealtime) {
    TimedRunnable nextTask;
    while ((nextTask = delayedTasks.peek()) != null) {
      if (nextTask.getExecutionTime() > newElapsedRealtime) {
        break;
      }

      Runnable task = checkNotNull(delayedTasks.poll());
      tasksToRun.add(task);
    }

    if (!shouldQueueTasks) {
      runAllTasks();
    }
  }

  @Override
  public void execute(String name, Runnable runnable) {
    tasksToRun.add(runnable);
    if (!shouldQueueTasks) {
      runAllTasks();
    }
  }

  @Override
  public CancelableTask executeWithDelay(String name, Runnable runnable, long delayMs) {
    CancelableRunnableTask cancelable = new CancelableRunnableTask(runnable);
    delayedTasks.add(new TimedRunnable(cancelable, fakeClock.elapsedRealtime() + delayMs));
    return cancelable;
  }

  /** Runs all eligible tasks. */
  public void runAllTasks() {
    boolean policy = fakeThreadUtils.enforceMainThread(true);
    for (Runnable task : tasksToRun) {
      task.run();
      completedTaskCount++;
    }
    tasksToRun.clear();
    fakeThreadUtils.enforceMainThread(policy);
  }

  /** Returns {@literal true} if there are tasks to run or tasks have run. */
  public boolean hasTasks() {
    return !tasksToRun.isEmpty() || completedTaskCount != 0;
  }

  /** Returns the number of tasks that have run. */
  public int getCompletedTaskCount() {
    return completedTaskCount;
  }

  private static final class TimedRunnable implements Runnable {

    private final long executionTime;
    private final Runnable runnable;

    private TimedRunnable(Runnable runnable, long executeTime) {
      this.runnable = runnable;
      this.executionTime = executeTime;
    }

    long getExecutionTime() {
      return executionTime;
    }

    @Override
    public void run() {
      runnable.run();
    }
  }
}
