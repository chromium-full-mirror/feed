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

package com.google.android.libraries.feed.piet.ui;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.FrameLayout.LayoutParams;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.functional.Suppliers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for the {@link RoundedCornerWrapperView}. */
@RunWith(RobolectricTestRunner.class)
public class RoundedCornerWrapperViewTest {

  private static final Supplier<Boolean> LEFT_TO_RIGHT = Suppliers.of(false);

  private Context context;
  private Canvas canvas;
  private Bitmap bitmap;

  @Before
  public void setUp() {
    context = Robolectric.buildActivity(Activity.class).get();
    bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
  }

  @Test
  public void testZeroDimensions_notRounded() {
    RoundedCornerWrapperView view = new RoundedCornerWrapperView(context, 0, 0, LEFT_TO_RIGHT);

    view.setLayoutParams(new LayoutParams(0, 100));
    assertThat(view.getWidth()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.setLayoutParams(new LayoutParams(100, 0));
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.setLayoutParams(new LayoutParams(0, 0));
    assertThat(view.getWidth()).isEqualTo(0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails
  }

  @Test
  public void testZeroDimensions_rounded() {
    RoundedCornerWrapperView view = new RoundedCornerWrapperView(context, 7, 10, LEFT_TO_RIGHT);

    view.setLayoutParams(new LayoutParams(0, 100));
    assertThat(view.getWidth()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.setLayoutParams(new LayoutParams(100, 0));
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.setLayoutParams(new LayoutParams(0, 0));
    assertThat(view.getWidth()).isEqualTo(0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails
  }
}
