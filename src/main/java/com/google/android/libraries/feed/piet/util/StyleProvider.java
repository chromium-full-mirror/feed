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

package com.google.android.libraries.feed.piet.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.Font;

/** Provide styles for the slice contents. */
public interface StyleProvider {
  /** Default font or foreground color */
  int getColor();

  /** The font for this style */
  Font getFont();

  /** The max_lines for a TextView */
  int getMaxLines();

  /** The min_height for a view */
  int getMinHeight();

  /** Height of a view in dp */
  int getHeight();

  /** Width of a view in dp */
  int getWidth();

  /** Whether a height is explicitly specified */
  boolean hasHeight();

  /** Whether a width is explicitly specified */
  boolean hasWidth();

  /** This style's margins */
  EdgeWidths getMargins();

  /** This style's padding */
  EdgeWidths getPadding();

  /** This style's background */
  Fill getBackground();

  /** The {@link RoundedCorners} to be used with the background color. */
  RoundedCorners getRoundedCorners();

  /** Whether rounded corners are explicitly specified */
  boolean hasRoundedCorners();

  /** Applies all style information to a view. */
  void setElementStyles(Context context, FrameContext frameContext, View view);

  /** Sets appropriate margins on a {@link MarginLayoutParams} instance. */
  void setMargins(Context context, MarginLayoutParams marginLayoutParams);
}
