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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamItemTouchCallbacks}. */
@RunWith(RobolectricTestRunner.class)
public final class StreamItemTouchCallbacksTest {
  private final int NO_MOVEMENT_FLAG = 0;

  @Mock private PietViewHolder pietViewHolder;

  private StreamItemTouchCallbacks callbacks;
  private RecyclerView recyclerView;

  @Before
  public void setUp() {
    initMocks(this);
    callbacks = new StreamItemTouchCallbacks();
    recyclerView = new RecyclerView(Robolectric.setupActivity(Activity.class));
  }

  @Test
  public void testGetMovementFlags_nonPietType() {
    ViewHolder nonPietViewHolder = mock(ViewHolder.class);

    int movementFlags = callbacks.getMovementFlags(recyclerView, nonPietViewHolder);

    assertThat(movementFlags).isEqualTo(NO_MOVEMENT_FLAG);
  }

  @Test
  public void testGetMovementFlags_nonSwipeablePiet() {
    when(pietViewHolder.canSwipe()).thenReturn(false);

    int movementFlags = callbacks.getMovementFlags(recyclerView, pietViewHolder);

    assertThat(movementFlags).isEqualTo(NO_MOVEMENT_FLAG);
  }

  @Test
  public void testGetMovementFlags_swipeablePiet() {
    when(pietViewHolder.canSwipe()).thenReturn(true);

    int movementFlags = callbacks.getMovementFlags(recyclerView, pietViewHolder);

    assertThat(movementFlags)
        .isEqualTo(
            ItemTouchHelper.Callback.makeMovementFlags(
                0, ItemTouchHelper.START | ItemTouchHelper.END));
  }

  @Test
  public void testOnSwiped() {
    callbacks.onSwiped(pietViewHolder, 0);

    verify(pietViewHolder).swipeToDismissPerformed();
  }
}
