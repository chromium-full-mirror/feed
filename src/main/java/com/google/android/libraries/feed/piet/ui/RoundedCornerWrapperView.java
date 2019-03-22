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

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.ui.RoundedCornerMaskCache.Corner;
import com.google.android.libraries.feed.piet.ui.RoundedCornerMaskCache.RoundedCornerBitmaps;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.RadiusOptionsCase;
import com.google.search.now.ui.piet.StylesProto.Borders;

/** Wrapper for {@link View} instances in Piet that require rounded corners. */
public class RoundedCornerWrapperView extends FrameLayout {

  private final Paint paint;
  private final Paint maskPaint;
  private final int radiusOverride;
  private final RoundedCorners roundedCorners;
  private final Supplier<Boolean> isRtLSupplier;
  private final Context context;
  private final Borders borders;
  private final RoundedCornerMaskCache maskCache;
  private final Canvas offscreenCanvas;

  /*@Nullable*/ private Bitmap offscreenBitmap = null;
  /*@Nullable*/ private RoundRectShape outlineShape = null;
  private int roundedCornerRadius;

  // Masks for each of the corners of the view; null if that corner is not rounded.
  /*@Nullable*/ private Bitmap cornerTL = null;
  /*@Nullable*/ private Bitmap cornerTR = null;
  /*@Nullable*/ private Bitmap cornerBL = null;
  /*@Nullable*/ private Bitmap cornerBR = null;

  // Keep track of current mask configuration so we can use cached values if nothing has changed.
  private int lastRadius = -1;
  private int lastWidth = -1;
  private int lastHeight = -1;
  private boolean lastRtL;

