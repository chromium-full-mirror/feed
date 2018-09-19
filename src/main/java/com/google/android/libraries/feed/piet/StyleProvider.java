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

package com.google.android.libraries.feed.piet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.ui.RoundedCornerColorDrawable;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Style;

/**
 * Class which understands how to create the current styles.
 *
 * <p>Feel free to add "has..." methods for any of the fields if needed.
 */
class StyleProvider {

  @VisibleForTesting
  static final Style DEFAULT_STYLE =
      Style.newBuilder()
          .setFont(Font.newBuilder().setSize(14))
          .setColor(0x84000000)
          .setPadding(EdgeWidths.getDefaultInstance())
          .setMargins(EdgeWidths.getDefaultInstance())
          .setMaxLines(0)
          .build();

  private final Style style;
  private final AssetProvider assetProvider;

  StyleProvider(Style style, AssetProvider assetProvider) {
    this.style = style;
    this.assetProvider = assetProvider;
  }

  /** Default font or foreground color */
  public int getColor() {
    return style.getColor();
  }

  /** Whether a color is explicitly specified */
  public boolean hasColor() {
    return style.hasColor();
  }

  /** This style's background */
  public Fill getBackground() {
    return style.getBackground();
  }

  /** The {@link RoundedCorners} to be used with the background color. */
  public RoundedCorners getRoundedCorners() {
    return style.getRoundedCorners();
  }

  /** Whether rounded corners are explicitly specified */
  public boolean hasRoundedCorners() {
    return style.hasRoundedCorners();
  }

  /** The font for this style */
  public Font getFont() {
    return style.getFont();
  }

  /** This style's padding */
  public EdgeWidths getPadding() {
    return style.getPadding();
  }

  /** This style's margins */
  public EdgeWidths getMargins() {
    return style.getMargins();
  }

  /** The max_lines for a TextView */
  public int getMaxLines() {
    return style.getMaxLines();
  }

  /** The min_height for a view */
  public int getMinHeight() {
    return style.getMinHeight();
  }

  /** Height of a view in dp */
  public int getHeight() {
    return style.getHeight();
  }

  /** Width of a view in dp */
  public int getWidth() {
    return style.getWidth();
  }

  /** Whether a height is explicitly specified */
  public boolean hasHeight() {
    return style.hasHeight();
  }

  /** Whether a width is explicitly specified */
  public boolean hasWidth() {
    return style.hasWidth();
  }

  /** Sets the Padding and Background Color on the view. */
  public void applyElementStyles(Context context, View view) {
    EdgeWidths padding = getPadding();
    applyPadding(context, view, padding);
    view.setBackground(createBackground(context));

    if (getMinHeight() > 0) {
      view.setMinimumHeight((int) LayoutUtils.dpToPx(getMinHeight(), context));
    } else {
      view.setMinimumHeight(0);
    }
  }

  /** Set the padding on a view */
  void applyPadding(Context context, View view, EdgeWidths padding) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      view.setPaddingRelative(
          (int) LayoutUtils.dpToPx(padding.getStart(), context),
          (int) LayoutUtils.dpToPx(padding.getTop(), context),
          (int) LayoutUtils.dpToPx(padding.getEnd(), context),
          (int) LayoutUtils.dpToPx(padding.getBottom(), context));
    } else {
      view.setPadding(
          (int) LayoutUtils.dpToPx(padding.getStart(), context),
          (int) LayoutUtils.dpToPx(padding.getTop(), context),
          (int) LayoutUtils.dpToPx(padding.getEnd(), context),
          (int) LayoutUtils.dpToPx(padding.getBottom(), context));
    }
  }

  /** Sets appropriate margins on a {@link MarginLayoutParams} instance. */
  public void applyMargins(Context context, MarginLayoutParams marginLayoutParams) {
    EdgeWidths margins = getMargins();
    int startMargin = (int) LayoutUtils.dpToPx(margins.getStart(), context);
    int endMargin = (int) LayoutUtils.dpToPx(margins.getEnd(), context);
    marginLayoutParams.setMargins(
        startMargin,
        (int) LayoutUtils.dpToPx(margins.getTop(), context),
        endMargin,
        (int) LayoutUtils.dpToPx(margins.getBottom(), context));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      marginLayoutParams.setMarginStart(startMargin);
      marginLayoutParams.setMarginEnd(endMargin);
    }
  }

  public int getRoundedCornerRadius(Context context) {
    RoundedCorners roundedCorners = getRoundedCorners();
    if (roundedCorners.hasRadius()) {
      return (int) LayoutUtils.dpToPx(roundedCorners.getRadius(), context);
    } else {
      return assetProvider.getDefaultCornerRadius();
    }
  }

  /**
   * Return a {@link Drawable} with the fill and rounded corners defined on the style; returns
   * {@code null} if the background has no color defined.
   *
   * @param context
   */
  /*@Nullable*/
  Drawable createBackground(Context context) {
    Fill background = getBackground();
    if (!background.hasColor()) {
      return null;
    }
    RoundedCornerColorDrawable bgDrawable = new RoundedCornerColorDrawable(background.getColor());
    if (hasRoundedCorners()) {
      int layoutDirection =
          LayoutUtils.isDefaultLocaleRtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
      bgDrawable.setRoundedCorners(
          getRoundedCornerRadius(context), getRoundedCorners().getBitmask(), layoutDirection);
    }
    return bgDrawable;
  }

  @Override
  public boolean equals(/*@Nullable*/ Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StyleProvider that = (StyleProvider) o;

    return style.equals(that.style) && assetProvider.equals(that.assetProvider);
  }

  @Override
  public int hashCode() {
    return style.hashCode();
  }
}
