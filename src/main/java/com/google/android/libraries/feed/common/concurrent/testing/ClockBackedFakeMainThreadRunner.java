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

import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A {@link MainThreadRunner} which immediately executes non-delayed tasks, and executes delayed
 * tasks by listening to a {@link FakeClock}.
 */
public final class ClockBackedFakeMainThreadRunner extends MainThreadRunner {

  private final FakeClock fakeClock;

  // Suppressing use of Comparator, which is fine because this is only used in tests.
  @SuppressWarnings("AndroidJdkLibsChecker")
  private final PriorityQueue<TimedRunnable> delayedTasks =
      new PriorityQueue<>(Comparator.comparingLong(TimedRunnable::getExecutionTime));

  public static ClockBackedFakeMainThreadRunner create(FakeClock fakeClock) {
    return new ClockBackedFakeMainThreadRunner(fakeClock);
  }

  private ClockBackedFakeMainThreadRunner(FakeClock fakeClock) {
    this.fakeClock = fakeClock;
    fakeClock.addClockUpdateListener(
        (newCurrentTime, newElapsedRealtime) -> runTasksBefore(newElapsedRealtime));
  }

  private void runTasksBefore(long newElapsedRealtime) {
    TimedRunnable nextTask;
    while ((nextTask = delayedTasks.peek()) != null) {
      if (nextTask.getExecutionTime() > newElapsedRealtime) {
        return;
      }

      checkNotNull(delayedTasks.poll()).run();
    }
  }

  @Override
  public void execute(String name, Runnable runnable) {
    runnable.run();
  }

  @Override
  public void executeWithDelay(String name, Runnable runnable, long delayMs) {
    delayedTasks.add(new TimedRunnable(runnable, fakeClock.elapsedRealtime() + delayMs));
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
