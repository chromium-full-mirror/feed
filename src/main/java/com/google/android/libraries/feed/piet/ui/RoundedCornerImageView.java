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
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.google.android.libraries.feed.common.ui.LayoutUtils;

/** {@link ImageView} which is able to show colors and bitmaps with rounded-off corners. */
public class RoundedCornerImageView extends ImageView {

  /*@Nullable*/ @VisibleForTesting Shape roundedRectangle;
  /*@Nullable*/ @VisibleForTesting BitmapShader shader;
  /*@Nullable*/ @VisibleForTesting Paint paint;
  private float radius;
  private int cornerField;

  // Whether or not to apply the shader, if we have one. This might be set to false if the image
  // is smaller than the view and does not need to have the corners rounded.
  private boolean applyShader;

  public RoundedCornerImageView(Context context) {
    this(context, null, 0);
  }

  public RoundedCornerImageView(Context context, /*@Nullable*/ AttributeSet attrs) {
    this(context, attrs, 0);
  }

  // Nullness checker doesn't like null attrs in super call but this is valid.
  @SuppressWarnings("initialization")
  public RoundedCornerImageView(Context context, /*@Nullable*/ AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    roundedRectangle = null;
    paint = null;
  }

  private void createRoundedCornerMask(float radius, int cornerField) {
    // If we don't have any radius values, don't bother creating the mask.
    if (radius > 0) {
      int layoutDirection =
          LayoutUtils.isDefaultLocaleRtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
      float[] radii =
          RoundedCornerViewHelper.createRoundedCornerMask(radius, cornerField, layoutDirection);
      roundedRectangle = new RoundRectShape(radii, null, null);
    } else {
      roundedRectangle = null;
    }

    maybeCreateShader();
  }

  /**
   * Updates the rounded corner, using the radius set in the layout.
   *
   * @param roundedCorners Bitmask from {@link RoundedCorners.getBitmask()}
   */
  public void setRoundedCorners(int roundedCorners) {
    if (cornerField == roundedCorners) {
      return;
    }
    cornerField = roundedCorners;
    createRoundedCornerMask(radius, roundedCorners);

    // Request a re-draw.
    invalidate();
  }

  @Override
  public void setImageDrawable(/*@Nullable*/ Drawable drawable) {
    super.setImageDrawable(drawable);

    // Reset shaders.  We will need to recalculate them.
    shader = null;
    applyShader = false;

    maybeCreateShader();

    updateApplyShader();
  }

  protected void maybeCreateShader() {
    // Only create the shader if we have a rectangle to use as a mask.
    Drawable drawable = getDrawable();
    Bitmap bitmap = null;
    if (drawable instanceof BitmapDrawable) {
      bitmap = ((BitmapDrawable) drawable).getBitmap();
    } else if (drawable instanceof LayerDrawable) {
      // TODO: Find a better way to support LayerDrawables as converting to a Bitmap is
      // expensive memory-wise.
      bitmap = convertLayerDrawableToBitmap((LayerDrawable) drawable);
    }
    if (roundedRectangle != null && bitmap != null) {
      shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

      paint = new Paint();
      paint.setAntiAlias(true);
    } else {
      shader = null;
      paint = null;
    }
  }

  @Override
  protected boolean setFrame(int l, int t, int r, int b) {
    boolean changed = super.setFrame(l, t, r, b);
    updateApplyShader();
    return changed;
  }

  @Override
  public void setScaleType(ScaleType scaleType) {
    super.setScaleType(scaleType);
    updateApplyShader();
  }

  /**
   * This custom onMeasure scales the image to fill the container. If the container has only one
   * constrained dimension, the image is aspect ratio scaled to its max possible size given the
   * constraining dimension.
   *
   * <p>This is overridden because adjustViewBounds does not scale up small images in API 17-, and
   * because we want the image to scale independent of the dimensions of the Drawable - the image
   * should not change size based on the resolution of the Drawable, only the aspect ratio.
   */
  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // If we don't have any drawable, there's nothing special we can do.
    Drawable drawable = getDrawable();
    if (drawable == null
        || drawable.getIntrinsicWidth() == -1
        || drawable.getIntrinsicHeight() == -1) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

    int drawableWidth = drawable.getIntrinsicWidth();
    int drawableHeight = drawable.getIntrinsicHeight();

    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

