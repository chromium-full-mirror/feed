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

package com.google.android.libraries.feed.common.concurrent;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.GuardedBy;

/**
 * This class is responsible for running tasks on the Feed single-threaded Executor. The primary job
 * of this class is to run high priority tasks and to delay certain task until other complete. When
 * we are delaying tasks, they will be added to a set of queues and will run in order within the
 * task priority. There are three priorities of tasks defined:
 *
 * <ol>
 *   <li>Initialization, HEAD_INVALIDATE, HEAD_RESET - These tasks will be placed on the Executor
 *       when they are received.
 *   <li>USER_FACING - These tasks are high priority, running after the immediate tasks.
 *   <li>BACKGROUND - These are low priority tasks which run after all other tasks finish.
 * </ol>
 *
 * <p>The {@code TaskQueue} start in initialization mode. All tasks will be delayed until we
 * initialization is completed. The {@link #executeInitialization} method is run to initialize the
 * FeedSessionManager. We also enter delayed mode when we either reset the $HEAD or invalidate the
 * $HEAD. For HEAD_RESET, we are making a request which will complete. Once it's complete, we will
 * process any delayed tasks. HEAD_INVALIDATE simply clears the contents of $HEAD. The expectation
 * is a future HEAD_RESET will populate $HEAD. Once the delay is cleared, we will run the
 * USER_FACING tasks followed by the BACKGROUND tasks. Once all of these tasks have run, we will run
 * tasks immediately until we either have a task which is of type HEAD_INVALIDATE or HEAD_RESET.
 */
public class TaskQueue implements Dumpable {
  private static final String TAG = "TaskQueue";

  /** TaskType identifies the type of task being run and implicitly the priority of the task */
  @IntDef({
    TaskType.UNKNOWN,
    TaskType.POST_INIT,
    TaskType.HEAD_INVALIDATE,
    TaskType.HEAD_RESET,
    TaskType.USER_FACING,
    TaskType.BACKGROUND,
  })
  public @interface TaskType {
    // Unknown task priority, shouldn't be used, this will be treated as a background task
    int UNKNOWN = 0;
    // Runs immediately, post initialization task which waited for initialization to be finished
    int POST_INIT = 1;
    // Runs immediately, $HEAD is invalidated (cleared) and delay tasks until HEAD_RESET
    int HEAD_INVALIDATE = 2;
    // Runs immediately, indicates the task will create a new $HEAD instance.
    // Once finished, start running other tasks until the delayed tasks are all run.
    int HEAD_RESET = 3;
    // User facing task which should run at a higher priority than Background.
    int USER_FACING = 4;
    // Background tasks run at the lowest priority
    int BACKGROUND = 5;
  }

  private final Object lock = new Object();

  @GuardedBy("lock")
  private final Queue<TaskWrapper> immediateTasks = new ArrayDeque<>();

  @GuardedBy("lock")
  private final Queue<TaskWrapper> userTasks = new ArrayDeque<>();

  @GuardedBy("lock")
  private final Queue<TaskWrapper> backgroundTasks = new ArrayDeque<>();

  @GuardedBy("lock")
  private boolean waitingForHeadReset = false;

  @GuardedBy("lock")
  private boolean initialized = false;

  private final ExecutorService executor;
  private final Handler mainThreadHandler;

  // counters used for dump
  private int taskCount = 0;
  private int immediateTasksCount = 0;
  private int delayedTaskCount = 0;
  private int postInitTaskCount = 0;
  private int headInvalidateTaskCount = 0;
  private int headResetTaskCount = 0;
  private int userFacingTaskCount = 0;
  private int backgroundTaskCount = 0;
  private int maxImmediateTasks = 0;
  private int maxUserFacingTasks = 0;
  private int maxBackgroundTasks = 0;

  public TaskQueue(ExecutorService executor) {
    this.executor = executor;
    mainThreadHandler = new Handler(Looper.getMainLooper());
  }

  /** Returns {@code true} if we are delaying for a request */
  public boolean isMakingRequest() {
    synchronized (lock) {
      return waitingForHeadReset;
    }
  }

