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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View.MeasureSpec;
import java.lang.ref.WeakReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link RoundedCornerImageView}. */
@RunWith(RobolectricTestRunner.class)
public class RoundedCornerImageViewTest {

  private static final int DRAWABLE_WIDTH = 80;
  private static final int DRAWABLE_HEIGHT = 40;
  private static final int DRAWABLE_ASPECT_RATIO = 2;

  private static final int CONTAINER_WIDTH = 90;
  private static final int CONTAINER_HEIGHT = 30;

  private RoundedCornerImageView view;
  private BitmapDrawable drawable;

  @Before
  public void setUp() {
    view = new RoundedCornerImageView(Robolectric.setupActivity(Activity.class));
    drawable =
        new BitmapDrawable(
            Bitmap.createBitmap(DRAWABLE_WIDTH, DRAWABLE_HEIGHT, Bitmap.Config.RGB_565));
  }

  @Test
  public void testSetImageDrawable_withColor() {
    ColorDrawable drawable = new ColorDrawable(Color.RED);

    view.setImageDrawable(drawable);

    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.roundedRectangle).isNotNull();

    // No shader should be created
    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
  }

  @Test
  public void testSetImageDrawable_withLayerDrawable() {
    LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {drawable});

    view.setImageDrawable(layerDrawable);

    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.roundedRectangle).isNotNull();

    assertThat(view.shader).isNotNull();
    assertThat(view.paint).isNotNull();
  }

  @Test
  public void testSetImageDrawable_withLayerDrawableAndIntrinsicHeightAndWidthUnknown() {
    LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {new ColorDrawable(Color.RED)});

    view.setImageDrawable(layerDrawable);

    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.roundedRectangle).isNotNull();

    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
  }

  @Test
  public void testSetImageDrawable_invalidDrawable() {
    Drawable drawable = new InsetDrawable(new ColorDrawable(Color.RED), 4);

    view.setImageDrawable(drawable);

    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.roundedRectangle).isNotNull();

    // No shader should be created
    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
  }

  @Test
  public void testSetImageDrawable_getBitmapReturnsNullDoesNotNPE() {
    BitmapDrawable drawable = new BitmapDrawable();

    view.setImageDrawable(drawable);
    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.roundedRectangle).isNotNull();

    // No shader should be created
    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
  }

  @Test
  public void testSetImageDrawable_nullHoldsNoResources() {
    BitmapDrawable drawable = new BitmapDrawable(Bitmap.createBitmap(4, 4, Bitmap.Config.RGB_565));
    WeakReference<Drawable> weakDrawable = new WeakReference<>(drawable);
    view.setImageDrawable(drawable);

    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.shader).isNotNull();
    assertThat(view.paint).isNotNull();
    view.setImageDrawable(null);
    drawable = null;

    System.gc();
    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
    assertThat(view.getDrawable()).isNull();
    assertThat(weakDrawable.get()).isNull();
  }

  @Test
  public void testSetCornerRadius() {
    BitmapDrawable drawable = new BitmapDrawable(Bitmap.createBitmap(4, 4, Bitmap.Config.RGB_565));

    view.setImageDrawable(drawable);
    view.setRoundedCorners(
        RoundedCornerViewHelper.TOP_CORNERS | RoundedCornerViewHelper.BOTTOM_CORNERS);
    view.setCornerRadius(5);
    assertThat(view.shader).isNotNull();
    assertThat(view.paint).isNotNull();

    view.setCornerRadius(0);
    assertThat(view.shader).isNull();
    assertThat(view.paint).isNull();
    assertThat(view.getDrawable()).isNotNull();
  }

  @Test
  public void testScaling_noDrawable_exactly() {
    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.EXACTLY);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.EXACTLY);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_WIDTH);
  }

  @Test
  public void testScaling_noDrawable_unspecified() {
    int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(0);
    assertThat(view.getMeasuredWidth()).isEqualTo(0);
  }

  @Test
  public void testScaling_bothExactly() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.EXACTLY);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.EXACTLY);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_WIDTH);
  }

  @Test
  public void testScaling_exactlyWidth_unspecifiedHeight() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.EXACTLY);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_WIDTH / DRAWABLE_ASPECT_RATIO);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_WIDTH);
  }

  @Test
  public void testScaling_exactlyHeight_unspecifiedWidth() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.EXACTLY);
    int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_HEIGHT * DRAWABLE_ASPECT_RATIO);
  }

  @Test
  public void testScaling_exactlyWidth_atMostHeight() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.AT_MOST);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.EXACTLY);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_WIDTH);
  }

  @Test
  public void testScaling_exactlyHeight_atMostWidth() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.EXACTLY);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.AT_MOST);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_HEIGHT * DRAWABLE_ASPECT_RATIO);
  }

  @Test
  public void testScaling_atMostHeightAndWidth_widerContainer() {
    drawable = new BitmapDrawable(Bitmap.createBitmap(50, 100, Bitmap.Config.RGB_565));
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(200, MeasureSpec.AT_MOST);
    int widthSpec = MeasureSpec.makeMeasureSpec(400, MeasureSpec.AT_MOST);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(200);
    assertThat(view.getMeasuredWidth()).isEqualTo(100);
  }

  @Test
  public void testScaling_atMostHeightAndWidth_tallerContainer() {
    drawable = new BitmapDrawable(Bitmap.createBitmap(100, 50, Bitmap.Config.RGB_565));
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(400, MeasureSpec.AT_MOST);
    int widthSpec = MeasureSpec.makeMeasureSpec(200, MeasureSpec.AT_MOST);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(100);
    assertThat(view.getMeasuredWidth()).isEqualTo(200);
  }

  @Test
  public void testScaling_atMostWidth_unspecifiedHeight() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    int widthSpec = MeasureSpec.makeMeasureSpec(CONTAINER_WIDTH, MeasureSpec.AT_MOST);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_WIDTH / DRAWABLE_ASPECT_RATIO);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_WIDTH);
  }

  @Test
  public void testScaling_atMostHeight_unspecifiedWidth() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(CONTAINER_HEIGHT, MeasureSpec.AT_MOST);
    int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(CONTAINER_HEIGHT);
    assertThat(view.getMeasuredWidth()).isEqualTo(CONTAINER_HEIGHT * DRAWABLE_ASPECT_RATIO);
  }

  @Test
  public void testScaling_bothUnspecified() {
    view.setImageDrawable(drawable);

    int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    view.measure(widthSpec, heightSpec);

    assertThat(view.getMeasuredHeight()).isEqualTo(0xFFFFFF);
    assertThat(view.getMeasuredWidth()).isEqualTo(0xFFFFFF);
  }
}
