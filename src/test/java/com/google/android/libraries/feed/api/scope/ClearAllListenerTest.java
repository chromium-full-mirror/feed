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

package com.google.android.libraries.feed.api.scope;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.lifecycle.Resettable;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link ClearAllListener} class. */
@RunWith(RobolectricTestRunner.class)
public class ClearAllListenerTest {
  @Mock SessionManager sessionManager;
  @Mock Resettable store;
  @Mock ThreadUtils threadUtils;

  @Mock private Clock clock;
  @Mock private TaskQueue taskQueue;
  private FeedAppLifecycleListener appLifecycleListener;

  @Before
  public void setUp() {
    initMocks(this);
    taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    appLifecycleListener = new FeedAppLifecycleListener(threadUtils);
    taskQueue = spy(taskQueue);
  }

  @Test
  public void testClearAll() {
    setupClearAllListener();
    appLifecycleListener.onClearAll();
    verify(sessionManager).reset();
    verify(store).reset();
    verify(taskQueue).reset();
    verify(taskQueue).completeReset();
  }

  @Test
  public void testClearAllWithRefresh() {
    setupClearAllListener();
    appLifecycleListener.onClearAllWithRefresh();
    verify(sessionManager).reset();
    verify(sessionManager).triggerRefresh(null, RequestReason.CLEAR_ALL);
    verify(store).reset();
    verify(taskQueue).reset();
    verify(taskQueue).completeReset();
  }

  @Test
  public void testNonClearLifecycld() {
    setupClearAllListener();
    appLifecycleListener.onEnterForeground();
    verifyZeroInteractions(sessionManager);
    verifyZeroInteractions(store);
  }

  private void setupClearAllListener() {
    new ClearAllListener(taskQueue, sessionManager, store, threadUtils, appLifecycleListener);
  }
}
