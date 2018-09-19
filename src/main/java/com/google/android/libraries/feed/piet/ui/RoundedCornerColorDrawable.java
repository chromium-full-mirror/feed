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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;

/** Extension of {@code ColorDrawable} that will render with rounded corners. */
public class RoundedCornerColorDrawable extends ColorDrawable {
  /*@Nullable*/ private Shape roundedRectangle;
  /*@Nullable*/ private Paint paint;
  private float radius;
  private int cornerField;
  private int layoutDirection;

  public RoundedCornerColorDrawable(int color) {
    super(color);
  }

  @Override
  public void draw(Canvas canvas) {
    Shape localRoundedRectangle = roundedRectangle;
    if (localRoundedRectangle == null) {
      super.draw(canvas);
    } else {
      Rect bounds = getBounds();
      localRoundedRectangle.resize(bounds.right, bounds.bottom);

      if (paint != null) {
        // paint contains the shader that will texture the shape
        paint.setColor(getColor());
        localRoundedRectangle.draw(canvas, paint);
      }
    }
  }

  /**
   * Updates the rounded corner, using the radius set in the layout.
   *
   * @param roundedCorners Bitmask from {@link RoundedCorners.getBitmask()}
   * @param layoutDirection Either 0 for {@link android.view.View#LAYOUT_DIRECTION_LTR} or 1 for
   *     {@link android.view.View#LAYOUT_DIRECTION_RTL}. Used to determine whether corners need to
   *     be flipped for RTL
   */
  public void setRoundedCorners(float radius, int roundedCorners, int layoutDirection) {
    if (cornerField == roundedCorners
        && this.radius == radius
        && this.layoutDirection == layoutDirection) {
      return;
    }
    this.cornerField = roundedCorners;
    this.radius = radius;
    this.layoutDirection = layoutDirection;
    createRoundedCornerMask(radius, roundedCorners, layoutDirection);

    // Request a re-draw.
    invalidateSelf();
  }

  private void createRoundedCornerMask(float radius, int cornerField, int layoutDirection) {
    // If we don't have any radius values, don't bother creating the mask.
    if (radius > 0) {
      float[] radii =
          RoundedCornerViewHelper.createRoundedCornerMask(radius, cornerField, layoutDirection);
      roundedRectangle = new RoundRectShape(radii, null, null);
      paint = new Paint();
      paint.setAntiAlias(true);
    } else {
      roundedRectangle = null;
      paint = null;
    }
  }
}