  /**
   * This method will reset the task queue. This means we will delay all tasks created until the
   * {@link #initialize(Runnable)} task is called again. Also any currently delayed tasks will be
   * removed from the Queue and not run.
   */
  public void reset() {
    synchronized (lock) {
      waitingForHeadReset = false;
      initialized = false;

      // clear all delayed tasks
      immediateTasks.clear();
      userTasks.clear();
      backgroundTasks.clear();
    }
  }

  /**
   * Called to initialize the {@link
   * com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager}. This needs to be the
   * first task run, all other tasks are delayed until initialization finishes.
   */
  public void initialize(Runnable runnable) {
    // TODO: Should this have a TaskType.INIT?
    TaskWrapper task =
        new BaseTaskWrapper(
            "initialize", TaskType.POST_INIT, () -> executeInitialization(runnable));
    task.runTask();
  }

  /** Execute a Task on the Executor. */
  public void execute(String task, @TaskType int taskType, Runnable runnable) {
    execute(task, taskType, runnable, null, 0);
  }

  /** Execute a task providing a timeout task */
  public void execute(
      String task,
      @TaskType int taskType,
      Runnable runnable,
      /*@Nullable*/ Runnable timeOutRunnable,
      long timeoutMillis) {

    Logger.i(TAG, " - task[%s - %s]: %s", taskType, isDelayed(), task);
    countTask(taskType);
    boolean queueTask = isDelayed();
    TaskWrapper taskWrapper;
    Runnable taskRunnable = getRunnable(runnable, taskType);
    if (timeOutRunnable != null) {
      taskWrapper =
          new TimeoutTaskWrapper(task, taskType, taskRunnable, timeOutRunnable)
              .startTimeout(timeoutMillis);
    } else {
      taskWrapper = new BaseTaskWrapper(task, taskType, taskRunnable);
    }
    scheduleTask(queueTask, taskWrapper, taskType);
  }

  private void scheduleTask(boolean queueTask, TaskWrapper taskWrapper, @TaskType int taskType) {
    if (queueTask) {
      delayedTaskCount++;
      queueTask(taskWrapper, taskType);
    } else {
      immediateTasksCount++;
      taskWrapper.runTask();
    }
  }

  private Runnable getRunnable(Runnable runnable, @TaskType int taskType) {
    if (taskType == TaskType.HEAD_RESET) {
      return () -> executeHeadReset(runnable);
    }
    if (taskType == TaskType.HEAD_INVALIDATE) {
      synchronized (lock) {
        waitingForHeadReset = true;
      }
      return () -> executeHeadInvalidate(runnable);
    }
    return () -> executeTask(runnable);
  }

  private void queueTask(TaskWrapper taskWrapper, @TaskType int taskType) {
    synchronized (lock) {
      if (taskType == TaskType.HEAD_INVALIDATE
          || taskType == TaskType.HEAD_RESET
          || taskType == TaskType.POST_INIT) {
        immediateTasks.add(taskWrapper);
        maxImmediateTasks = Math.max(immediateTasks.size(), maxImmediateTasks);
        if (taskType == TaskType.HEAD_RESET) {
          executeNextTask();
        }
      } else if (taskType == TaskType.USER_FACING) {
        userTasks.add(taskWrapper);
        maxUserFacingTasks = Math.max(userTasks.size(), maxUserFacingTasks);
      } else {
        backgroundTasks.add(taskWrapper);
        maxBackgroundTasks = Math.max(backgroundTasks.size(), maxBackgroundTasks);
      }
    }
  }

  private void countTask(@TaskType int taskType) {
    taskCount++;
    if (taskType == TaskType.POST_INIT) {
      postInitTaskCount++;
    } else if (taskType == TaskType.HEAD_INVALIDATE) {
      headInvalidateTaskCount++;
    } else if (taskType == TaskType.HEAD_RESET) {
      headResetTaskCount++;
    } else if (taskType == TaskType.USER_FACING) {
      userFacingTaskCount++;
    } else if (taskType == TaskType.BACKGROUND) {
      backgroundTaskCount++;
    }
  }

  /** Indicates that tasks are being delayed until a response is processed */
  @VisibleForTesting
  public boolean isDelayed() {
    synchronized (lock) {
      return !initialized || waitingForHeadReset;
    }
  }