    if (widthSpecMode == MeasureSpec.UNSPECIFIED && heightSpecMode != MeasureSpec.UNSPECIFIED) {
      // Aspect ratio scale the width
      measuredWidth = aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight);
    } else if (heightSpecMode == MeasureSpec.UNSPECIFIED
        && widthSpecMode != MeasureSpec.UNSPECIFIED) {
      // Aspect ratio scale the height
      measuredHeight = aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight);
    } else if (heightSpecMode == MeasureSpec.UNSPECIFIED
        && widthSpecMode == MeasureSpec.UNSPECIFIED) {
      // If both are UNSPECIFIED, take up as much room as possible.
      measuredWidth = Integer.MAX_VALUE;
      measuredHeight = Integer.MAX_VALUE;
    } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
      measuredWidth =
          Math.min(
              aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight), measuredWidth);
    } else if (heightSpecMode == MeasureSpec.AT_MOST && widthSpecMode == MeasureSpec.EXACTLY) {
      measuredHeight =
          Math.min(
              aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight), measuredHeight);
    } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
      int desiredWidth = aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight);
      int desiredHeight = aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight);
      if (desiredWidth < measuredWidth) {
        measuredWidth = desiredWidth;
      } else if (desiredHeight < measuredHeight) {
        measuredHeight = desiredHeight;
      }
    }
    // else keep values from the MeasureSpec because both modes are EXACTLY.

    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  private int aspectRatioScaleWidth(int constrainingHeight, int drawableWidth, int drawableHeight) {
    int imageHeight = constrainingHeight - getPaddingTop() - getPaddingBottom();
    int imageWidth = imageHeight * drawableWidth / drawableHeight;
    return imageWidth + getPaddingRight() + getPaddingLeft();
  }

  private int aspectRatioScaleHeight(int constrainingWidth, int drawableWidth, int drawableHeight) {
    int imageWidth = constrainingWidth - getPaddingRight() - getPaddingLeft();
    int imageHeight = imageWidth * drawableHeight / drawableWidth;
    return imageHeight + getPaddingTop() + getPaddingBottom();
  }

  /**
   * Updates the flag to tell whether or not to apply the shader that produces the rounded corners.
   * We should not apply the shader if the final image is smaller than the view, because it will try
   * to tile the image, which is not desirable. This should be called when the image is changed, or
   * the view bounds change.
   */
  private void updateApplyShader() {
    Drawable drawable = getDrawable();
    if ((drawable == null)
        || (!(drawable instanceof BitmapDrawable) && !(drawable instanceof LayerDrawable))
        || (shader == null)
        || (paint == null)) {
      // In this state we wouldn't use the shader anyway.
      applyShader = false;
      return;
    }

    // Default to using the shader.
    applyShader = true;

    // If the scale type would not scale up the image, and the image is smaller than the
    // view bounds, then just draw it normally so the shader won't have adverse effects.
    // CENTER does not do any scaling, and simply centers the image. In that case we need to
    // check to see if the image is smaller than the view in either dimension, and don't apply
    // the shader if it is. CENTER_INSIDE will only scale down, so we need to calculate the
    // scaled size of the image, and only apply the shader if it happens to match the size of
    // the view.
    // TODO: this won't work with a custom image matrix, but that's probably ok for now
    ScaleType scaleType = getScaleType();
    if (scaleType == ScaleType.CENTER || scaleType == ScaleType.CENTER_INSIDE) {
      int viewWidth = getWidth() - getPaddingRight() - getPaddingLeft();
      int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
      int drawableWidth = drawable.getIntrinsicWidth();
      int drawableHeight = drawable.getIntrinsicHeight();
      if (scaleType == ScaleType.CENTER_INSIDE) {
        float scale =
            Math.min(
                (float) viewWidth / (float) drawableWidth,
                (float) viewHeight / (float) drawableHeight);
        drawableWidth = (int) ((scale * drawableWidth) + 0.5f);
        drawableHeight = (int) ((scale * drawableHeight) + 0.5f);
      }
      if ((drawableWidth < viewWidth) || (drawableHeight < viewHeight)) {
        applyShader = false;
      }
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    Drawable drawable = getDrawable();
    Shape localRoundedRect = roundedRectangle;
    Paint localPaint = paint;
    if (drawable == null || localPaint == null || localRoundedRect == null) {
      super.onDraw(canvas);
      return;
    }

    if (!isSupportedDrawable(drawable)) {
      super.onDraw(canvas);
      return;
    }

    if (drawable instanceof ColorDrawable) {
      ColorDrawable colorDrawable = (ColorDrawable) drawable;
      localPaint.setColor(colorDrawable.getColor());
    }

    if (shader != null && applyShader) {
      shader.setLocalMatrix(getImageMatrix());
      localPaint.setShader(shader);
    }

    // Paint contains the configuration we need.
    // TODO: This does not properly handle padding.  Padding will not be taken into account
    // when rounded corners are used. We will fix this as part of implemented rounded corners on all
    // views.
    localRoundedRect.resize(getWidth(), getHeight());
    localRoundedRect.draw(canvas, localPaint);
  }

  public void setCornerRadius(int radius) {
    if (this.radius == radius) {
      return;
    }
    this.radius = radius;
    createRoundedCornerMask(this.radius, cornerField);
    invalidate();
  }

  public float getCornerRadius() {
    return radius;
  }

  /*@Nullable*/
  private Bitmap convertLayerDrawableToBitmap(LayerDrawable layerDrawable) {
    // Cannot convert LayerDrawable if intrinsic width and height are unknown.
    if (layerDrawable.getIntrinsicWidth() == -1 || layerDrawable.getIntrinsicHeight() == -1) {
      return null;
    }

    Bitmap bitmap =
        Bitmap.createBitmap(
            layerDrawable.getIntrinsicWidth(),
            layerDrawable.getIntrinsicHeight(),
            Config.ARGB_8888);

    layerDrawable.draw(new Canvas(bitmap));
    return bitmap;
  }

  private boolean isSupportedDrawable(Drawable drawable) {
    return (drawable instanceof ColorDrawable)
        || (drawable instanceof BitmapDrawable)
        || (drawable instanceof LayerDrawable);
  }
}
