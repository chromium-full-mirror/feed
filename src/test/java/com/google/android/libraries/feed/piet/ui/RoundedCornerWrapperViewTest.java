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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.functional.Suppliers;
import com.google.android.libraries.feed.piet.ui.RoundedCornerMaskCache.Corner;
import com.google.android.libraries.feed.piet.ui.RoundedCornerMaskCache.RoundedCornerBitmaps;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.Borders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for the {@link RoundedCornerWrapperView}. */
@RunWith(RobolectricTestRunner.class)
public class RoundedCornerWrapperViewTest {

  private static final Supplier<Boolean> LEFT_TO_RIGHT = Suppliers.of(false);
  private static final int TOP_CORNERS_BITMASK = 3;
  private static final int LEFT_CORNERS_BITMASK = 9;

  private Context context;
  private Canvas canvas;
  private Bitmap bitmap;
  private RoundedCornerMaskCache maskCache;

  @Before
  public void setUp() {
    context = Robolectric.buildActivity(Activity.class).get();
    bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
    maskCache = new RoundedCornerMaskCache();
  }

  @Test
  public void testZeroDimensions_notRounded() {
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            RoundedCorners.getDefaultInstance(),
            maskCache,
            LEFT_TO_RIGHT,
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    view.layout(0, 0, 0, 100);
    assertThat(view.getWidth()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.layout(0, 0, 100, 0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.layout(0, 0, 0, 0);
    assertThat(view.getWidth()).isEqualTo(0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails
  }

  @Test
  public void testZeroDimensions_rounded() {
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setBitmask(7).setRadiusDp(10).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            LEFT_TO_RIGHT,
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    view.layout(0, 0, 0, 100);
    assertThat(view.getWidth()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.layout(0, 0, 100, 0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails

    view.layout(0, 0, 0, 0);
    assertThat(view.getWidth()).isEqualTo(0);
    assertThat(view.getHeight()).isEqualTo(0);
    view.draw(canvas);
    // Assert nothing fails
  }

  @Test
  public void testRoundedCorners_bordersAdded() {
    // The width, height, and radius don't really matter for this test. As long as they exist, a
    // BorderDrawable should be set on the view.
    Borders borders = Borders.newBuilder().setWidth(10).build();

    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(10).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context, roundedCorners, maskCache, Suppliers.of(false), 0, borders);

    view.layout(0, 0, 100, 100);
    view.draw(canvas);

    // Assert that borders are created.
    assertThat(view.getForeground()).isInstanceOf(BorderDrawable.class);
    // The specifics of how the borders look are difficult to test here, and so are tested in a
    // screendiff test instead.
  }

  @Test
  public void testRoundedCorners_bordersWidthZero() {
    Borders borders = Borders.newBuilder().setWidth(0).build();

    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(10).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context, roundedCorners, maskCache, Suppliers.of(false), 0, borders);

    // Set a width and height on the view so that the only thing stopping borders from being created
    // is the border width of 0.
    view.layout(0, 0, 100, 100);
    view.draw(canvas);

    // Assert that borders are not created.
    assertThat(view.getForeground()).isNull();
  }

  @Test
  public void testGetRadius_usesOverride() {
    int viewWidth = 100;
    int viewHeight = 100;
    int radiusToSet = 10;
    int radiusOverride = 20;
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(radiusToSet).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            radiusOverride,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Make sure it's using the override value, NOT the radius from RoundedCorners.
    assertThat(calculatedRadius).isEqualTo(radiusOverride);
  }

  @Test
  public void testGetRadius_radiusDp() {
    int viewWidth = 100;
    int viewHeight = 100;
    int radiusToSet = 10;
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(radiusToSet).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    assertThat(calculatedRadius).isEqualTo(radiusToSet);
  }

  @Test
  public void testGetRadius_radiusPercentageOfHeight() {
    int viewWidth = 60;
    int viewHeight = 100;
    int radiusPercentageOfHeight = 25;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setRadiusPercentageOfHeight(radiusPercentageOfHeight).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // 25% of the height is 25. .25 * 100 = 25
    assertThat(calculatedRadius).isEqualTo(25);
  }

  @Test
  public void testGetRadius_radiusPercentageOfWidth() {
    int viewWidth = 60;
    int viewHeight = 100;
    int radiusPercentageOfWidth = 25;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setRadiusPercentageOfWidth(radiusPercentageOfWidth).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // 25% of the width is 15. .25 * 60 = 15
    assertThat(calculatedRadius).isEqualTo(15);
  }

  @Test
  public void testGetRadius_smallWidth_RadiusDpShouldAdjust() {
    int viewWidth = 30;
    int viewHeight = 40;
    int radiusToSet = 20;
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(radiusToSet).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // A radius of 20 on each corner will add up to 40, which is bigger than the width. The radius
    // should be adjusted to be 15.
    assertThat(calculatedRadius).isEqualTo(15);
  }

  @Test
  public void testGetRadius_smallWidth_RadiusDpShouldNotAdjust() {
    int viewWidth = 30;
    int viewHeight = 40;
    int radiusToSet = 20;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(LEFT_CORNERS_BITMASK)
            .setRadiusDp(radiusToSet)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // The radius of 20 is more than half the width. However, since only the left corners are
    // rounded, the radius should not be adjusted, since 20dp is only half the height.
    assertThat(calculatedRadius).isEqualTo(20);
  }

  @Test
  public void testGetRadius_smallHeight_RadiusDpShouldAdjust() {
    int viewWidth = 40;
    int viewHeight = 30;
    int radiusToSet = 20;
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadiusDp(radiusToSet).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // A radius of 20 on each corner will add up to 40, which is bigger than the height. The radius
    // should be adjusted to be 15.
    assertThat(calculatedRadius).isEqualTo(15);
  }

  @Test
  public void testGetRadius_smallHeight_RadiusDpShouldNotAdjust() {
    int viewWidth = 40;
    int viewHeight = 30;
    int radiusToSet = 20;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(TOP_CORNERS_BITMASK)
            .setRadiusDp(radiusToSet)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // The radius of 20 is more than half the height. However, since only the top corners are
    // rounded, the radius should not be adjusted, since 20dp is only half the width.
    assertThat(calculatedRadius).isEqualTo(20);
  }

  @Test
  public void testGetRadius_radiusPercentageOfHeight_100Percent_shouldAdjust() {
    int viewWidth = 60;
    int viewHeight = 100;
    int radiusPercentageOfHeight = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setRadiusPercentageOfHeight(radiusPercentageOfHeight).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Since all corners are rounded, the radius should shrink to 30, half the width.
    assertThat(calculatedRadius).isEqualTo(30);
  }

  @Test
  public void testGetRadius_radiusPercentageOfHeight_100Percent_topCorners_shouldAdjust() {
    int viewWidth = 60;
    int viewHeight = 100;
    int radiusPercentageOfHeight = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(TOP_CORNERS_BITMASK)
            .setRadiusPercentageOfHeight(radiusPercentageOfHeight)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Only top corners are rounded, but the radius should still shrink to 30, half the width, since
    // the width is small.
    assertThat(calculatedRadius).isEqualTo(30);
  }

  @Test
  public void testGetRadius_radiusPercentageOfHeight_100Percent_topCorners_shouldNotAdjust() {
    int viewWidth = 100;
    int viewHeight = 40;
    int radiusPercentageOfHeight = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(TOP_CORNERS_BITMASK)
            .setRadiusPercentageOfHeight(radiusPercentageOfHeight)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Only top corners are rounded, and 40*2=80 is still less than the width (100), so we don't
    // need to shrink the radius.
    assertThat(calculatedRadius).isEqualTo(40);
  }

  @Test
  public void testGetRadius_radiusPercentageOfWidth_100Percent_shouldAdjust() {
    int viewWidth = 100;
    int viewHeight = 60;
    int radiusPercentageOfWidth = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setRadiusPercentageOfWidth(radiusPercentageOfWidth).build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Since all corners are rounded, the radius should shrink to 30, half the height, instead of
    // 100% of 100 = 100.
    assertThat(calculatedRadius).isEqualTo(30);
  }

  @Test
  public void testGetRadius_radiusPercentageOfWidth_100Percent_leftCorners_shouldAdjust() {
    int viewWidth = 100;
    int viewHeight = 60;
    int radiusPercentageOfWidth = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(LEFT_CORNERS_BITMASK)
            .setRadiusPercentageOfWidth(radiusPercentageOfWidth)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Only left corners are rounded, but the radius should still shrink to 30, half the height,
    // since the height is small. 100% of 100 = 100, which is greater than the height.
    assertThat(calculatedRadius).isEqualTo(30);
  }

  @Test
  public void testGetRadius_radiusPercentageOfWidth_100Percent_topCorners_shouldNotAdjust() {
    int viewWidth = 40;
    int viewHeight = 100;
    int radiusPercentageOfWidth = 100;
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setBitmask(LEFT_CORNERS_BITMASK)
            .setRadiusPercentageOfWidth(radiusPercentageOfWidth)
            .build();
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            maskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    int calculatedRadius = view.getRadius(viewWidth, viewHeight);

    // Only left corners are rounded, and 40*2=80 is still less than the height (100), so we don't
    // need to shrink the radius.
    assertThat(calculatedRadius).isEqualTo(40);
  }

  @Test
  public void testGetsMasksFromCache() {
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder().setBitmask(LEFT_CORNERS_BITMASK).setRadiusDp(16).build();
    RoundedCornerMaskCache mockMaskCache = mock(RoundedCornerMaskCache.class);
    RoundedCornerBitmaps mockBitmaps = mock(RoundedCornerBitmaps.class);
    Bitmap maskBitmap = maskCache.getMasks(16).get(Corner.TOP_LEFT);
    when(mockMaskCache.getPaint()).thenReturn(maskCache.getPaint());
    when(mockMaskCache.getMaskPaint()).thenReturn(maskCache.getMaskPaint());
    when(mockMaskCache.getMasks(16)).thenReturn(mockBitmaps);
    when(mockBitmaps.get(anyInt())).thenReturn(maskBitmap);
    RoundedCornerWrapperView view =
        new RoundedCornerWrapperView(
            context,
            roundedCorners,
            mockMaskCache,
            Suppliers.of(false),
            /*radiusOverride= */ 0,
            Borders.getDefaultInstance());

    view.layout(0, 0, 100, 100);

    verify(mockMaskCache).getMasks(16);

    verify(mockBitmaps).get(Corner.TOP_LEFT);
    verify(mockBitmaps).get(Corner.BOTTOM_LEFT);
    verifyNoMoreInteractions(mockBitmaps);
  }
}
