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

package com.google.android.libraries.feed.basicstream.internal.scroll;

import static com.google.android.libraries.feed.basicstream.internal.scroll.ScrollRestorer.nonRestoringRestorer;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.libraries.feed.basicstream.internal.StreamSavedInstanceStateProto.ScrollState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ScrollRestorer}. */
@RunWith(RobolectricTestRunner.class)
public class ScrollRestorerTest {

  private static final int POSITION = 4;
  private static final int OFFSET = -10;
  private static final ScrollState SCROLL_STATE =
      ScrollState.newBuilder().setPosition(POSITION).setOffset(OFFSET).build();

  @Mock private StreamScrollMonitor streamScrollMonitor;

  private RecyclerView recyclerView;
  private LinearLayoutManagerForTest layoutManager;

  @Before
  public void setUp() {
    initMocks(this);

    Context context = Robolectric.setupActivity(Activity.class);
    recyclerView = new RecyclerView(context);
    layoutManager = new LinearLayoutManagerForTest(context);
    recyclerView.setLayoutManager(layoutManager);
  }

  @Test
  public void testRestoreScroll() {
    ScrollRestorer scrollRestorer =
        new ScrollRestorer(recyclerView, streamScrollMonitor, SCROLL_STATE);
    scrollRestorer.maybeRestoreScroll();

    verify(streamScrollMonitor).onProgrammaticScroll();
    assertThat(layoutManager.scrolledPosition).isEqualTo(POSITION);
    assertThat(layoutManager.scrolledOffset).isEqualTo(OFFSET);
  }

  @Test
  public void testRestoreScroll_repeatedCall() {
    ScrollRestorer scrollRestorer =
        new ScrollRestorer(recyclerView, streamScrollMonitor, createScrollState(10, 20));
    scrollRestorer.maybeRestoreScroll();

    layoutManager.scrollToPositionWithOffset(POSITION, OFFSET);
    scrollRestorer.maybeRestoreScroll();

    verify(streamScrollMonitor).onProgrammaticScroll();
    assertThat(layoutManager.scrolledPosition).isEqualTo(POSITION);
    assertThat(layoutManager.scrolledOffset).isEqualTo(OFFSET);
  }

  @Test
  public void testAbandonRestoringScroll() {
    layoutManager.scrollToPositionWithOffset(POSITION, OFFSET);

    ScrollRestorer scrollRestorer =
        new ScrollRestorer(recyclerView, streamScrollMonitor, createScrollState(10, 20));
    scrollRestorer.abandonRestoringScroll();
    scrollRestorer.maybeRestoreScroll();

    assertThat(layoutManager.scrolledPosition).isEqualTo(POSITION);
    assertThat(layoutManager.scrolledOffset).isEqualTo(OFFSET);
  }

  @Test
  public void testGetScrollStateForScrollPosition() {
    layoutManager.firstVisibleItemPosition = POSITION;
    layoutManager.firstVisibleViewOffset = OFFSET;
    ScrollState restorerScrollState =
        nonRestoringRestorer(recyclerView, streamScrollMonitor).getScrollStateForScrollRestore();
    assertThat(restorerScrollState).isEqualTo(SCROLL_STATE);
  }

  @Test
  public void testGetBundleForScrollPosition_invalidPosition() {
    assertThat(
            nonRestoringRestorer(recyclerView, streamScrollMonitor)
                .getScrollStateForScrollRestore())
        .isNull();
  }

  private ScrollState createScrollState(int position, int offset) {
    return ScrollState.newBuilder().setPosition(position).setOffset(offset).build();
  }

  private class LinearLayoutManagerForTest extends LinearLayoutManager {

    private final Context context;

    private int firstVisibleItemPosition = RecyclerView.NO_POSITION;
    private int firstVisibleViewOffset;

    private int scrolledPosition;
    private int scrolledOffset;

    public LinearLayoutManagerForTest(Context context) {
      super(context);
      this.context = context;
    }

    @Override
    public int findFirstVisibleItemPosition() {
      return firstVisibleItemPosition;
    }

    @Override
    public View findViewByPosition(int position) {
      if (position == firstVisibleItemPosition) {
        View view = new View(context);
        view.setTop(firstVisibleViewOffset);
        return view;
      }

      return super.findViewByPosition(position);
    }

    @Override
    public void scrollToPositionWithOffset(int scrollPosition, int scrollOffset) {
      scrolledPosition = scrollPosition;
      scrolledOffset = scrollOffset;
    }
  }
}
