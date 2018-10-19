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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link DrawableScalingHelper}. */
@RunWith(RobolectricTestRunner.class)
public class DrawableScalingHelperTest {

  private static final int DEFAULT_WIDTH_DP = 100;
  private static final int DEFAULT_HEIGHT_DP = 150;

  @Mock private AssetProvider assetProvider;

  private Context context;
  private Bitmap defaultBitmap;
  private BitmapDrawable defaultBitmapDrawable;

  @Before
  public void setUp() {
    initMocks(this);

    context = Robolectric.buildActivity(Activity.class).get();
    defaultBitmap = Bitmap.createBitmap(DEFAULT_WIDTH_DP, DEFAULT_HEIGHT_DP, Config.ALPHA_8);
    defaultBitmapDrawable = new BitmapDrawable(context.getResources(), defaultBitmap);

    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(true);
  }

  @Test
  public void testMaybeScaleDrawable() {
    int specifiedWidth = 10;
    int specifiedHeight = 15;
    Drawable returnedDrawable =
        DrawableScalingHelper.maybeScaleDrawable(
            context, assetProvider, defaultBitmapDrawable, specifiedWidth, specifiedHeight);

    assertThat(returnedDrawable).isInstanceOf(BitmapDrawable.class);
    Bitmap createdBitmap = ((BitmapDrawable) returnedDrawable).getBitmap();
    assertThat(createdBitmap.getWidth()).isEqualTo(specifiedWidth);
    assertThat(createdBitmap.getHeight()).isEqualTo(specifiedHeight);
  }

  @Test
  public void testMaybeScaleDrawable_noScaleIfDimensMatch() {
    Drawable returnedDrawable =
        DrawableScalingHelper.maybeScaleDrawable(
            context, assetProvider, defaultBitmapDrawable, DEFAULT_WIDTH_DP, DEFAULT_HEIGHT_DP);

    assertThat(returnedDrawable).isSameAs(defaultBitmapDrawable);
  }

  @Test
  public void testMaybeScaleDrawable_hostDisablesScaling() {
    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(false);
    Drawable returnedDrawable =
        DrawableScalingHelper.maybeScaleDrawable(
            context, assetProvider, defaultBitmapDrawable, /* widthPx= */ 10, /* heightPx= */ 15);

    assertThat(returnedDrawable).isSameAs(defaultBitmapDrawable);
  }

  @Test
  public void testMaybeScaleDrawable_unsupportedDrawableType() {
    ColorDrawable colorDrawable = new ColorDrawable(Color.RED);
    Drawable returnedDrawable =
        DrawableScalingHelper.maybeScaleDrawable(
            context, assetProvider, colorDrawable, /* widthPx= */ 10, /* heightPx= */ 15);

    assertThat(returnedDrawable).isSameAs(colorDrawable);
  }
}
