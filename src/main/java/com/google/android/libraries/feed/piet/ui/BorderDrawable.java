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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.search.now.ui.piet.StylesProto.Borders;
import com.google.search.now.ui.piet.StylesProto.Borders.Edges;

/**
 * Shape used to draw borders. Uses offsets to push the border out of the drawing bounds if it is
 * not specified.
 */
public class BorderDrawable extends ShapeDrawable {
  private final float[] cornerRadii;
  private final int initialWidth;
  private final int initialHeight;
  private final boolean hasLeftBorder;
  private final boolean hasRightBorder;
  private final boolean hasTopBorder;
  private final boolean hasBottomBorder;
  private final int offsetToHideLeft;
  private final int offsetToHideRight;
  private final int offsetToHideTop;
  private final int offsetToHideBottom;
  private final int borderWidth;

  public BorderDrawable(Context context, Borders borders, float[] cornerRadii, boolean isRtL) {
    this(context, borders, cornerRadii, isRtL, /* width= */ 0, /* height= */ 0);
  }

  // Doesn't like calls to getPaint()
  @SuppressWarnings("initialization")
  public BorderDrawable(
      Context context, Borders borders, float[] cornerRadii, boolean isRtL, int width, int height) {
    super(new RoundRectShape(cornerRadii, null, null));
    this.cornerRadii = cornerRadii;
    this.initialWidth = width;
    this.initialHeight = height;

    borderWidth = (int) LayoutUtils.dpToPx(borders.getWidth(), context);

    // Calculate the offsets which push the border outside the view, making it invisible
    int bitmask = borders.getBitmask();
    if (bitmask == 0 || bitmask == 15) {
      // All borders are visible
      hasLeftBorder = true;
      hasRightBorder = true;
      hasTopBorder = true;
      hasBottomBorder = true;
      offsetToHideLeft = 0;
      offsetToHideRight = 0;
      offsetToHideTop = 0;
      offsetToHideBottom = 0;
    } else {
      int leftEdge = isRtL ? Edges.END.getNumber() : Edges.START.getNumber();
      int rightEdge = isRtL ? Edges.START.getNumber() : Edges.END.getNumber();
      hasLeftBorder = (bitmask & leftEdge) != 0;
      hasRightBorder = (bitmask & rightEdge) != 0;
      hasTopBorder = (bitmask & Edges.TOP.getNumber()) != 0;
      hasBottomBorder = (bitmask & Edges.BOTTOM.getNumber()) != 0;
      offsetToHideLeft = hasLeftBorder ? 0 : -borderWidth;
      offsetToHideRight = hasRightBorder ? 0 : borderWidth;
      offsetToHideTop = hasTopBorder ? 0 : -borderWidth;
      offsetToHideBottom = hasBottomBorder ? 0 : borderWidth;
    }
    getPaint().setStyle(Paint.Style.STROKE);
    // Multiply the width by two - the centerline of the stroke will be the edge of the view, so
    // half of the stroke will be outside the view. In order for the visible portion to have the
    // correct width, the full stroke needs to be twice as wide.
    // For rounded corners, this relies on the containing FrameLayout to crop the outside half of
    // the rounded border; otherwise, the border would get thicker on the corners.
    getPaint().setStrokeWidth(borderWidth * 2);
    getPaint().setColor(borders.getColor());
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    super.setBounds(
        left + offsetToHideLeft,
        top + offsetToHideTop,
        right + offsetToHideRight,
        bottom + offsetToHideBottom);
  }

