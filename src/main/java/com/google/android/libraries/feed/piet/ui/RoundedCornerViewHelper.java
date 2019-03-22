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

import android.view.View;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;
import java.util.Arrays;

/** Helper class to help work with rounded corner views. */
public class RoundedCornerViewHelper {

  public static float[] createRoundedCornerMask(float radius, int cornerField, boolean isRtL) {
    final int layoutDirection = isRtL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
    return createRoundedCornerMask(radius, cornerField, layoutDirection);
  }

  /**
   * Returns a float[] representing the mask for which corners should be rounded with the radius.
   *
   * @param layoutDirection either 0 for {@link android.view.View#LAYOUT_DIRECTION_LTR} or 1 for
   *     {@link android.view.View#LAYOUT_DIRECTION_RTL}. If an invalid value is passed in, default
   *     to LTR.
   */
  private static float[] createRoundedCornerMask(
      float radius, int cornerField, int layoutDirection) {
    float[] radii = new float[8];
    // If we don't have any radius, don't bother creating the mask.
    if (radius > 0) {
      if (cornerField == Corners.CORNERS_UNSPECIFIED_VALUE) {
        Arrays.fill(radii, 0, 8, radius);
        return radii;
      }

      if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        if ((cornerField & Corners.TOP_START_VALUE) != 0) {
          radii[2] = radius;
          radii[3] = radius;
        }
        if ((cornerField & Corners.TOP_END_VALUE) != 0) {
          radii[0] = radius;
          radii[1] = radius;
        }
        if ((cornerField & Corners.BOTTOM_END_VALUE) != 0) {
          radii[6] = radius;
          radii[7] = radius;
        }
        if ((cornerField & Corners.BOTTOM_START_VALUE) != 0) {
          radii[4] = radius;
          radii[5] = radius;
        }
      } else {
        if ((cornerField & Corners.TOP_START_VALUE) != 0) {
          radii[0] = radius;
          radii[1] = radius;
        }
        if ((cornerField & Corners.TOP_END_VALUE) != 0) {
          radii[2] = radius;
          radii[3] = radius;
        }
        if ((cornerField & Corners.BOTTOM_END_VALUE) != 0) {
          radii[4] = radius;
          radii[5] = radius;
        }
        if ((cornerField & Corners.BOTTOM_START_VALUE) != 0) {
          radii[6] = radius;
          radii[7] = radius;
        }
      }
    }

    return radii;
  }

  /**
   * Evaluates whether the rounded corners are valid--meaning one of the radius values is set, and
   * is greater than zero.
   */
  public static boolean hasValidRoundedCorners(RoundedCorners roundedCorners, int radiusOverride) {
    // TODO Remove deprecated radius code.
    return ((roundedCorners.hasRadiusPercentageOfWidth()
            && (roundedCorners.getRadiusPercentageOfWidth() > 0))
        || (roundedCorners.hasRadiusPercentageOfHeight()
            && (roundedCorners.getRadiusPercentageOfHeight() > 0))
        || (roundedCorners.hasRadiusDp() && (roundedCorners.getRadiusDp() > 0))
        || (roundedCorners.hasRadius() && (roundedCorners.getRadius() > 0))
        || (radiusOverride > 0));
  }

  // Prevent instantiation
  private RoundedCornerViewHelper() {}
}
