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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.ui.BorderDrawable;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;
import com.google.search.now.ui.piet.ShadowsProto.ElevationShadow;
import com.google.search.now.ui.piet.ShadowsProto.Shadow;
import com.google.search.now.ui.piet.StylesProto.Borders;
import com.google.search.now.ui.piet.StylesProto.Borders.Edges;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.ImageLoadingSettings;
import com.google.search.now.ui.piet.StylesProto.Style;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** TODO: Test remaining methods of StyleProvider. */
@RunWith(RobolectricTestRunner.class)
public class StyleProviderTest {

  @Mock private AssetProvider mockAssetProvider;
  @Mock private ElementAdapter<View, ?> adapter;
  @Mock private TextElementAdapter textAdapter;

  private View view;
  private View baseView;
  private TextView textView;

  private Context context;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    view = new View(context);
    baseView = new View(context);
    textView = new TextView(context);
    when(adapter.getView()).thenReturn(view);
    when(adapter.getBaseView()).thenReturn(baseView);
    when(adapter.getContext()).thenReturn(context);
    when(textAdapter.getView()).thenReturn(view);
    when(textAdapter.getBaseView()).thenReturn(textView);
    when(textAdapter.getContext()).thenReturn(context);
  }

  @Test
  public void testGetters_withStyleDefined() {
    Style style =
        Style.newBuilder()
            .setColor(1)
            .setBackground(Fill.newBuilder().setColor(2))
            .setImageLoadingSettings(
                ImageLoadingSettings.newBuilder()
                    .setPreLoadFill(Fill.newBuilder().setColor(9))
                    .setFadeInImageOnLoad(true)
                    .build())
            .setRoundedCorners(RoundedCorners.newBuilder().setRadius(3))
            .setFont(Font.newBuilder().setSize(4))
            .setMaxLines(5)
            .setMinHeight(6)
            .setHeight(7)
            .setWidth(8)
            .build();

    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);

    assertThat(styleProvider.getColor()).isEqualTo(style.getColor());
    assertThat(styleProvider.getBackground()).isEqualTo(style.getBackground());
    assertThat(styleProvider.hasPreLoadFill()).isTrue();
    assertThat(styleProvider.getPreLoadFill())
        .isEqualTo(style.getImageLoadingSettings().getPreLoadFill());
    assertThat(styleProvider.getFadeInImageOnLoad())
        .isEqualTo(style.getImageLoadingSettings().getFadeInImageOnLoad());
    assertThat(styleProvider.getRoundedCorners()).isEqualTo(style.getRoundedCorners());
    assertThat(styleProvider.hasRoundedCorners()).isTrue();
    assertThat(styleProvider.getFont()).isEqualTo(style.getFont());
    assertThat(styleProvider.getMaxLines()).isEqualTo(style.getMaxLines());
    assertThat(styleProvider.getHeight()).isEqualTo(style.getHeight());
    assertThat(styleProvider.getWidth()).isEqualTo(style.getWidth());
    assertThat(styleProvider.hasHeight()).isTrue();
    assertThat(styleProvider.hasWidth()).isTrue();
  }

  @Test
  public void testGetters_withEmptyStyleDefined() {
    Style style = Style.getDefaultInstance();

    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);

    assertThat(styleProvider.getColor()).isEqualTo(style.getColor());
    assertThat(styleProvider.getBackground()).isEqualTo(style.getBackground());
    assertThat(styleProvider.hasPreLoadFill()).isFalse();
    assertThat(styleProvider.getPreLoadFill()).isEqualTo(Fill.getDefaultInstance());
    assertThat(styleProvider.getFadeInImageOnLoad()).isFalse();
    assertThat(styleProvider.hasRoundedCorners()).isFalse();
    assertThat(styleProvider.getRoundedCorners()).isEqualTo(RoundedCorners.getDefaultInstance());
    assertThat(styleProvider.getFont()).isEqualTo(Font.getDefaultInstance());
    assertThat(styleProvider.getMaxLines()).isEqualTo(style.getMaxLines());
    assertThat(styleProvider.getHeight()).isEqualTo(0);
    assertThat(styleProvider.getWidth()).isEqualTo(0);
    assertThat(styleProvider.hasHeight()).isFalse();
    assertThat(styleProvider.hasWidth()).isFalse();
  }

  @Test
  public void testHasBorders_falseIfWidthZero() {
    StyleProvider styleProvider =
        new StyleProvider(
            Style.newBuilder().setBorders(Borders.newBuilder().setBitmask(15)).build(),
            mockAssetProvider);

    assertThat(styleProvider.hasBorders()).isFalse();
  }

  @Test
  public void testApplyPadding() {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    new StyleProvider(Style.getDefaultInstance(), mockAssetProvider)
        .applyPadding(context, view, padding, 0);
    verifyPadding(view, padding);
  }

  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  @Test
  public void testApplyPadding_JB() {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    new StyleProvider(Style.newBuilder().setPadding(padding).build(), mockAssetProvider)
        .applyElementStyles(adapter);
    verifyPadding(baseView, padding);
  }

  @Test
  public void testApplyPadding_lineSpacing() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom.
    testApplyPadding_lineSpacing_LollipopAndUp(
        /* lineSpacing= */ 1.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ 25);
  }

  @Config(sdk = VERSION_CODES.LOLLIPOP)
  @Test
  public void testApplyPadding_lineSpacing_L() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom.
    testApplyPadding_lineSpacing_LollipopAndUp(
        /* lineSpacing= */ 1.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ 25);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testApplyPadding_lineSpacing_KK() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom.
    testApplyPadding_lineSpacing_KKandUnder(
        /* lineSpacing= */ 1.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ 25);
  }

  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  @Test
  public void testApplyPadding_lineSpacing_JB() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom.
    testApplyPadding_lineSpacing_KKandUnder(
        /* lineSpacing= */ 1.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ 25);
  }

  @Test
  public void testApplyPadding_lineSpacingLessThanOne() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, we take away an extra 25 at top and bottom, making
    // topAndBottomPadding negative.
    testApplyPadding_lineSpacing_LollipopAndUp(
        /* lineSpacing= */ 0.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ -25);
  }

  @Config(sdk = VERSION_CODES.LOLLIPOP)
  @Test
  public void testApplyPadding_lineSpacingLessThanOne_L() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, we take away an extra 25 at top and bottom, making
    // topAndBottomPadding negative.
    testApplyPadding_lineSpacing_LollipopAndUp(
        /* lineSpacing= */ 0.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ -25);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testApplyPadding_lineSpacingLessThanOne_KK() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, we take away an extra 25 at top and bottom, making
    // topAndBottomPadding negative.
    testApplyPadding_lineSpacing_KKandUnder(
        /* lineSpacing= */ 0.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ -25);
  }

  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  @Test
  public void testApplyPadding_lineSpacingLessThanOne_JB() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, we take away an extra 25 at top and bottom, making
    // topAndBottomPadding negative.
    testApplyPadding_lineSpacing_KKandUnder(
        /* lineSpacing= */ 0.5f, /* textHeight= */ 100.0f, /* topAndBottomPadding= */ -25);
  }

  private void testApplyPadding_lineSpacing_LollipopAndUp(
      float lineSpacing, float textHeight, int topAndBottomPadding) {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    when(textAdapter.getTextHeightPx()).thenReturn(textHeight);
    new StyleProvider(
            Style.newBuilder()
                .setFont(Font.newBuilder().setLineHeightRatio(lineSpacing))
                .setPadding(padding)
                .build(),
            mockAssetProvider)
        .applyElementStyles(textAdapter);

    assertThat(textView.getPaddingTop())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getTop(), context) + topAndBottomPadding);
    assertThat(textView.getPaddingBottom())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getBottom(), context) + topAndBottomPadding);
  }

  private void testApplyPadding_lineSpacing_KKandUnder(
      float lineSpacing, float textHeight, int topAndBottomPadding) {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    when(textAdapter.getTextHeightPx()).thenReturn(textHeight);
    new StyleProvider(
            Style.newBuilder()
                .setFont(Font.newBuilder().setLineHeightRatio(lineSpacing))
                .setPadding(padding)
                .build(),
            mockAssetProvider)
        .applyElementStyles(textAdapter);

    // In KitKat and lower, extra line spacing equal to twice the topAndBottomPadding is already
    // added to the bottom. That means topAndBottomPadding needs to be taken away from the bottom,
    // to make bottom padding the same as top padding.
    assertThat(textView.getPaddingTop())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getTop(), context) + topAndBottomPadding);
    assertThat(textView.getPaddingBottom())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getBottom(), context) - topAndBottomPadding);
  }

  @Test
  public void testApplyPadding_addsExtraForBorders_allSides() {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    Style bordersStyle = Style.newBuilder().setBorders(Borders.newBuilder().setWidth(5)).build();
    new StyleProvider(bordersStyle, mockAssetProvider).applyPadding(context, view, padding, 0);

    assertThat(view.getPaddingTop()).isEqualTo((int) LayoutUtils.dpToPx(1 + 5, context));
    assertThat(view.getPaddingBottom()).isEqualTo((int) LayoutUtils.dpToPx(2 + 5, context));
    assertThat(view.getPaddingStart()).isEqualTo((int) LayoutUtils.dpToPx(3 + 5, context));
    assertThat(view.getPaddingEnd()).isEqualTo((int) LayoutUtils.dpToPx(4 + 5, context));
  }

  @Test
  public void testApplyPadding_addsExtraForBorders_someSides() {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    Style bordersBottomRight =
        Style.newBuilder()
            .setBorders(
                Borders.newBuilder()
                    .setWidth(5)
                    .setBitmask(Edges.BOTTOM.getNumber() | Edges.END.getNumber()))
            .build();
    new StyleProvider(bordersBottomRight, mockAssetProvider)
        .applyPadding(context, view, padding, 0);

    assertThat(view.getPaddingTop()).isEqualTo((int) LayoutUtils.dpToPx(1, context));
    assertThat(view.getPaddingBottom()).isEqualTo((int) LayoutUtils.dpToPx(2 + 5, context));
    assertThat(view.getPaddingStart()).isEqualTo((int) LayoutUtils.dpToPx(3, context));
    assertThat(view.getPaddingEnd()).isEqualTo((int) LayoutUtils.dpToPx(4 + 5, context));
  }

  @Test
  public void testCreateBorderDrawable() {
    Borders borders =
        Borders.newBuilder()
            .setColor(Color.CYAN)
            .setWidth(4)
            .setBitmask(Edges.TOP.getNumber() | Edges.END.getNumber())
            .build();

    FrameLayout view = new FrameLayout(context);
    new StyleProvider(Style.newBuilder().setBorders(borders).build(), mockAssetProvider)
        .addBorders(view, context);
    BorderDrawable borderDrawable = (BorderDrawable) view.getForeground();
    borderDrawable.setBounds(0, 0, 0, 0);

    assertThat(borderDrawable.getPaint().getColor()).isEqualTo(Color.CYAN);
    assertThat(borderDrawable.getBounds()).isEqualTo(new Rect(-4, 0, 0, 4));
  }

  @Test
  public void testCreateBorderDrawable_roundedCorners() {
    Borders borders =
        Borders.newBuilder()
            .setColor(Color.CYAN)
            .setWidth(4)
            .setBitmask(Edges.TOP.getNumber() | Edges.END.getNumber())
            .build();
    RoundedCorners roundedCorners =
        RoundedCorners.newBuilder()
            .setRadius(16)
            .setBitmask(Corners.TOP_START.getNumber() | Corners.BOTTOM_END.getNumber())
            .build();

    FrameLayout view = new FrameLayout(context);
    new StyleProvider(
            Style.newBuilder().setBorders(borders).setRoundedCorners(roundedCorners).build(),
            mockAssetProvider)
        .addBorders(view, context);
    BorderDrawable borderDrawable = (BorderDrawable) view.getForeground();
    borderDrawable.setBounds(0, 0, 0, 0);

    assertThat(borderDrawable.getPaint().getColor()).isEqualTo(Color.CYAN);
    assertThat(borderDrawable.getBounds()).isEqualTo(new Rect(-4, 0, 0, 4));

    // I don't think there's any great way to test the rounded corners part.
  }

  @Test
  public void testCreateBorderDrawable_noOp() {
    FrameLayout view = new FrameLayout(context);
    new StyleProvider(
            Style.newBuilder().setBorders(Borders.newBuilder().setWidth(0)).build(),
            mockAssetProvider)
        .addBorders(view, context);

    assertThat(view.getForeground()).isNull();
  }

  @Test
  public void testApplyMargins() {
    EdgeWidths margins =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    Style marginStyle = Style.newBuilder().setMargins(margins).build();

    MarginLayoutParams params =
        new MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT);

    new StyleProvider(marginStyle, mockAssetProvider).applyMargins(context, params);
    verifyMargins(params, margins);
  }

  @Test
  public void testElementStyles_padding() {
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(1).setBottom(2).setStart(3).setEnd(4).build();
    Style paddingStyle = Style.newBuilder().setPadding(padding).build();

    new StyleProvider(paddingStyle, mockAssetProvider).applyElementStyles(adapter);
    verifyPadding(baseView, padding);
  }

  @Test
  public void testElementStyles_backgroundAndCorners() {
    int color = 0xffffffff;
    Fill background = Fill.newBuilder().setColor(color).build();
    RoundedCorners corners = RoundedCorners.newBuilder().setRadius(4).setBitmask(3).build();

    Style style =
        Style.newBuilder()
            .setColor(color)
            .setBackground(background)
            .setRoundedCorners(corners)
            .build();

    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    styleProvider.applyElementStyles(adapter);

    ColorDrawable colorDrawable = (ColorDrawable) view.getBackground();
    assertThat(colorDrawable.getColor()).isEqualTo(color);
  }

  @Test
  public void testElementStyles_noBackgroundInStyle() {
    view.setBackground(new ColorDrawable(0xffff0000));

    new StyleProvider(Style.getDefaultInstance(), mockAssetProvider).applyElementStyles(adapter);
    assertThat(view.getBackground()).isNull();
  }

  @Test
  public void testElementStyles_noColorInFill() {
    view.setBackground(new ColorDrawable(0xffff0000));

    Style style = Style.newBuilder().setBackground(Fill.getDefaultInstance()).build();

    new StyleProvider(style, mockAssetProvider).applyElementStyles(adapter);

    assertThat(view.getBackground()).isNull();
  }

  @Test
  public void testElementStyles_minimumHeight() {
    int minHeight = 12345;
    Style heightStyle = Style.newBuilder().setMinHeight(minHeight).build();

    new StyleProvider(heightStyle, mockAssetProvider).applyElementStyles(adapter);

    assertThat(baseView.getMinimumHeight()).isEqualTo(minHeight);
  }

  @Test
  public void testElementStyles_noMinimumHeight() {
    Style noHeightStyle = Style.getDefaultInstance();
    view.setMinimumHeight(12345);

    new StyleProvider(noHeightStyle, mockAssetProvider).applyElementStyles(adapter);

    assertThat(baseView.getMinimumHeight()).isEqualTo(0);
  }

  @Test
  public void testElementStyles_shadow() {
    int elevation = 5280;
    Style shadowStyle =
        Style.newBuilder()
            .setShadow(
                Shadow.newBuilder()
                    .setElevationShadow(ElevationShadow.newBuilder().setElevation(elevation)))
            .build();

    new StyleProvider(shadowStyle, mockAssetProvider).applyElementStyles(adapter);

    assertThat(view.getElevation()).isEqualTo((float) elevation);
  }

  @Test
  public void testElementStyles_noShadow() {
    Style shadowStyle =
        Style.newBuilder()
            .setShadow(
                Shadow.newBuilder()
                    .setElevationShadow(ElevationShadow.newBuilder().setElevation(5280)))
            .build();
    Style noShadowStyle = Style.getDefaultInstance();

    new StyleProvider(shadowStyle, mockAssetProvider).applyElementStyles(adapter);

    new StyleProvider(noShadowStyle, mockAssetProvider).applyElementStyles(adapter);

    assertThat(view.getElevation()).isEqualTo(0.0f);
  }

  @Test
  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  public void testElementStyles_shadowDoesNotError_JB() {
    Style shadowStyle =
        Style.newBuilder()
            .setShadow(
                Shadow.newBuilder()
                    .setElevationShadow(ElevationShadow.newBuilder().setElevation(5280)))
            .build();
    Style noShadowStyle = Style.getDefaultInstance();

    new StyleProvider(shadowStyle, mockAssetProvider).applyElementStyles(adapter);

    new StyleProvider(noShadowStyle, mockAssetProvider).applyElementStyles(adapter);

    // Assert nothing failed.
  }

  @Test
  public void testElementStyles_opacity() {
    new StyleProvider(Style.newBuilder().setOpacity(0.5f).build(), mockAssetProvider)
        .applyElementStyles(adapter);

    assertThat(baseView.getAlpha()).isEqualTo(0.5f);

    new StyleProvider(Style.getDefaultInstance(), mockAssetProvider).applyElementStyles(adapter);

    assertThat(baseView.getAlpha()).isEqualTo(1.0f);
  }

  private void verifyPadding(View view, EdgeWidths padding) {
    assertThat(view.getPaddingTop()).isEqualTo((int) LayoutUtils.dpToPx(padding.getTop(), context));
    assertThat(view.getPaddingBottom())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getBottom(), context));
    assertThat(view.getPaddingStart())
        .isEqualTo((int) LayoutUtils.dpToPx(padding.getStart(), context));
    assertThat(view.getPaddingEnd()).isEqualTo((int) LayoutUtils.dpToPx(padding.getEnd(), context));
  }

  @Test
  public void testCreateBackground() {
    int color = 12345;
    Fill fill = Fill.newBuilder().setColor(color).build();
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadius(4321).build();
    StyleProvider styleProvider;

    styleProvider = new StyleProvider(Style.getDefaultInstance(), mockAssetProvider);
    assertThat(styleProvider.createBackground()).isNull();

    styleProvider =
        new StyleProvider(
            Style.newBuilder().setBackground(fill).setRoundedCorners(roundedCorners).build(),
            mockAssetProvider);
    Drawable backgroundDrawable = styleProvider.createBackground();
    assertThat(backgroundDrawable).isInstanceOf(ColorDrawable.class);

    ColorDrawable background = (ColorDrawable) backgroundDrawable;
    assertThat(background.getColor()).isEqualTo(color);
  }

  @Test
  public void testCreatePreLoadFill() {
    int color = 12345;
    Fill fill = Fill.newBuilder().setColor(color).build();
    RoundedCorners roundedCorners = RoundedCorners.newBuilder().setRadius(4321).build();
    StyleProvider styleProvider;

    styleProvider = new StyleProvider(Style.getDefaultInstance(), mockAssetProvider);
    assertThat(styleProvider.createPreLoadFill()).isNull();

    styleProvider =
        new StyleProvider(
            Style.newBuilder()
                .setImageLoadingSettings(
                    ImageLoadingSettings.newBuilder().setPreLoadFill(fill).build())
                .setRoundedCorners(roundedCorners)
                .build(),
            mockAssetProvider);
    Drawable preLoadFillDrawable = styleProvider.createPreLoadFill();
    assertThat(preLoadFillDrawable).isInstanceOf(ColorDrawable.class);

    ColorDrawable background = (ColorDrawable) preLoadFillDrawable;
    assertThat(background.getColor()).isEqualTo(color);
  }

  @Test
  public void testEqualsAndHashCode() {
    Style style = Style.newBuilder().setColor(123).build();
    Style sameStyle = Style.newBuilder().setColor(123).build();
    Style differentStyle = Style.newBuilder().setHeight(456).build();

    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    StyleProvider sameStyleProvider = new StyleProvider(sameStyle, mockAssetProvider);
    StyleProvider differentStyleProvider = new StyleProvider(differentStyle, mockAssetProvider);

    assertThat(styleProvider).isEqualTo(sameStyleProvider);
    assertThat(styleProvider.hashCode()).isEqualTo(sameStyleProvider.hashCode());
    assertThat(styleProvider).isNotEqualTo(differentStyleProvider);
  }

  private void verifyMargins(MarginLayoutParams params, EdgeWidths margins) {
    assertThat(params.getMarginStart())
        .isEqualTo((int) LayoutUtils.dpToPx(margins.getStart(), context));
    assertThat(params.getMarginEnd())
        .isEqualTo((int) LayoutUtils.dpToPx(margins.getEnd(), context));
    assertThat(params.topMargin).isEqualTo((int) LayoutUtils.dpToPx(margins.getTop(), context));
    assertThat(params.bottomMargin)
        .isEqualTo((int) LayoutUtils.dpToPx(margins.getBottom(), context));
    assertThat(params.leftMargin).isEqualTo((int) LayoutUtils.dpToPx(margins.getStart(), context));
    assertThat(params.rightMargin).isEqualTo((int) LayoutUtils.dpToPx(margins.getEnd(), context));
  }
}