  @Override
  public void setBounds(Rect bounds) {
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

  @Override
  protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.KITKAT && hasLargeRadius()) {
      drawBorderWithPath(canvas, paint);
    } else {
      super.onDraw(shape, canvas, paint);
    }
  }

  /**
   * Checks if the radius is larger than half the width or height. Only applies to elements with
   * rounded corners.
   */
  private boolean hasLargeRadius() {
    if (initialWidth == 0 || initialHeight == 0) {
      return false;
    }
    int radius = 0;
    for (float cornerRadius : cornerRadii) {
      if (cornerRadius != 0) {
        radius = (int) cornerRadius;
        break;
      }
    }
    if (radius == 0) {
      return false;
    }
    return radius > (initialWidth / 2) || radius > (initialHeight / 2);
  }

  /**
   * Draws the border with a {@link Path} instead of a {@link RoundRectShape}. This is used because
   * in JellyBean, RoundRectShape clamps the rounded corner radius to half the length of the side of
   * the shape. When the adjacent corner is not rounded, we want to allow radii that are more than
   * 50% of that side.
   */
  private void drawBorderWithPath(Canvas canvas, Paint paint) {
    Path path = new Path();
    int height = initialHeight;
    int width = initialWidth;
    // This is necessary because of extra height/width added for borders.
    if (!hasTopBorder) {
      height += borderWidth;
    }
    if (!hasLeftBorder) {
      width += borderWidth;
    }
    if (hasTopBorder) {
      if (cornerRadii[0] != 0) {
        // Add second half of top left corner.
        float radius = cornerRadii[0];
        path.addArc(topLeftBoundingBox(radius), 225, 45);
      }
      if (cornerRadii[2] != 0) {
        // If the top right corner is rounded, add the top border and half the top right corner.
        float radius = cornerRadii[2];
        path.lineTo(width - radius, 0);
        path.addArc(topRightBoundingBox(width, radius), 270, 46);
      } else {
        // Add border across top.
        path.lineTo(width, 0);
      }
    } else {
      // If there is no top border, jump to the top right corner.
      path.moveTo(width, 0);
    }

    if (hasRightBorder) {
      if (cornerRadii[2] != 0) {
        // Add second half of top right corner.
        float radius = cornerRadii[2];
        path.addArc(topRightBoundingBox(width, radius), 315, 45);
      }
      if (cornerRadii[4] != 0) {
        // If the bottom right corner is rounded, add right border and half the bottom right corner.
        float radius = cornerRadii[4];
        path.lineTo(width, height - radius);
        path.addArc(bottomRightBoundingBox(width, height, radius), 0, 46);
      } else {
        // Add right border, no rounded corner at bottom.
        path.lineTo(width, height);
      }
    } else {
      // If there is no right border, jump to the bottom right corner.
      path.moveTo(width, height);
    }

    if (hasBottomBorder) {
      if (cornerRadii[4] != 0) {
        // Add second half of bottom right corner.
        float radius = cornerRadii[4];
        path.addArc(bottomRightBoundingBox(width, height, radius), 45, 45);
      }
      if (cornerRadii[6] != 0) {
        // Add bottom border with rounded corner at bottom left.
        float radius = cornerRadii[6];
        path.lineTo(radius, height);
        path.addArc(bottomLeftBoundingBox(height, radius), 90, 46);
      } else {
        path.lineTo(0, height);
      }
    } else {
      // If there is no bottom border, jump to the bottom left corner.
      path.moveTo(0, height);
    }

    if (hasLeftBorder) {
      if (cornerRadii[6] != 0) {
        // Add second half of bottom left corner.
        float radius = cornerRadii[6];
        path.addArc(bottomLeftBoundingBox(height, radius), 135, 45);
      }
      if (cornerRadii[0] != 0) {
        // Add left border with rounded corner at top left.
        float radius = cornerRadii[0];
        path.lineTo(0, radius);
        path.addArc(topLeftBoundingBox(radius), 180, 46);
      } else {
        // Add left border.
        path.lineTo(0, 0);
      }
    }

    // Actually draw the path that was just built. The paint defines the width/color of the border.
    canvas.drawPath(path, paint);
  }

  private RectF topLeftBoundingBox(float radius) {
    return new RectF(0, 0, radius * 2, radius * 2);
  }

  private RectF topRightBoundingBox(float width, float radius) {
    return new RectF(width - (radius * 2), 0, width, radius * 2);
  }

  private RectF bottomRightBoundingBox(float width, float height, float radius) {
    return new RectF(width - (radius * 2), height - (radius * 2), width, height);
  }

  private RectF bottomLeftBoundingBox(float height, float radius) {
    return new RectF(0, height - (radius * 2), radius * 2, height);
  }
}
