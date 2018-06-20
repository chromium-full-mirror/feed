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

package com.google.android.libraries.feed.basicstream.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.basicstream.internal.drivers.ContentDriver;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link DeepestContentTracker}. */
@RunWith(RobolectricTestRunner.class)
public class DeepestContentTrackerTest {

  private static final String CONTENT_ID_1 = "CONTENT_ID_1";
  private static final String CONTENT_ID_2 = "CONTENT_ID_2";

  @Mock private ContentDriver firstContentDriver;
  @Mock private ContentDriver secondContentDriver;

  @Mock private FeedViewHolder firstViewHolder;
  @Mock private FeedViewHolder secondViewHolder;

  private DeepestContentTracker deepestContentTracker;

  @Before
  public void setUp() {
    initMocks(this);

    when(firstContentDriver.getContentId()).thenReturn(CONTENT_ID_1);
    when(firstViewHolder.getAdapterPosition()).thenReturn(0);

    when(secondContentDriver.getContentId()).thenReturn(CONTENT_ID_2);
    when(secondViewHolder.getAdapterPosition()).thenReturn(1);

    deepestContentTracker = new DeepestContentTracker();
  }

  @Test
  public void testUpdateDeepestContentTracker() {
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testUpdateDeepestContentTracker_updatesFromDeeperContent() {
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(2, CONTENT_ID_2);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_2);
  }

  @Test
  public void testUpdateDeepestContentTracker_doesntUpdatesFromShallowerContent() {
    deepestContentTracker.updateDeepestContentTracker(2, CONTENT_ID_2);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_2);
  }

  @Test
  public void testUpdateDeepestContentTracker_ignoresNullContentId() {
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(10000, /* contentId= */ null);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testReset() {
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);

    deepestContentTracker.reset();

    assertThat(deepestContentTracker.getChildViewDepth()).isNull();
  }
}
