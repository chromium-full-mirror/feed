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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link SchedulerApiWrapper}. */
@RunWith(RobolectricTestRunner.class)
public class SchedulerApiWrapperTest {

  @Mock private ThreadUtils threadUtils;
  @Mock private SchedulerApi schedulerApi;
  @Mock private SessionManagerState sessionManagerState;

  @Spy private MainThreadRunner mainThreadRunner = new FakeMainThreadRunner();

  @Before
  public void setup() {
    initMocks(this);
  }

  @After
  public void validate() {
    validateMockitoUsage();
  }

  @Test
  public void testShouldSessionRequestData_mainThread() {
    when(threadUtils.isMainThread()).thenReturn(true);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.shouldSessionRequestData(sessionManagerState);
    verify(schedulerApi).shouldSessionRequestData(sessionManagerState);
    verify(mainThreadRunner, never()).execute(anyString(), any());
  }

  @Test
  public void testShouldSessionRequestData_backgroundThread() {
    when(threadUtils.isMainThread()).thenReturn(false);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.shouldSessionRequestData(sessionManagerState);
    verify(schedulerApi).shouldSessionRequestData(sessionManagerState);
    verify(mainThreadRunner).execute(anyString(), any(Runnable.class));
  }

  @Test
  public void testOnReceiveNewContent_mainThread() {
    when(threadUtils.isMainThread()).thenReturn(true);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.onReceiveNewContent(0);
    verify(schedulerApi).onReceiveNewContent(0);
    verify(mainThreadRunner, never()).execute(anyString(), any());
  }

  @Test
  public void testOnReceiveNewContent_backgroundThread() {
    when(threadUtils.isMainThread()).thenReturn(false);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.onReceiveNewContent(0);
    verify(schedulerApi).onReceiveNewContent(0);
    verify(mainThreadRunner).execute(anyString(), any(Runnable.class));
  }

  @Test
  public void testOnRequestErrort_mainThread() {
    when(threadUtils.isMainThread()).thenReturn(true);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.onRequestError(0);
    verify(schedulerApi).onRequestError(0);
    verify(mainThreadRunner, never()).execute(anyString(), any());
  }

  @Test
  public void testOnRequestError_backgroundThread() {
    when(threadUtils.isMainThread()).thenReturn(false);
    SchedulerApiWrapper wrapper =
        new SchedulerApiWrapper(schedulerApi, threadUtils, mainThreadRunner);
    wrapper.onRequestError(0);
    verify(schedulerApi).onRequestError(0);
    verify(mainThreadRunner).execute(anyString(), any(Runnable.class));
  }

  private static class FakeMainThreadRunner extends MainThreadRunner {
    @Override
    public void execute(String name, Runnable runnable) {
      runnable.run();
    }
  }
}
