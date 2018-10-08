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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.VisibleForTesting;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;

/** Wrapper for {@link View} instances in Piet that require rounded corners. */
public class RoundedCornerWrapperView extends FrameLayout {

  private static final String TAG = "RoundedCornerWrapper";
  private final Paint maskPaint;
  private final PorterDuffXfermode xfermode;
  private final Paint paint;
  private final RoundedCorners roundedCorners;

  // This is instantiated in the constructor and modified before use to save time on garbage
  // collection.
  private final RectF maskRect;

  private Bitmap maskBitmap;

  public RoundedCornerWrapperView(Context context) {
    this(context, RoundedCorners.getDefaultInstance());
  }

  public RoundedCornerWrapperView(Context context, RoundedCorners roundedCorners) {
    super(context);

    maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    xfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    maskPaint.setXfermode(xfermode);
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    maskRect = new RectF();
    this.roundedCorners = roundedCorners;

    setWillNotDraw(false);
  }

  @Override
  public void draw(Canvas canvas) {
    if (!shouldRoundAnyCorner()) {
      super.draw(canvas);
      return;
    }

    Bitmap offscreenBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    Canvas offscreenCanvas = new Canvas(offscreenBitmap);

    // Draw the view without rounded corners on the offscreen canvas.
    super.draw(offscreenCanvas);

    if (maskBitmap == null
        || maskBitmap.getHeight() != getHeight()
        || maskBitmap.getWidth() != getWidth()) {
      maskBitmap = createMask(getWidth(), getHeight());
    }

    // Mask the parts of the view that should be included
    offscreenCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint);

    paint.setXfermode(null);
    canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint);
  }

  private Bitmap createMask(int width, int height) {
    int radius =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                roundedCorners.getRadius(),
                getContext().getResources().getDisplayMetrics());

    // Radius can't be bigger than half of the width or height of the view.
    // TODO: Allow for radius to be greater than half of the dimensions as long as adjacent
    // corners aren't rounded.
    if (radius > width / 2 || radius > height / 2) {
      radius = Math.min(width / 2, height / 2);
      Logger.w(
          TAG,
          "Can't use radius of %s px, instead using %s. Width: %s, height: %s",
          radius,
          width,
          height);
    }

    Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
    Canvas canvas = new Canvas(mask);

    canvas.drawRect(0, 0, width, height, paint);

    paint.setXfermode(xfermode);

    // First, we want the mask to include everything other than the parts of the corners not visible
    // because of the rounding.
    maskRect.set(0, 0, width, height);
    canvas.drawRoundRect(maskRect, radius, radius, paint);

    // For each corner, we want to include the corner in the mask if we aren't rounding it.
    if (!shouldRoundCorner(Corners.TOP_START)) {
      canvas.drawRect(
          LayoutUtils.isDefaultLocaleRtl() ? topRight(width, radius) : topLeft(radius), paint);
    }
    if (!shouldRoundCorner(Corners.TOP_END)) {
      canvas.drawRect(
          LayoutUtils.isDefaultLocaleRtl() ? topLeft(radius) : topRight(width, radius), paint);
    }
    if (!shouldRoundCorner(Corners.BOTTOM_START)) {
      canvas.drawRect(
          LayoutUtils.isDefaultLocaleRtl()
              ? bottomRight(width, height, radius)
              : bottomLeft(height, radius),
          paint);
    }
    if (!shouldRoundCorner(Corners.BOTTOM_END)) {
      canvas.drawRect(
          LayoutUtils.isDefaultLocaleRtl()
              ? bottomLeft(height, radius)
              : bottomRight(width, height, radius),
          paint);
    }

    return mask;
  }

  private RectF topLeft(int radius) {
    maskRect.set(0, 0, radius, radius);
    return maskRect;
  }

  private RectF topRight(int width, int radius) {
    maskRect.set(width - radius, 0, width, radius);
    return maskRect;
  }

  private RectF bottomLeft(int height, int radius) {
    maskRect.set(0, height - radius, radius, height);
    return maskRect;
  }

  private RectF bottomRight(int width, int height, int radius) {
    maskRect.set(width - radius, height - radius, width, height);
    return maskRect;
  }

  private boolean shouldRoundAnyCorner() {
    return roundedCorners.getRadius() > 0;
  }

  private boolean shouldRoundCorner(Corners corner) {
    return (roundedCorners.getBitmask() == 0)
        || (roundedCorners.getBitmask() & corner.getNumber()) != 0;
  }

  @VisibleForTesting
  public RoundedCorners getRoundedCorners() {
    return roundedCorners;
  }
}
