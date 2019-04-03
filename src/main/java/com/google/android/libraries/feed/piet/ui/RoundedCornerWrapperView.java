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


import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.RadiusOptionsCase;
import com.google.search.now.ui.piet.StylesProto.Borders;

/** Wrapper for {@link View} instances in Piet that require rounded corners. */
public abstract class RoundedCornerWrapperView extends FrameLayout {
  final int radiusOverride;
  final RoundedCorners roundedCorners;
  final Supplier<Boolean> isRtLSupplier;
  final Context context;
  final Borders borders;

  int roundedCornerRadius;

  // Keep track of current mask configuration so we can use cached values if nothing has changed.
  int lastWidth = -1;
  int lastHeight = -1;

  RoundedCornerWrapperView(
      Context context,
      RoundedCorners roundedCorners,
      Supplier<Boolean> isRtLSupplier,
      int radiusOverride,
      Borders borders) {
    super(context);

    this.radiusOverride = radiusOverride;
    this.roundedCorners = roundedCorners;
    this.isRtLSupplier = isRtLSupplier;

    this.context = context;
    this.borders = borders;
  }

  /**
   * Creates a border drawable and adds it to this view's foreground. This is called from {@link
   * RoundedCornerWrapperView} because when the corners are rounded, the borders also need to be
   * rounded, and the radius can't properly be calculated until the view has been laid out and the
   * height and width are known. From this class, it is easy to make sure the border is created
   * after layout happens, since we are overriding draw. Draw is guaranteed to happen after layout,
   * so we can make sure that the borders are drawn with the appropriate measurements.
   */
  void addBorders(int radius) {
    if (borders.getWidth() <= 0) {
      return;
    }
    boolean isRtL = isRtLSupplier.get();
    // Set up outline of borders
    float[] outerRadii =
        RoundedCornerViewHelper.createRoundedCornerMask(radius, roundedCorners.getBitmask(), isRtL);

    // Create a drawable to stroke the border
    BorderDrawable borderDrawable =
        new BorderDrawable(context, borders, outerRadii, isRtL, lastWidth, lastHeight);
    this.setForeground(borderDrawable);
  }

  /**
   * Returns the radius, which is only calculated if necessary. If the width and height are the same
   * as the previous width and height, there's no need to re-calculate, and the previous radius is
   * returned. The width and height are needed for radii calculated as a percentage of width or
   * height, and to make sure that the radius isn't too big for the size of the view.
   */
  public int getRadius(int width, int height) {
    if (!hasRoundedCorners() || width == 0 || height == 0) {
      return 0;
    }
    if (radiusOverride > 0) {
      roundedCornerRadius = radiusOverride;
      return roundedCornerRadius;
    }
    if (width == lastWidth && height == lastHeight) {
      return roundedCornerRadius;
    }
    int radius = makeRadius(width, height);
    lastWidth = width;
    lastHeight = height;
    return radius;
  }

  /** Calculates the corner radius, clipping to width or height when necessary. */
  private int makeRadius(int width, int height) {

    int radius = 0;
    RadiusOptionsCase radiusOptions = roundedCorners.getRadiusOptionsCase();

    switch (radiusOptions) {
      case RADIUS_DP:
        radius = (int) LayoutUtils.dpToPx(roundedCorners.getRadiusDp(), context);
        break;
      case RADIUS_PERCENTAGE_OF_HEIGHT:
        radius = roundedCorners.getRadiusPercentageOfHeight() * height / 100;
        break;
      case RADIUS_PERCENTAGE_OF_WIDTH:
        radius = roundedCorners.getRadiusPercentageOfWidth() * width / 100;
        break;
      default:
        // TODO Remove deprecated radius code.
        if (roundedCorners.hasRadius()) {
          radius = (int) LayoutUtils.dpToPx(roundedCorners.getRadius(), context);
        }
    }

    roundedCornerRadius = adjustRadiusIfTooBig(width, height, radius);
    return roundedCornerRadius;
  }

  /**
   * Returns the radius that was passed in, or a smaller radius if necessary. If the current radius
   * is bigger than the width or height, or if it has adjacent rounded corners and the radius is
   * more than half of the width or height, the radius is made smaller. It shrinks on all sides,
   * even if only one corner needs to shrink--Piet does not allow different corners to have
   * different radii.
   */
  private int adjustRadiusIfTooBig(int width, int height, int currentRadius) {
    currentRadius = Math.min(currentRadius, height);
    currentRadius = Math.min(currentRadius, width);

    if (hasVerticallyAdjacentRoundedCorners()) {
      currentRadius = Math.min(currentRadius, height / 2);
    }
    if (hasHorizontallyAdjacentRoundedCorners()) {
      currentRadius = Math.min(currentRadius, width / 2);
    }
    return currentRadius;
  }

  /** This should always be true; we should not be using this view when corners are not round. */
  public boolean hasRoundedCorners() {
    return RoundedCornerViewHelper.hasValidRoundedCorners(roundedCorners, radiusOverride);
  }

  private boolean hasVerticallyAdjacentRoundedCorners() {
    return (shouldRoundCorner(Corners.TOP_START) && shouldRoundCorner(Corners.BOTTOM_START))
        || (shouldRoundCorner(Corners.TOP_END) && shouldRoundCorner(Corners.BOTTOM_END));
  }

  private boolean hasHorizontallyAdjacentRoundedCorners() {
    return (shouldRoundCorner(Corners.TOP_START) && shouldRoundCorner(Corners.TOP_END))
        || (shouldRoundCorner(Corners.BOTTOM_START) && shouldRoundCorner(Corners.BOTTOM_END));
  }

  boolean shouldRoundCorner(Corners corner) {
    int bitmask = roundedCorners.getBitmask();
    return (bitmask == 0) || (bitmask & corner.getNumber()) != 0;
  }
}
