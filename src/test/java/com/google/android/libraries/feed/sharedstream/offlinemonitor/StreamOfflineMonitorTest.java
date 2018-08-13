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

package com.google.android.libraries.feed.sharedstream.offlinemonitor;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.host.offlineindicator.OfflineIndicatorApi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamOfflineMonitor}. */
@RunWith(RobolectricTestRunner.class)
public class StreamOfflineMonitorTest {

  private static final String URL = "google.com";

  @Mock private Consumer<Boolean> offlineStatusConsumer1;
  @Mock private Consumer<Boolean> offlineStatusConsumer2;

  private StreamOfflineMonitor streamOfflineMonitor;

  @Before
  public void setup() {
    initMocks(this);
    streamOfflineMonitor = new StreamOfflineMonitor(mock(OfflineIndicatorApi.class));
  }

  @Test
  public void testIsAvailableOffline() {
    assertThat(streamOfflineMonitor.isAvailableOffline(URL)).isFalse();
    streamOfflineMonitor.updateOfflineStatus(URL, true);
    assertThat(streamOfflineMonitor.isAvailableOffline(URL)).isTrue();
    streamOfflineMonitor.updateOfflineStatus(URL, false);
    assertThat(streamOfflineMonitor.isAvailableOffline(URL)).isFalse();
  }

  @Test
  public void testNotifyListeners() {
    streamOfflineMonitor.addOfflineStatusConsumer(URL, offlineStatusConsumer1);

    streamOfflineMonitor.updateOfflineStatus(URL, true);

    verify(offlineStatusConsumer1).accept(true);

    streamOfflineMonitor.updateOfflineStatus(URL, false);

    verify(offlineStatusConsumer1).accept(false);
  }

  @Test
  public void testNotify_multipleListeners() {
    streamOfflineMonitor.addOfflineStatusConsumer(URL, offlineStatusConsumer1);
    streamOfflineMonitor.addOfflineStatusConsumer(URL, offlineStatusConsumer2);
    streamOfflineMonitor.updateOfflineStatus(URL, true);

    verify(offlineStatusConsumer1).accept(true);
    verify(offlineStatusConsumer2).accept(true);
  }

  @Test
  public void testRemoveConsumer() {
    streamOfflineMonitor.addOfflineStatusConsumer(URL, offlineStatusConsumer1);

    streamOfflineMonitor.removeOfflineStatusConsumer(URL, offlineStatusConsumer1);

    streamOfflineMonitor.updateOfflineStatus(URL, true);

    verify(offlineStatusConsumer1, never()).accept(anyBoolean());
  }

  @Test
  public void testNotifyOnlyOnce() {
    streamOfflineMonitor.addOfflineStatusConsumer(URL, offlineStatusConsumer1);

    streamOfflineMonitor.updateOfflineStatus(URL, true);
    streamOfflineMonitor.updateOfflineStatus(URL, true);
    streamOfflineMonitor.updateOfflineStatus(URL, false);
    streamOfflineMonitor.updateOfflineStatus(URL, false);

    verify(offlineStatusConsumer1, times(1)).accept(true);
    verify(offlineStatusConsumer1, times(1)).accept(false);
  }
}
