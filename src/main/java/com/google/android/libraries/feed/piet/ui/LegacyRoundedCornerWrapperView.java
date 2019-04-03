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

import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.Borders;

/**
 * Wrapper for {@link View} instances in Piet that require rounded corners.
 *
 * <p>This class does not support antialiasing on rounded corners, but has much better memory and
 * CPU performance on JB and KK (API 16 and 19)
 */
public class LegacyRoundedCornerWrapperView extends RoundedCornerWrapperView {

  /*@Nullable*/ private Path clipPath = null;

  // Doesn't like the call to setLayerType
  @SuppressWarnings("initialization")
  public LegacyRoundedCornerWrapperView(
      Context context,
      RoundedCorners roundedCorners,
      Supplier<Boolean> isRtLSupplier,
      int radiusOverride,
      Borders borders) {
    super(context, roundedCorners, isRtLSupplier, radiusOverride, borders);

    checkState(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);

    if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
      // clipPath doesn't work with hardware rendering on < 18.
      setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    int radius = getRadius(w, h);
    float[] radii =
        RoundedCornerViewHelper.createRoundedCornerMask(
            radius, roundedCorners.getBitmask(), isRtLSupplier.get());

    clipPath = new Path();
    clipPath.addRoundRect(new RectF(0, 0, w, h), radii, Path.Direction.CW);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (clipPath != null) {
      canvas.clipPath(clipPath);
    }
    super.dispatchDraw(canvas);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (!changed || !hasRoundedCorners()) {
      return;
    }

    int width = getWidth();
    int height = getHeight();
    if (width == 0 || height == 0) {
      // The view is not visible; no further processing is needed.
      return;
    }

    int radius = getRadius(width, height);

    addBorders(radius);
  }
}
