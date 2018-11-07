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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.ui.BorderDrawable;
import com.google.android.libraries.feed.piet.ui.RoundedCornerViewHelper;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.Borders;
import com.google.search.now.ui.piet.StylesProto.Borders.Edges;
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

  // For borders with no rounded corners.
  private static final float[] ZERO_RADIUS = new float[] {0, 0, 0, 0, 0, 0, 0, 0};

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

  /** The pre-load fill for this style */
  public Fill getPreLoadFill() {
    return style.getImageLoadingSettings().getPreLoadFill();
  }

  /** Whether the style has a pre-load fill */
  public boolean hasPreLoadFill() {
    return style.getImageLoadingSettings().hasPreLoadFill();
  }

  /** Whether to fade in the image after it's loaded */
  public boolean getFadeInImageOnLoad() {
    return style.getImageLoadingSettings().getFadeInImageOnLoad();
  }

  /** Image scale type on this style */
  public Image.ScaleType getScaleType() {
    return style.getScaleType();
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

  /** Whether this style has borders */
  public boolean hasBorders() {
    return style.getBorders().getWidth() > 0;
  }

  /** This style's borders */
  public Borders getBorders() {
    return style.getBorders();
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
  void applyElementStyles(ElementAdapter<?, ?> adapter) {
    Context context = adapter.getContext();
    View view = adapter.getView();
    View baseView = adapter.getBaseView();

    // Apply layout styles
    EdgeWidths padding = getPadding();

    // Adds padding to the top and bottom of text views to match the behavior of CSS when line
    // spacing is not 1.0.  This value can be negative if line spacing is less than 1.0.
    int extraPaddingForLineHeight = 0;
    if (adapter instanceof TextElementAdapter && getFont().getLineHeightRatio() != 1.0f) {
      float textSize = ((TextElementAdapter) adapter).getTextHeightPx();
      float extraLineHeight = (getFont().getLineHeightRatio() - 1.0f) / 2;
      extraPaddingForLineHeight = (int) (textSize * extraLineHeight);
    }

    applyPadding(context, baseView, padding, extraPaddingForLineHeight);

    if (getMinHeight() > 0) {
      baseView.setMinimumHeight((int) LayoutUtils.dpToPx(getMinHeight(), context));
    } else {
      baseView.setMinimumHeight(0);
    }

    // Apply appearance styles
    view.setBackground(createBackground());
    if (style.getShadow().hasElevationShadow()) {
      ViewCompat.setElevation(view, style.getShadow().getElevationShadow().getElevation());
    } else {
      ViewCompat.setElevation(view, 0.0f);
    }

    baseView.setAlpha(style.getOpacity());
  }

  /** Set the padding on a view. This includes adding extra padding for line spacing on TextView. */
  void applyPadding(
      Context context, View view, EdgeWidths padding, int extraTopAndBottomPaddingPx) {
    int startPadding =
        (int) LayoutUtils.dpToPx(padding.getStart() + getBorderWidth(Edges.START), context);
    int endPadding =
        (int) LayoutUtils.dpToPx(padding.getEnd() + getBorderWidth(Edges.END), context);
    int topPadding =
        (int) LayoutUtils.dpToPx(padding.getTop() + getBorderWidth(Edges.TOP), context)
            + extraTopAndBottomPaddingPx;
    int bottomPadding =
        (int) LayoutUtils.dpToPx(padding.getBottom() + getBorderWidth(Edges.BOTTOM), context);
    // In API version 21 (Lollipop), the implementation of lineSpacingMultiplier() changed to add no
    // extra space beneath a block of text. Before API 21, we need to subtract the extra padding (so
    // that only half the padding is on the bottom). After API 21, extra padding needs to be added.
    if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
      bottomPadding -= extraTopAndBottomPaddingPx;
    } else {
      bottomPadding += extraTopAndBottomPaddingPx;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      view.setPaddingRelative(startPadding, topPadding, endPadding, bottomPadding);
    } else {
      view.setPadding(
          LayoutUtils.isDefaultLocaleRtl() ? endPadding : startPadding,
          topPadding,
          LayoutUtils.isDefaultLocaleRtl() ? startPadding : endPadding,
          bottomPadding);
    }
  }

  int getBorderWidth(Borders.Edges edge) {
    if (!hasBorders()) {
      return 0;
    }
    if (getBorders().getBitmask() == 0 || ((getBorders().getBitmask() & edge.getNumber()) != 0)) {
      return getBorders().getWidth();
    }
    return 0;
  }

  void addBorders(FrameLayout view, Context context) {
    if (!hasBorders()) {
      return;
    }

    // Set up outline of borders
    float[] outerRadii;
    if (hasRoundedCorners()) {
      outerRadii =
          RoundedCornerViewHelper.createRoundedCornerMask(
              getRoundedCornerRadius(context), getRoundedCorners().getBitmask());
    } else {
      outerRadii = ZERO_RADIUS;
    }

    // Create a drawable to stroke the border
    BorderDrawable borderDrawable = new BorderDrawable(context, getBorders(), outerRadii);
    view.setForeground(borderDrawable);
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
   */
  /*@Nullable*/
  Drawable createBackground() {
    return createBackgroundForFill(getBackground());
  }

  /**
   * Return a {@link Drawable} with the fill and rounded corners defined on the style; returns
   * {@code null} if the pre load fill has no color defined.
   */
  /*@Nullable*/
  Drawable createPreLoadFill() {
    return createBackgroundForFill(getPreLoadFill());
  }

  /**
   * Return a {@link Drawable} with a given Fill and the rounded corners defined on the style;
   * returns {@code null} if the background has no color defined.
   */
  /*@Nullable*/
  private Drawable createBackgroundForFill(Fill background) {
    if (!background.hasColor()) {
      return null;
    }
    return new ColorDrawable(background.getColor());
  }

  @Override
  public boolean equals(/*@Nullable*/ Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StyleProvider)) {
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
