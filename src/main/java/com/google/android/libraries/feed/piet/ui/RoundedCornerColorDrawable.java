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
import javax.annotation.Nullable;

/** Extension of {@code ColorDrawable} that will render with rounded corners. */
public class RoundedCornerColorDrawable extends ColorDrawable {
  @Nullable private Shape roundedRectangle;
  @Nullable private Paint paint;
  private final float radius;

  public static final int TOP_LEFT = 1;
  public static final int TOP_RIGHT = 2;
  public static final int BOTTOM_RIGHT = 4;
  public static final int BOTTOM_LEFT = 8;

  public static final int TOP_CORNERS = TOP_LEFT | TOP_RIGHT;
  public static final int BOTTOM_CORNERS = BOTTOM_LEFT | BOTTOM_RIGHT;

  public RoundedCornerColorDrawable(int color, int radius) {
    super(color);
    this.radius = radius;
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
   * @param roundedCorners any of {@code TOP_LEFT}, {@code TOP_RIGHT}, {@code BOTTOM_RIGHT}, or
   *     {@code BOTTOM_LEFT}, or {@code 0}
   */
  public void setRoundedCorners(int roundedCorners) {
    createRoundedCornerMask(radius, roundedCorners);

    // Request a re-draw.
    invalidateSelf();
  }

  private void createRoundedCornerMask(float radius, int cornerField) {
    // If we don't have any radius values, don't bother creating the mask.
    if (radius > 0 && cornerField > 0) {
      float[] radii = new float[8];
      if ((cornerField & TOP_LEFT) != 0) {
        radii[0] = radius;
        radii[1] = radius;
      }
      if ((cornerField & TOP_RIGHT) != 0) {
        radii[2] = radius;
        radii[3] = radius;
      }
      if ((cornerField & BOTTOM_RIGHT) != 0) {
        radii[4] = radius;
        radii[5] = radius;
      }
      if ((cornerField & BOTTOM_LEFT) != 0) {
        radii[6] = radius;
        radii[7] = radius;
      }
      roundedRectangle = new RoundRectShape(radii, null, null);
      paint = new Paint();
      paint.setAntiAlias(true);
    } else {
      roundedRectangle = null;
      paint = null;
    }
  }
}
