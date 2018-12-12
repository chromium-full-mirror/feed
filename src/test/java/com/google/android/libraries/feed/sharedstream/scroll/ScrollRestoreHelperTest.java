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

package com.google.android.libraries.feed.sharedstream.scroll;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.sharedstream.proto.ScrollStateProto.ScrollState;
import com.google.android.libraries.feed.testing.android.LinearLayoutManagerForTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ScrollRestoreHelperTest {

  private static final int HEADER_COUNT = 10;
  private static final int ABANDON_RESTORE_BELOW_FOLD_THRESHOLD = 5;
  private static final int FIRST_VISIBLE_ITEM_POSITION = 3;
  private static final int FIRST_VISIBLE_ITEM_OFFSET = 14;

  private LinearLayoutManagerForTest linearLayoutManager;

  @Before
  public void setUp() {
    linearLayoutManager =
        new LinearLayoutManagerForTest(Robolectric.buildActivity(Activity.class).get());
    linearLayoutManager.firstVisibleItemPosition = FIRST_VISIBLE_ITEM_POSITION;
    linearLayoutManager.firstVisibleViewOffset = FIRST_VISIBLE_ITEM_OFFSET;
  }

  @Test
  public void testGetScrollStateForScrollRestore_noFirstPosition_returnsNull() {
    linearLayoutManager.firstVisibleItemPosition = RecyclerView.NO_POSITION;

    assertThat(
            ScrollRestoreHelper.getScrollStateForScrollRestore(
                linearLayoutManager, new Configuration.Builder().build(), HEADER_COUNT))
        .isNull();
  }

  @Test
  public void testGetScrollStateForScrollRestore_noLastPosition_returnsNull() {
    // configurationMap.put(ConfigKey.ABANDON_RESTORE_BELOW_FOLD, true);

    linearLayoutManager.lastVisibleItemPosition = RecyclerView.NO_POSITION;

    assertThat(
            ScrollRestoreHelper.getScrollStateForScrollRestore(
                linearLayoutManager, new Configuration.Builder().build(), HEADER_COUNT))
        .isNull();
  }

  @Test
  public void testGetScrollStateForScrollRestore_scrolledTooFar_returnsNull() {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.ABANDON_RESTORE_BELOW_FOLD, true)
            .put(
                ConfigKey.ABANDON_RESTORE_BELOW_FOLD_THRESHOLD,
                ABANDON_RESTORE_BELOW_FOLD_THRESHOLD)
            .build();

    setUpForRestoringBelowTheFold();

    // With ABANDON_RESTORE_BELOW_FOLD true, we should not restore below the fold.
    assertThat(
            ScrollRestoreHelper.getScrollStateForScrollRestore(
                linearLayoutManager, configuration, HEADER_COUNT))
        .isNull();
  }

  @Test
  public void testGetScrollStateForScrollRestore_dontAbandonScrollRestore_returnsScrollState() {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.ABANDON_RESTORE_BELOW_FOLD, false)
            .put(
                ConfigKey.ABANDON_RESTORE_BELOW_FOLD_THRESHOLD,
                ABANDON_RESTORE_BELOW_FOLD_THRESHOLD)
            .build();

    setUpForRestoringBelowTheFold();

    // With ABANDON_RESTORE_BELOW_FOLD false, we should restore below the fold.
    assertThat(
            ScrollRestoreHelper.getScrollStateForScrollRestore(
                linearLayoutManager, configuration, HEADER_COUNT))
        .isEqualTo(
            ScrollState.newBuilder()
                .setPosition(FIRST_VISIBLE_ITEM_POSITION)
                .setOffset(FIRST_VISIBLE_ITEM_OFFSET)
                .build());
  }

  private void setUpForRestoringBelowTheFold() {
    linearLayoutManager.lastVisibleItemPosition =
        HEADER_COUNT + ABANDON_RESTORE_BELOW_FOLD_THRESHOLD + 1;
  }
}
