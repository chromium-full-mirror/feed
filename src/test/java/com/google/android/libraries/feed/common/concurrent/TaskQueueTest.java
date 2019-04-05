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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.concurrent.TaskQueue.TaskType;
import com.google.android.libraries.feed.common.concurrent.testing.ClockBackedFakeMainThreadRunner;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.testing.host.logging.FakeBasicLoggingApi;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link TaskQueue} class. */
@RunWith(RobolectricTestRunner.class)
public class TaskQueueTest {
  private final FakeBasicLoggingApi fakeBasicLoggingApi = new FakeBasicLoggingApi();
  private final FakeClock fakeClock = new FakeClock();
  private final List<Integer> callOrder = new ArrayList<>();
  private final ClockBackedFakeMainThreadRunner fakeMainThreadRunner =
      ClockBackedFakeMainThreadRunner.create(fakeClock);
  private final TaskQueue taskQueue =
      new TaskQueue(
          fakeBasicLoggingApi,
          MoreExecutors.directExecutor(),
          fakeMainThreadRunner,
          fakeClock,
          /* checkStarvation= */ true);

  private boolean delayedTaskHasRun = false;

  @Before
  public void setUp() {
    initMocks(this);
    callOrder.clear();
  }

  @Test
  public void testInitialization() {
    assertThat(taskQueue.isDelayed()).isTrue();
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testInitializationCallOrder() {
    Runnable background = () -> runnable(1);
    Runnable userFacing = () -> runnable(2);
    Runnable postInit = () -> runnable(3);
    Runnable init = () -> runnable(4);

    taskQueue.execute("background", TaskType.BACKGROUND, background);
    taskQueue.execute("userFacing", TaskType.USER_FACING, userFacing);
    taskQueue.execute("postInit", TaskType.IMMEDIATE, postInit);
    assertThat(taskQueue.hasBacklog()).isTrue();
    taskQueue.initialize(init);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(4, 3, 2, 1);
  }

  @Test
  public void testPostInit() {
    taskQueue.initialize(this::noOp);

    Runnable invalidate = () -> runnable(1);
    Runnable postInit = () -> runnable(2);
    Runnable userFacing = () -> runnable(3);
    Runnable reset = () -> runnable(4);

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, invalidate);
    taskQueue.execute("postInit", TaskType.IMMEDIATE, postInit);
    taskQueue.execute("userFacing", TaskType.USER_FACING, userFacing);
    taskQueue.execute("reset", TaskType.HEAD_RESET, reset);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(1, 2, 4, 3);
  }

  @Test
  public void testHeadInvalidateReset() {
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, this::noOp);
    taskQueue.execute("delayedTask", TaskType.BACKGROUND, this::delayedTask);
    assertThat(taskQueue.isDelayed()).isTrue();
    assertThat(delayedTaskHasRun).isFalse();
    taskQueue.execute("headReset", TaskType.HEAD_RESET, this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
    assertThat(delayedTaskHasRun).isTrue();
  }

  @Test
  public void testHeadInvalidateDropMultiple() {
    Runnable invalidate = () -> runnable(1);
    Runnable invalidateNotRun = () -> runnable(2);
    Runnable init = () -> runnable(3);

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, invalidate);
    taskQueue.execute("headInvalidateDropped", TaskType.HEAD_INVALIDATE, invalidateNotRun);
    assertThat(taskQueue.hasBacklog()).isTrue();
    taskQueue.initialize(init);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(3, 1);
  }

  @Test
  public void testHeadInvalidateCallOrder() {
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();

    Runnable headInvalidate = () -> runnable(1);
    Runnable background = () -> runnable(2);
    Runnable userFacing = () -> runnable(3);
    Runnable postInit = () -> runnable(4);
    Runnable headReset = () -> runnable(5);

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, headInvalidate);
    taskQueue.execute("background", TaskType.BACKGROUND, background);
    taskQueue.execute("userFacing", TaskType.USER_FACING, userFacing);
    taskQueue.execute("postInit", TaskType.IMMEDIATE, postInit); // run immediately
    assertThat(taskQueue.hasBacklog()).isTrue();
    taskQueue.execute("headReset", TaskType.HEAD_RESET, headReset);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(1, 4, 5, 3, 2);
  }

  @Test
  public void testResetTaskQueue_immediate() {
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();

    taskQueue.reset();
    assertThat(taskQueue.isDelayed()).isTrue();
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testResetQueue_withDelay() {
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();

    Runnable headInvalidate = () -> runnable(1);
    Runnable background = () -> runnable(2);
    Runnable userFacing = () -> runnable(3);
    Runnable postInit = () -> runnable(4);

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, headInvalidate);
    taskQueue.execute("background", TaskType.BACKGROUND, background);
    taskQueue.execute("userFacing", TaskType.USER_FACING, userFacing);
    taskQueue.execute("postInit", TaskType.IMMEDIATE, postInit);
    assertThat(taskQueue.hasBacklog()).isTrue();

    taskQueue.reset();
    assertThat(taskQueue.isDelayed()).isTrue();
    assertThat(taskQueue.hasBacklog()).isFalse();
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testStarvation_initialization() {
    taskQueue.initialize(this::noOp);
    taskQueue.execute("delayedTask", TaskType.BACKGROUND, this::delayedTask);

    assertThat(delayedTaskHasRun).isTrue();
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testStarvation_headInvalidate() {
    taskQueue.initialize(this::noOp);
    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, this::noOp);
    taskQueue.execute("delayedTask", TaskType.BACKGROUND, this::delayedTask);

    assertThat(fakeMainThreadRunner.getCompletedTaskCount()).isEqualTo(0);
    assertThat(delayedTaskHasRun).isFalse();
    assertThat(taskQueue.isDelayed()).isTrue();

    runAndAssertStarvationChecks();
    assertThat(delayedTaskHasRun).isTrue();
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testStarvation_reset() {
    taskQueue.initialize(this::noOp);
    taskQueue.reset();
    taskQueue.execute("delayedTask", TaskType.BACKGROUND, this::delayedTask);

    assertThat(delayedTaskHasRun).isFalse();
    assertThat(taskQueue.isDelayed()).isTrue();

    runAndAssertStarvationChecks();
    assertThat(delayedTaskHasRun).isTrue();
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  private void runAndAssertStarvationChecks() {
    int starvationTaskCount = 0;
    long startTimeMillis = fakeClock.currentTimeMillis();
    while (fakeClock.currentTimeMillis() - startTimeMillis < TaskQueue.STARVATION_TIMEOUT_MS) {
      fakeClock.advance(TaskQueue.STARVATION_CHECK_MS);
      starvationTaskCount++;
      assertThat(fakeMainThreadRunner.getCompletedTaskCount()).isEqualTo(starvationTaskCount);
    }
  }

  private void assertOrder(Integer... args) {
    assertThat(callOrder.size()).isEqualTo(args.length);
    for (int i = 0; i < args.length; i++) {
      assertThat(callOrder.get(i)).isEqualTo(args[i]);
    }
  }

  private void runnable(int taskId) {
    callOrder.add(taskId);
  }

  private void noOp() {}

  private void delayedTask() {
    delayedTaskHasRun = true;
  }
}
