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

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link DeepestContentTracker}. */
@RunWith(RobolectricTestRunner.class)
public class DeepestContentTrackerTest {

  private static final String CONTENT_ID_1 = "CONTENT_ID_1";
  private static final String CONTENT_ID_2 = "CONTENT_ID_2";

  private DeepestContentTracker deepestContentTracker;

  @Before
  public void setUp() {
    deepestContentTracker = new DeepestContentTracker();
  }

  @Test
  public void testUpdateDeepestContentTracker() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testUpdateDeepestContentTracker_updatesFromDeeperContent() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_2);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_2);
  }

  @Test
  public void testUpdateDeepestContentTracker_doesNotAddContentIdIfAlreadyTracked() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_2);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_2);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testUpdateDeepestContentTracker_ignoresNullContentId() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(1, /* contentId= */ null);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testUpdateDeepestContentTracker_throwsExceptionWhenUpdatingPreviousKnownPosition() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_2);

    assertThatRunnable(() -> deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_2))
        .throwsAnExceptionOfType(RuntimeException.class);
  }

  @Test
  public void testRemoveContentId() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_2);

    deepestContentTracker.removeContentId(1);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testRemoveContentId_removingShallowerContentIdRetainsDeeperId() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);
    deepestContentTracker.updateDeepestContentTracker(1, CONTENT_ID_2);

    deepestContentTracker.removeContentId(0);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_2);
  }

  @Test
  public void testRemoveContentId_doesNotRemoveContentId() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);

    deepestContentTracker.removeContentId(1);

    assertThat(deepestContentTracker.getChildViewDepth()).isEqualTo(CONTENT_ID_1);
  }

  @Test
  public void testReset() {
    deepestContentTracker.updateDeepestContentTracker(0, CONTENT_ID_1);

    deepestContentTracker.reset();

    assertThat(deepestContentTracker.getChildViewDepth()).isNull();
  }
}
