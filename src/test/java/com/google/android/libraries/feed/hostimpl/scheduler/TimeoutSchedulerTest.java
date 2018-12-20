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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.android.libraries.feed.testing.conformance.scheduler.SchedulerConformanceTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link TimeoutScheduler}. */
@RunWith(RobolectricTestRunner.class)
public class TimeoutSchedulerTest extends SchedulerConformanceTest {
  private static final boolean USE_TIMEOUT_SCHEDULER = false;
  private static final long TIMEOUT_STORIES_ARE_CURRENT = 2;
  private static final long TIMEOUT_STORIES_CURRENT_WITH_REFRESH = 4;

  @Mock private ThreadUtils threadUtils;
  private FeedAppLifecycleListener lifecycleListenerObservable;
  @Mock private Clock clock;

  private Configuration config;

  @Before
  public void setup() {
    initMocks(this);
    config =
        new Configuration.Builder()
            .put(ConfigKey.USE_TIMEOUT_SCHEDULER, USE_TIMEOUT_SCHEDULER)
            .put(ConfigKey.TIMEOUT_STORIES_ARE_CURRENT, TIMEOUT_STORIES_ARE_CURRENT)
            .put(
                ConfigKey.TIMEOUT_STORIES_CURRENT_WITH_REFRESH,
                TIMEOUT_STORIES_CURRENT_WITH_REFRESH)
            .build();
    lifecycleListenerObservable = new FeedAppLifecycleListener(threadUtils);
    scheduler = new TimeoutScheduler(clock, config, lifecycleListenerObservable);
  }

  @Test
  public void testOffDefault() {
    TimeoutScheduler scheduler = getScheduler(false);
    SessionManagerState state = new SessionManagerState(true, 1L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.NO_REQUEST_WITH_WAIT);
  }

  @Test
  public void outstandingRequestHasContent() {
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(true, 1L, true);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.NO_REQUEST_WITH_TIMEOUT);
  }

  @Test
  public void outstandingRequestNoContent() {
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(false, 1L, true);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.NO_REQUEST_WITH_WAIT);
  }

  @Test
  public void noContent() {
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(false, 0L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.REQUEST_WITH_WAIT);
  }

  @Test
  public void currentStories() {
    when(clock.currentTimeMillis()).thenReturn(2L);
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(true, 1L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.NO_REQUEST_WITH_CONTENT);
  }

  @Test
  public void currentStoriesWithRefresh() {
    when(clock.currentTimeMillis()).thenReturn(4L);
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(true, 1L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.REQUEST_WITH_CONTENT);
  }

  @Test
  public void nonCurrentStories() {
    when(clock.currentTimeMillis()).thenReturn(5L);
    TimeoutScheduler scheduler = getScheduler(true);
    SessionManagerState state = new SessionManagerState(true, 1L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.REQUEST_WITH_TIMEOUT);
  }

  @Test
  public void testLifecycleEvent() {
    TimeoutScheduler scheduler = getScheduler(true);
    assertThat(scheduler.clearAllTriggered.get()).isFalse();
    lifecycleListenerObservable.onClearAll();
    assertThat(scheduler.clearAllTriggered.get()).isTrue();
    SessionManagerState state = new SessionManagerState(true, 1L, false);
    assertThat(scheduler.shouldSessionRequestData(state))
        .isEqualTo(RequestBehavior.NO_REQUEST_WITH_TIMEOUT);
    assertThat(scheduler.clearAllTriggered.get()).isFalse();
  }

  private TimeoutScheduler getScheduler(boolean useScheduler) {
    config =
        new Configuration.Builder()
            .put(ConfigKey.USE_TIMEOUT_SCHEDULER, useScheduler)
            .put(ConfigKey.TIMEOUT_STORIES_ARE_CURRENT, TIMEOUT_STORIES_ARE_CURRENT)
            .put(
                ConfigKey.TIMEOUT_STORIES_CURRENT_WITH_REFRESH,
                TIMEOUT_STORIES_CURRENT_WITH_REFRESH)
            .build();
    return new TimeoutScheduler(clock, config, lifecycleListenerObservable);
  }
}
