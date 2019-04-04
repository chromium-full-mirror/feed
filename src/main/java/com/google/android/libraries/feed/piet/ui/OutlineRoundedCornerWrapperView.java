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
import android.graphics.Outline;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.Borders;

/**
 * Wrapper for {@link View} instances in Piet that require rounded corners.
 *
 * <p>This version uses a much higher-performance clipToOutline strategy, which only works when all
 * 4 corners are rounded.
 */
public class OutlineRoundedCornerWrapperView extends RoundedCornerWrapperView {

  // Doesn't like the call to setOutlineProvider
  @SuppressWarnings("initialization")
  public OutlineRoundedCornerWrapperView(
      Context context,
      RoundedCorners roundedCorners,
      Supplier<Boolean> isRtLSupplier,
      int radiusOverride,
      Borders borders) {
    super(context, roundedCorners, isRtLSupplier, radiusOverride, borders);

    checkState(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && (roundedCorners.getBitmask() == 0 || roundedCorners.getBitmask() == 15));

    if (hasRoundedCorners()) {
      super.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
              int radius = getRadius(view.getWidth(), view.getHeight());
              outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
              return;
            }
          });
      setClipToOutline(true);
      setClipChildren(true);
    } else {
      super.setOutlineProvider(ViewOutlineProvider.BOUNDS);
    }

    setWillNotDraw(false);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (VERSION.SDK_INT <= VERSION_CODES.M) {
      // Set a background so that shadows work on M-
      int radius = getRadius(w, h);
      float[] radii =
          RoundedCornerViewHelper.createRoundedCornerMask(
              radius, roundedCorners.getBitmask(), isRtLSupplier.get());
      ShapeDrawable backgroundForShadow = new ShapeDrawable(new RoundRectShape(radii, null, null));
      backgroundForShadow.getPaint().setColor(Color.TRANSPARENT);
      setBackground(backgroundForShadow);
    }
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