  @VisibleForTesting
  boolean hasBacklog() {
    synchronized (lock) {
      return !backgroundTasks.isEmpty() || !userTasks.isEmpty() || !immediateTasks.isEmpty();
    }
  }

  private void executeInitialization(Runnable runnable) {
    runnable.run();
    synchronized (lock) {
      initialized = true;
    }
    executeNextTask();
  }

  private void executeHeadReset(Runnable runnable) {
    runnable.run();
    synchronized (lock) {
      waitingForHeadReset = false;
    }
    executeNextTask();
  }

  private void executeHeadInvalidate(Runnable runnable) {
    runnable.run();
    synchronized (lock) {
      waitingForHeadReset = true;
    }
    executeNextTask();
  }

  private void executeTask(Runnable runnable) {
    runnable.run();
    executeNextTask();
  }

  private void executeNextTask() {
    synchronized (lock) {
      if (!immediateTasks.isEmpty()) {
        TaskWrapper task = immediateTasks.remove();
        task.runTask();
      } else if (!userTasks.isEmpty() && !isDelayed()) {
        TaskWrapper task = userTasks.remove();
        task.runTask();
      } else if (!backgroundTasks.isEmpty() && !isDelayed()) {
        TaskWrapper task = backgroundTasks.remove();
        task.runTask();
      }
    }
  }

  interface TaskWrapper {
    void runTask();
  }

  class BaseTaskWrapper implements TaskWrapper {
    private final String task;
    private final @TaskType int taskType;
    private final Runnable taskRunnable;

    BaseTaskWrapper(String task, @TaskType int taskType, Runnable taskRunnable) {
      this.task = task;
      this.taskType = taskType;
      this.taskRunnable = taskRunnable;
    }

    @Override
    public void runTask() {
      Logger.i(TAG, "Execute task [%s - %s]: %s", taskType, isDelayed(), task);
      executor.execute(taskRunnable);
    }
  }

  /** Runs a Task which has a timeout */
  class TimeoutTaskWrapper implements TaskWrapper {
    private final String task;
    private final @TaskType int taskType;
    private final Runnable taskRunnable;
    private final Runnable timeoutRunnable;
    private final AtomicBoolean started = new AtomicBoolean(false);

    TimeoutTaskWrapper(
        String task, @TaskType int taskType, Runnable taskRunnable, Runnable timeoutRunnable) {
      this.task = task;
      this.taskType = taskType;
      this.taskRunnable = taskRunnable;
      this.timeoutRunnable = timeoutRunnable;
    }

    TimeoutTaskWrapper startTimeout(long timeoutMillis) {
      Logger.i("FeedSessionManager", "startingTimeout %s", timeoutMillis);
      mainThreadHandler.postDelayed(this::runTimeoutCallback, timeoutMillis);
      return this;
    }

    @Override
    public void runTask() {
      Logger.i(TAG, "Execute task [%s - %s]: %s", taskType, isDelayed(), task);
      started.set(true);
      executor.execute(taskRunnable);
    }

    void runTimeoutCallback() {
      if (started.get()) {
        return;
      }
      Logger.i(TAG, "Execute Timeout [%s]: %s", taskType, task);
      executor.execute(timeoutRunnable);
    }
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("tasks").value(taskCount);
    dumper.forKey("immediateRun").value(immediateTasksCount).compactPrevious();
    dumper.forKey("delayedRun").value(delayedTaskCount).compactPrevious();

    dumper.forKey("postInitTasks").value(postInitTaskCount);
    dumper.forKey("headInvalidateTasks").value(headInvalidateTaskCount).compactPrevious();
    dumper.forKey("headResetTasks").value(headResetTaskCount).compactPrevious();
    dumper.forKey("userFacingTasks").value(userFacingTaskCount).compactPrevious();
    dumper.forKey("backgroundTasks").value(backgroundTaskCount).compactPrevious();

    dumper.forKey("maxImmediateQueue").value(maxImmediateTasks);
    dumper.forKey("maxUserFacingQueue").value(maxUserFacingTasks).compactPrevious();
    dumper.forKey("maxBackgroundQueue").value(maxBackgroundTasks).compactPrevious();
  }
}