  // Doesn't like the call to setOutlineProvider
  @SuppressWarnings("initialization")
  public RoundedCornerWrapperView(
      Context context,
      RoundedCorners roundedCorners,
      RoundedCornerMaskCache maskCache,
      Supplier<Boolean> isRtLSupplier,
      int radiusOverride,
      Borders borders) {
    super(context);
    this.maskCache = maskCache;
    this.isRtLSupplier = isRtLSupplier;
    this.roundedCorners = roundedCorners;
    this.radiusOverride = radiusOverride;
    this.context = context;
    this.borders = borders;
    offscreenCanvas = new Canvas();
    lastRtL = !isRtLSupplier.get(); // Flip this so we must update the layout on the first time.

    this.paint = maskCache.getPaint();
    this.maskPaint = maskCache.getMaskPaint();

    if (hasRoundedCorners() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      super.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
              RoundRectShape localOutlineShape = outlineShape;
              if (localOutlineShape == null
                  || localOutlineShape.getHeight() != view.getHeight()
                  || localOutlineShape.getWidth() != view.getWidth()) {
                int radius = getRadius(view.getWidth(), view.getHeight());
                float[] radii =
                    RoundedCornerViewHelper.createRoundedCornerMask(
                        radius, roundedCorners.getBitmask(), isRtLSupplier.get());
                localOutlineShape = new RoundRectShape(radii, null, null);
                localOutlineShape.resize(view.getWidth(), view.getHeight());
                outlineShape = localOutlineShape;
              }
              localOutlineShape.getOutline(outline);
            }
          });
    }

    setWillNotDraw(false);
  }

  private void initCornerMasks(int radius, boolean isRtL) {
    RoundedCornerBitmaps masks = maskCache.getMasks(radius);

    if ((shouldRoundCorner(Corners.TOP_START) && !isRtL)
        || (shouldRoundCorner(Corners.TOP_END) && isRtL)) {
      cornerTL = masks.get(Corner.TOP_LEFT);
    } else {
      cornerTL = null;
    }

    if ((shouldRoundCorner(Corners.TOP_END) && !isRtL)
        || (shouldRoundCorner(Corners.TOP_START) && isRtL)) {
      cornerTR = masks.get(Corner.TOP_RIGHT);
    } else {
      cornerTR = null;
    }

    if ((shouldRoundCorner(Corners.BOTTOM_START) && !isRtL)
        || (shouldRoundCorner(Corners.BOTTOM_END) && isRtL)) {
      cornerBL = masks.get(Corner.BOTTOM_LEFT);
    } else {
      cornerBL = null;
    }

    if ((shouldRoundCorner(Corners.BOTTOM_END) && !isRtL)
        || (shouldRoundCorner(Corners.BOTTOM_START) && isRtL)) {
      cornerBR = masks.get(Corner.BOTTOM_RIGHT);
    } else {
      cornerBR = null;
    }
  }

  /**
   * Creates corner masks (which cover the parts of the corners that should not be shown) and
   * borders, as necessary. Both must be created after the radius is known. However, if the size of
   * the view and the LtR remain the same, it will only create the masks and borders once.
   */
  private void setupCornerMasksAndBorders(int radius) {
    if (!hasRoundedCorners()) {
      return;
    }
    boolean isRtL = isRtLSupplier.get();
    if (radius == lastRadius && isRtL == lastRtL) {
      return;
    }

    initCornerMasks(radius, isRtL);
    addBorders(radius);

    lastRadius = radius;
    lastRtL = isRtL;
  }

  /**
   * Creates a border drawable and adds it to this view's foreground. This is called from {@link
   * RoundedCornerWrapperView} because when the corners are rounded, the borders also need to be
   * rounded, and the radius can't properly be calculated until the view has been laid out and the
   * height and width are known. From this class, it is easy to make sure the border is created
   * after layout happens, since we are overriding draw. Draw is guaranteed to happen after layout,
   * so we can make sure that the borders are drawn with the appropriate measurements.
   */
  private void addBorders(int radius) {
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
   * Ensures that the wrapper view is invalidated when child views are invalidated. This method only
   * exists in Android O+.
   */
  @Override
  public void onDescendantInvalidated(View child, View target) {
    super.onDescendantInvalidated(child, target);
    if (hasRoundedCorners()) {
      Rect targetRect = new Rect();
      target.getDrawingRect(targetRect);
      invalidate(targetRect);
    }
  }

  /**
   * Using as an indicator that the child view was invalidated. By overriding this method, we ensure
   * that the wrapper view is invalidated when the child view is. This is only used in Android N-
   * and is deprecated, but we must use it because onDescendantInvalidated only exists in O+.
   */
  @Override
  public ViewParent invalidateChildInParent(final int[] location, final Rect dirty) {
    if (hasRoundedCorners()) {
      invalidate(dirty);
    }
    return super.invalidateChildInParent(location, dirty);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (!changed) {
      return;
    }

    int width = getWidth();
    int height = getHeight();
    if (width == 0 || height == 0) {
      // The view is not visible; no further processing is needed.
      return;
    }

    if (offscreenBitmap == null
        || offscreenBitmap.getHeight() != height
        || offscreenBitmap.getWidth() != width) {
      // We need to use an offscreen bitmap because the default canvas doesn't have transparency (?)
      if (offscreenBitmap != null) {
        offscreenBitmap.recycle();
      }
      offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      offscreenCanvas.setBitmap(offscreenBitmap);
    }

    int radius = getRadius(width, height);

    // Set up the corner masks and borders, both of which require knowing the radius.
    // This should no-op if radius and isLtR have not changed.
    setupCornerMasksAndBorders(radius);
  }

  @Override
  public void draw(Canvas canvas) {
    if (!hasRoundedCorners()) {
      super.draw(canvas);
      return;
    }
    int width = getWidth();
    int height = getHeight();
    if (width == 0 || height == 0) {
      // The view is not visible, and offscreenBitmap creation will fail. Stop here.
      return;
    }

    int radius = getRadius(width, height);

    // Draw the view without rounded corners on the offscreen canvas.
    Bitmap localOffscreenBitmap = checkNotNull(offscreenBitmap);
    localOffscreenBitmap.eraseColor(Color.TRANSPARENT);
    super.draw(offscreenCanvas);

    // Crop the corners off using masks
    maskCorners(offscreenCanvas, width, height, radius, maskPaint);

    // Draw the offscreen bitmap (view with rounded corners) to the target canvas.
    canvas.drawBitmap(localOffscreenBitmap, 0f, 0f, paint);
  }

  /**
   * Returns the radius, which is only calculated if necessary. If the width and height are the same
   * as the previous width and height, there's no need to re-calculate, and the previous radius is
   * returned. The width and height are needed for radii calculated as a percentage of width or
   * height, and to make sure that the radius isn't too big for the size of the view.
   */
  public int getRadius(int width, int height) {
    if (!hasRoundedCorners()) {
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

  /** Draws a mask on each corner that is rounded. */
  private void maskCorners(Canvas canvas, int width, int height, int radius, Paint paint) {
    if (cornerTL != null) {
      canvas.drawBitmap(cornerTL, 0, 0, paint);
    }
    if (cornerTR != null) {
      canvas.drawBitmap(cornerTR, width - radius, 0, paint);
    }
    if (cornerBL != null) {
      canvas.drawBitmap(cornerBL, 0, height - radius, paint);
    }
    if (cornerBR != null) {
      canvas.drawBitmap(cornerBR, width - radius, height - radius, paint);
    }
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

  private boolean shouldRoundCorner(Corners corner) {
    int bitmask = roundedCorners.getBitmask();
    return (bitmask == 0) || (bitmask & corner.getNumber()) != 0;
  }
}
