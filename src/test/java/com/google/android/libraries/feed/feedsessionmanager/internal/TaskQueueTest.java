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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.feedsessionmanager.internal.TaskQueue.TaskType;
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
  private final List<Integer> callOrder = new ArrayList<>();

  @Before
  public void setUp() {
    initMocks(this);
    callOrder.clear();
  }

  @Test
  public void testInitialization() {
    TaskQueue taskQueue = getTaskQueue();
    assertThat(taskQueue.isDelayed()).isTrue();
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testInitializationCallOrder() {
    TaskQueue taskQueue = getTaskQueue();
    Runnable background = () -> runnable(1);
    Runnable userFacing = () -> runnable(2);
    Runnable postInit = () -> runnable(3);
    Runnable init = () -> runnable(4);

    taskQueue.execute("background", TaskType.BACKGROUND, background);
    taskQueue.execute("userFacing", TaskType.USER_FACING, userFacing);
    taskQueue.execute("postInit", TaskType.POST_INIT, postInit);
    assertThat(taskQueue.hasBacklog()).isTrue();
    taskQueue.initialize(init);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(4, 3, 2, 1);
  }

  @Test
  public void testHeadInvalidateReset() {
    TaskQueue taskQueue = getTaskQueue();
    taskQueue.initialize(this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();

    taskQueue.execute("headInvalidate", TaskType.HEAD_INVALIDATE, this::noOp);
    assertThat(taskQueue.isDelayed()).isTrue();
    taskQueue.execute("headReset", TaskType.HEAD_RESET, this::noOp);
    assertThat(taskQueue.isDelayed()).isFalse();
  }

  @Test
  public void testHeadInvalidateCallOrder() {
    TaskQueue taskQueue = getTaskQueue();
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
    taskQueue.execute("postInit", TaskType.POST_INIT, postInit); // run immediately
    assertThat(taskQueue.hasBacklog()).isTrue();
    taskQueue.execute("headReset", TaskType.HEAD_RESET, headReset);

    assertThat(taskQueue.hasBacklog()).isFalse();
    assertOrder(1, 4, 5, 3, 2);
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

  private TaskQueue getTaskQueue() {
    return new TaskQueue(MoreExecutors.newDirectExecutorService());
  }
}
