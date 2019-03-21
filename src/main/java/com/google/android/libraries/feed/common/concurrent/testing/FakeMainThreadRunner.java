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

import com.google.android.libraries.feed.common.concurrent.CancelableRunnableTask;
import com.google.android.libraries.feed.common.concurrent.CancelableTask;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** {@link MainThreadRunner} used for tests which is able to queue up tasks. */
public final class FakeMainThreadRunner extends MainThreadRunner {

  private final FakeThreadUtils fakeThreadUtils;
  private final boolean runTasksImmediately;

  @SuppressWarnings("AndroidJdkLibsChecker")
  private final Set<DelayedRunnable> delayedTasks =
      new TreeSet<>(Comparator.comparingLong(DelayedRunnable::getDelayMs));

  private final List<Runnable> immediateTasks = new ArrayList<>();
  private final List<Runnable> completedTasks = new ArrayList<>();

  public static FakeMainThreadRunner runTasksImmediately() {
    return new FakeMainThreadRunner(
        new FakeThreadUtils(/* enforceThreadChecks= */ false), /* runTasksImmediately= */ true);
  }

  public static FakeMainThreadRunner runTasksImmediatelyWithThreadChecks(
      FakeThreadUtils fakeThreadUtils) {
    return new FakeMainThreadRunner(fakeThreadUtils, /* runTasksImmediately= */ true);
  }

  public static FakeMainThreadRunner queueAllTasks() {
    return new FakeMainThreadRunner(
        new FakeThreadUtils(/* enforceThreadChecks= */ false), /* runTasksImmediately= */ false);
  }

  FakeMainThreadRunner(FakeThreadUtils fakeThreadUtils, boolean runTasksImmediately) {
    this.fakeThreadUtils = fakeThreadUtils;
    this.runTasksImmediately = runTasksImmediately;
  }

  @Override
  public void execute(String name, Runnable runnable) {
    immediateTasks.add(runnable);

    if (runTasksImmediately) {
      runAllTasks();
    }
  }

  @Override
  public CancelableTask executeWithDelay(String name, Runnable runnable, long delayMs) {
    CancelableRunnableTask cancelable = new CancelableRunnableTask(runnable);
    delayedTasks.add(new DelayedRunnable(cancelable, delayMs));
    if (runTasksImmediately) {
      runAllTasks();
    }
    return cancelable;
  }

  public void runAllTasks() {
    boolean policy = fakeThreadUtils.enforceMainThread(true);

    for (Runnable task : immediateTasks) {
      task.run();
      completedTasks.add(task);
    }
    for (DelayedRunnable task : delayedTasks) {
      task.getRunnable().run();
      completedTasks.add(task.getRunnable());
    }

    immediateTasks.clear();
    delayedTasks.clear();

    fakeThreadUtils.enforceMainThread(policy);
  }

  public boolean hasTasks() {
    return !immediateTasks.isEmpty() || !completedTasks.isEmpty();
  }

  public int getCompletedTaskCount() {
    return completedTasks.size();
  }

  private static final class DelayedRunnable {

    private final Runnable runnable;
    private final long delayMs;

    private DelayedRunnable(Runnable runnable, long delayMs) {
      this.runnable = runnable;
      this.delayMs = delayMs;
    }

    Runnable getRunnable() {
      return runnable;
    }

    long getDelayMs() {
      return delayMs;
    }
  }
}
