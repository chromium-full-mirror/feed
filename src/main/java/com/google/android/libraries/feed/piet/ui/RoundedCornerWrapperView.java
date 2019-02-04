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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;

/** Wrapper for {@link View} instances in Piet that require rounded corners. */
public class RoundedCornerWrapperView extends FrameLayout {

  private static final String TAG = "RoundedCornerWrapper";
  private final Paint paint;
  private final Paint maskPaint;
  private final int roundedCornerRadius;
  private final int bitmask;

  /*@Nullable*/ private RoundRectShape outlineShape = null;

  // Masks for each of the corners of the view; null if that corner is not rounded.
  /*@Nullable*/ private Bitmap cornerTL = null;
  /*@Nullable*/ private Bitmap cornerTR = null;
  /*@Nullable*/ private Bitmap cornerBL = null;
  /*@Nullable*/ private Bitmap cornerBR = null;

  // Keep track of current mask configuration so we can use cached values if nothing has changed.
  int lastRadius = -1;
  boolean lastLtR;

  // Doesn't like the call to setOutlineProvider
  @SuppressWarnings("initialization")
  public RoundedCornerWrapperView(Context context, int bitmask, int roundedCornerRadius) {
    super(context);
    this.roundedCornerRadius = roundedCornerRadius;
    lastLtR = isLtR();

    paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    maskPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

    this.bitmask = bitmask;

    if (hasRoundedCorners() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      super.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
              RoundRectShape localOutlineShape = outlineShape;
              if (localOutlineShape == null
                  || localOutlineShape.getHeight() != view.getHeight()
                  || localOutlineShape.getWidth() != view.getWidth()) {
                int radius = makeRadius(view.getWidth(), view.getHeight(), roundedCornerRadius);
                float[] radii = RoundedCornerViewHelper.createRoundedCornerMask(radius, bitmask);
                localOutlineShape = new RoundRectShape(radii, null, null);
                localOutlineShape.resize(view.getWidth(), view.getHeight());
                outlineShape = localOutlineShape;
              }
              localOutlineShape.getOutline(outline);
            }
          });
    }

    // If the view never gets smaller in one dimension than radius * 2, this will be the only time
    // we need to create the corner masks.
    makeCornerMasks(roundedCornerRadius, lastLtR);

    setWillNotDraw(false);
  }

  /**
   * Make masks for each corner, as applicable.
   *
   * <p>This will no-op if radius and isLtR are the same as the last call.
   */
  void makeCornerMasks(int radius, boolean isLtR) {
    if (!hasRoundedCorners()) {
      return;
    }
    if (radius == lastRadius && isLtR == lastLtR) {
      return;
    }

    Canvas canvas = new Canvas();

    if ((shouldRoundCorner(Corners.TOP_START) && isLtR)
        || (shouldRoundCorner(Corners.TOP_END) && !isLtR)) {
      cornerTL = Bitmap.createBitmap(radius, radius, Config.ALPHA_8);
      canvas.setBitmap(cornerTL);
      canvas.drawColor(Color.BLACK);
      canvas.drawCircle(radius, radius, radius, maskPaint);
    }

    if ((shouldRoundCorner(Corners.TOP_END) && isLtR)
        || (shouldRoundCorner(Corners.TOP_START) && !isLtR)) {
      cornerTR = Bitmap.createBitmap(roundedCornerRadius, roundedCornerRadius, Config.ALPHA_8);
      canvas.setBitmap(cornerTR);
      canvas.drawColor(Color.BLACK);
      canvas.drawCircle(0, radius, radius, maskPaint);
    }

    if ((shouldRoundCorner(Corners.BOTTOM_START) && isLtR)
        || (shouldRoundCorner(Corners.BOTTOM_END) && !isLtR)) {
      cornerBL = Bitmap.createBitmap(radius, radius, Config.ALPHA_8);
      canvas.setBitmap(cornerBL);
      canvas.drawColor(Color.BLACK);
      canvas.drawCircle(radius, 0, radius, maskPaint);
    }

    if ((shouldRoundCorner(Corners.BOTTOM_END) && isLtR)
        || (shouldRoundCorner(Corners.BOTTOM_START) && !isLtR)) {
      cornerBR = Bitmap.createBitmap(radius, radius, Config.ALPHA_8);
      canvas.setBitmap(cornerBR);
      canvas.drawColor(Color.BLACK);
      canvas.drawCircle(0, 0, radius, maskPaint);
    }

    lastRadius = radius;
    lastLtR = isLtR;
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
  public void draw(Canvas canvas) {
    if (!hasRoundedCorners()) {
      super.draw(canvas);
      return;
    }
    int width = getWidth();
    int height = getHeight();
    if (width == 0 || height == 0) {
      // Bitmap creation will fail in these cases
      return;
    }

    boolean isLtR = isLtR();
    int radius = makeRadius(width, height, roundedCornerRadius);

    // We need to create a new bitmap because the default canvas doesn't have transparency (?)
    Bitmap offscreenBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    Canvas offscreenCanvas = new Canvas(offscreenBitmap);

    // Draw the view without rounded corners on the offscreen canvas.
    super.draw(offscreenCanvas);

    // Set up the corner masks. This should no-op if radius and isLtR have not changed.
    makeCornerMasks(radius, isLtR);

    // Crop the corners off using masks
    maskCorners(offscreenCanvas, width, height, radius, maskPaint);

    // Draw the offscreen bitmap (view with rounded corners) to the target canvas.
    canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint);
  }

  /** Calculates the corner radius, clipping it to 50% of the smaller of view height and width. */
  private static int makeRadius(int width, int height, int desiredRadius) {
    if (desiredRadius == 0) {
      return 0;
    }
    int radius = desiredRadius;

    // Radius can't be bigger than half of the width or height of the view.
    // TODO: Allow for radius to be greater than half of the dimensions as long as adjacent
    // corners aren't rounded.
    if (radius > width / 2 || radius > height / 2) {
      radius = Math.min(width / 2, height / 2);
      Logger.w(
          TAG,
          "Can't use radius of %s px, instead using %s. Width: %s, height: %s",
          desiredRadius,
          radius,
          width,
          height);
    }
    return radius;
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

  // TODO: Pass this in as a Supplier so the host can change it.
  private boolean isLtR() {
    return !LayoutUtils.isDefaultLocaleRtl();
  }

  /** This should always be true; we should not be using this view when corners are not round. */
  public boolean hasRoundedCorners() {
    return roundedCornerRadius > 0;
  }

  private boolean shouldRoundCorner(Corners corner) {
    return (bitmask == 0) || (bitmask & corner.getNumber()) != 0;
  }
}
