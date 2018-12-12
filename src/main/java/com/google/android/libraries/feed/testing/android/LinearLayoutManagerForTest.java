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

package com.google.android.libraries.feed.testing.android;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/** A {@link LinearLayoutManager} used for testing. */
public final class LinearLayoutManagerForTest extends LinearLayoutManager {

  private final Context context;

  public int firstVisibleItemPosition = RecyclerView.NO_POSITION;
  public int firstVisibleViewOffset;

  public int lastVisibleItemPosition = RecyclerView.NO_POSITION;

  public int scrolledPosition;
  public int scrolledOffset;

  public LinearLayoutManagerForTest(Context context) {
    super(context);
    this.context = context;
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return firstVisibleItemPosition;
  }

  @Override
  public int findLastVisibleItemPosition() {
    return lastVisibleItemPosition;
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
