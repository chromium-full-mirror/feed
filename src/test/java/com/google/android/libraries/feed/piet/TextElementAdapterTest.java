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

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.DebugLogger.MessageType;
import com.google.android.libraries.feed.piet.TextElementAdapter.TextElementKey;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.TypefaceProvider.GoogleSansTypeface;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.ErrorsProto.ErrorCode;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.StylesProto.TextAlignmentHorizontal;
import com.google.search.now.ui.piet.StylesProto.TextAlignmentVertical;
import com.google.search.now.ui.piet.StylesProto.Typeface.CommonTypeface;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests of the {@link TextElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class TextElementAdapterTest {
  @Mock private FrameContext frameContext;
  @Mock private StyleProvider mockStyleProvider;
  @Mock private HostProviders mockHostProviders;
  @Mock private AssetProvider mockAssetProvider;

  private AdapterParameters adapterParameters;

  private Context context;

  private TextElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();

    adapterParameters =
        new AdapterParameters(
            null,
            null,
            mockHostProviders,
            new ParameterizedTextEvaluator(new FakeClock()),
            null,
            null,
            new FakeClock());
    when(mockHostProviders.getAssetProvider()).thenReturn(mockAssetProvider);
    when(mockStyleProvider.getRoundedCorners()).thenReturn(RoundedCorners.getDefaultInstance());
    when(mockStyleProvider.getTextAlignment()).thenReturn(Gravity.START | Gravity.TOP);

    adapter = new TestTextElementAdapter(context, adapterParameters);
  }

  @Test
  public void testCreateAdapter_setsStyles() {
    Element textElement = getBaseTextElement(mockStyleProvider);
    int color = Color.RED;
    int maxLines = 72;
    when(mockStyleProvider.getFont()).thenReturn(Font.getDefaultInstance());
    when(mockStyleProvider.getColor()).thenReturn(color);
    when(mockStyleProvider.getMaxLines()).thenReturn(maxLines);

    adapter.createAdapter(textElement, frameContext);

    verify(mockStyleProvider).applyElementStyles(adapter);
    assertThat(adapter.getBaseView().getMaxLines()).isEqualTo(maxLines);
    assertThat(adapter.getBaseView().getEllipsize()).isEqualTo(TruncateAt.END);
    assertThat(adapter.getBaseView().getCurrentTextColor()).isEqualTo(color);
  }

  @Test
  public void testSetFont_usesCommonFont() {
    Font font =
        Font.newBuilder()
            .addTypeface(
                StylesProto.Typeface.newBuilder()
                    .setCommonTypeface(CommonTypeface.PLATFORM_DEFAULT_MEDIUM))
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("notused"))
            .build();

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);

    verify(mockAssetProvider, never()).getTypeface(any(), anyBoolean());
  }

  @Test
  public void testSetFont_callsHost() {
    Font font =
        Font.newBuilder()
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("goodfont"))
            .build();
    Typeface hostTypeface = Typeface.create("host", Typeface.BOLD_ITALIC);
    when(mockAssetProvider.getTypeface("goodfont", false)).thenReturn(hostTypeface);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();

    assertThat(typeface).isEqualTo(hostTypeface);
  }

  @Test
  public void testSetFont_callsHostWithItalic() {
    Font font =
        Font.newBuilder()
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("goodfont"))
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("badfont"))
            .setItalic(true)
            .build();
    Typeface hostTypeface = Typeface.create("host", Typeface.BOLD_ITALIC);
    when(mockAssetProvider.getTypeface("goodfont", true)).thenReturn(hostTypeface);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();

    assertThat(typeface).isEqualTo(hostTypeface);
  }

  @Test
  public void testSetFont_callsHostWithFallback() {
    Font font =
        Font.newBuilder()
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("badfont"))
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("goodfont"))
            .build();
    Typeface hostTypeface = Typeface.create("host", Typeface.BOLD_ITALIC);
    when(mockAssetProvider.getTypeface("goodfont", false)).thenReturn(hostTypeface);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();

    assertThat(typeface).isEqualTo(hostTypeface);
  }

  @Test
  public void testSetFont_hostReturnsNull() {
    Font font =
        Font.newBuilder()
            .addTypeface(StylesProto.Typeface.newBuilder().setCustomTypeface("notvalid"))
            .build();
    when(mockAssetProvider.getTypeface(anyString(), anyBoolean())).thenReturn(null);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();

    assertThat(typeface).isEqualTo(new TextView(context).getTypeface());
  }

  @Test
  public void testSetFont_callsHostForGoogleSans() {
    Font font =
        Font.newBuilder()
            .addTypeface(
                StylesProto.Typeface.newBuilder()
                    .setCommonTypeface(CommonTypeface.GOOGLE_SANS_MEDIUM))
            .build();
    Typeface hostTypeface = Typeface.create("fakegooglesans", Typeface.BOLD_ITALIC);
    when(mockAssetProvider.getTypeface(GoogleSansTypeface.GOOGLE_SANS_MEDIUM, false))
        .thenReturn(hostTypeface);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();

    assertThat(typeface).isEqualTo(hostTypeface);
  }

  @Test
  public void testSetFont_reportsFailureWithGoogleSans() {
    Font font =
        Font.newBuilder()
            .addTypeface(
                StylesProto.Typeface.newBuilder()
                    .setCommonTypeface(CommonTypeface.GOOGLE_SANS_MEDIUM))
            .build();
    when(mockAssetProvider.getTypeface(GoogleSansTypeface.GOOGLE_SANS_MEDIUM, false))
        .thenReturn(null);

    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);

    verify(frameContext)
        .reportMessage(
            MessageType.ERROR, ErrorCode.ERR_MISSING_FONTS, "Could not load Google Sans");
  }

  @Test
  public void testSetFont_italics() {
    Font font = Font.newBuilder().setItalic(true).build();
    TextElementKey key = new TextElementKey(font);

    adapter.setValuesUsedInRecyclerKey(key, frameContext);
    Typeface typeface = adapter.getBaseView().getTypeface();
    // Typeface.isBold and Typeface.isItalic don't work properly in roboelectric.
    assertThat(typeface.getStyle() & Typeface.BOLD).isEqualTo(0);
    assertThat(typeface.getStyle() & Typeface.ITALIC).isGreaterThan(0);
  }

  @Test
  public void testGoogleSansEnumToStringDef() {
    assertThat(TextElementAdapter.googleSansEnumToStringDef(CommonTypeface.GOOGLE_SANS_REGULAR))
        .isEqualTo(GoogleSansTypeface.GOOGLE_SANS_REGULAR);
    assertThat(TextElementAdapter.googleSansEnumToStringDef(CommonTypeface.GOOGLE_SANS_MEDIUM))
        .isEqualTo(GoogleSansTypeface.GOOGLE_SANS_MEDIUM);
    assertThat(TextElementAdapter.googleSansEnumToStringDef(CommonTypeface.PLATFORM_DEFAULT_MEDIUM))
        .isEqualTo(GoogleSansTypeface.UNDEFINED);
  }

  // TODO Remove this test once transition to line height is complete.
  @Test
  public void testSetLineHeightRatio() {
    float ratioToSet1 = 4.5f;
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(ratioToSet1)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element firstTextElement = getBaseTextElement(styleProvider1);

    adapter.createAdapter(firstTextElement, frameContext);
    float lineHeightRatio1 = adapter.getBaseView().getLineSpacingMultiplier();
    assertThat(lineHeightRatio1).isEqualTo(ratioToSet1);

    adapter.releaseAdapter();

    // test that the line height ratio is updated when the adapter is created a second time
    float ratioToSet2 = 2.1f;
    Style lineHeightStyle2 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(ratioToSet2)).build();
    StyleProvider styleProvider2 = new StyleProvider(lineHeightStyle2, mockAssetProvider);
    Element secondTextElement = getBaseTextElement(styleProvider2);

    adapter.createAdapter(secondTextElement, frameContext);
    float lineHeightRatio2 = adapter.getBaseView().getLineSpacingMultiplier();
    assertThat(lineHeightRatio2).isEqualTo(ratioToSet2);
  }

  @Test
  public void testSetLineHeight() {
    int lineHeightToSetSp = 18;
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeight(lineHeightToSetSp)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);

    adapter.createAdapter(textElement, frameContext);
    TextView textView = adapter.getBaseView();
    float actualLineHeightPx = textView.getLineHeight();
    int actualLineHeightSp = (int) LayoutUtils.pxToSp(actualLineHeightPx, textView.getContext());
    assertThat(actualLineHeightSp).isEqualTo(lineHeightToSetSp);
  }

  @Test
  public void testGetExtraLineHeight_roundDown() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40.2 - 0 = 40.2. This gets rounded down between the lines (to 40) and rounded up
    // for top and bottom padding (for 21 + 20 = 41).
    initializeAdapterWithLineHeightPx(40.2f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(40);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(21);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testGetExtraLineHeight_roundDown_kitkat() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40.2 - 0 = 40.2. In KitKat and lower, 40 pixels (the amount added between lines)
    // will have already been added to the bottom. To get to our desired value of 21 bottom pixels,
    // the actual bottom padding must be -19 (40 - 19 = 21).
    initializeAdapterWithLineHeightPx(40.2f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(40);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(-19);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  @Test
  public void testGetExtraLineHeight_noRound() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40 - 0 = 40. 40 pixels will be added between each line, and that amount is split
    // (20 and 20) to be added to the top and bottom of the text element.
    initializeAdapterWithLineHeightPx(40.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(40);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(20);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testGetExtraLineHeight_noRound_kitkat() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40 - 0 = 40. In KitKat and lower, 40 pixels (the amount added between lines) will
    // have already been added to the bottom. To get to our desired value of 20 bottom pixels, the
    // actual bottom padding must be -20 (40 - 20 = 20).
    initializeAdapterWithLineHeightPx(40.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(40);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(-20);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  @Test
  public void testGetExtraLineHeight_roundUp() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40.8 - 0 = 40.8. This gets rounded up between the lines (to 41) and rounded down
    // for top and bottom padding (for 20 + 20 = 40).
    initializeAdapterWithLineHeightPx(40.8f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(41);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(20);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testGetExtraLineHeight_roundUp_kitkat() {
    // This text element doesn't actually have text, so the initial height is 0, meaning the extra
    // height is 40.8 - 0 = 40.8. In KitKat and lower, 41 pixels (the amount added between lines)
    // will have already been added to the bottom. To get to our desired value of 20 bottom pixels,
    // the actual bottom padding must be -21 (41 - 21 = 20).
    initializeAdapterWithLineHeightPx(40.8f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(41);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(-21);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(20);
  }

  private void initializeAdapterWithLineHeightPx(float lineHeightPx) {
    // Line height is specified in sp, so line height px = scaledDensity  x line height sp
    int lineHeightSp = 10;
    context.getResources().getDisplayMetrics().scaledDensity = lineHeightPx / lineHeightSp;
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeight(lineHeightSp)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);
  }

  // TODO Remove this test once transition to line height is complete.
  @Test
  public void testGetExtraLineHeight_lineHeightAndLineHeightRatioSet() {
    // Even with line height ratio set, the code should use the line height code path, which means
    // the line height should be line height (10) x scaledDensity (1.01) = 10.1.
    context.getResources().getDisplayMetrics().scaledDensity = 1.01f;
    Style lineHeightStyle1 =
        Style.newBuilder()
            .setFont(Font.newBuilder().setLineHeight(10).setLineHeightRatio(2.0f))
            .build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.betweenLinesExtraPx()).isEqualTo(10);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(6);
    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(5);
  }

  // TODO Remove all these line height ratio tests when ratio is no longer used
  @Test
  public void testGetLineHeight_lineHeightRatio_currentVersion() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom.
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(1.5f)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);
    adapter.getBaseView().setTextSize(100.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(25);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(25);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testGetLineHeight_lineHeightRatio_kitkat() {
    // The line spacing is 150%. The extra 50% gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is an extra 25 at top and bottom. In KitKat
    // and lower, extra line spacing equal to that extra 50% is already added to the bottom. That
    // means padding needs to be taken away from the bottom, to make bottom padding the same as top.
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(1.5f)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);
    adapter.getBaseView().setTextSize(100.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(25);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(-25);
  }

  @Test
  public void testGetLineHeight_lineHeightRatio_lessThanOne() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, we take away an extra 25 at top and bottom, making
    // top and bottom padding negative.
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(0.5f)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);
    adapter.getBaseView().setTextSize(100.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(-25);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(-25);
  }

  @Config(sdk = VERSION_CODES.KITKAT)
  @Test
  public void testGetLineHeight_lineHeightRatio_lessThanOne_kitkat() {
    // The line spacing is 50%. The 50% deficit gets split between padding at the top and bottom of
    // the view, so since the text size is 100, there is a negative 25 at top and bottom. In KitKat
    // and lower, line spacing equal to that 50% deficit is already taken away from the bottom. That
    // means padding needs to be added to the bottom, to make bottom padding the same as top.
    Style lineHeightStyle1 =
        Style.newBuilder().setFont(Font.newBuilder().setLineHeightRatio(0.5f)).build();
    StyleProvider styleProvider1 = new StyleProvider(lineHeightStyle1, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider1);
    adapter.createAdapter(textElement, frameContext);
    adapter.getBaseView().setTextSize(100.0f);

    TextElementAdapter.ExtraLineHeight extraLineHeight = adapter.getExtraLineHeight();

    assertThat(extraLineHeight.topPaddingPx()).isEqualTo(-25);
    assertThat(extraLineHeight.bottomPaddingPx()).isEqualTo(25);
  }

  @Test
  public void testBind_setsTextAlignment_horizontal() {
    Style style =
        Style.newBuilder()
            .setTextAlignmentHorizontal(TextAlignmentHorizontal.TEXT_ALIGNMENT_CENTER)
            .build();
    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);

    assertThat(adapter.getBaseView().getGravity())
        .isEqualTo(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
  }

  @Test
  public void testBind_setsTextAlignment_vertical() {
    Style style =
        Style.newBuilder()
            .setTextAlignmentVertical(TextAlignmentVertical.TEXT_ALIGNMENT_BOTTOM)
            .build();
    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);

    assertThat(adapter.getBaseView().getGravity()).isEqualTo(Gravity.START | Gravity.BOTTOM);
  }

  @Test
  public void testBind_setsTextAlignment_both() {
    Style style =
        Style.newBuilder()
            .setTextAlignmentHorizontal(TextAlignmentHorizontal.TEXT_ALIGNMENT_END)
            .setTextAlignmentVertical(TextAlignmentVertical.TEXT_ALIGNMENT_MIDDLE)
            .build();
    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);

    assertThat(adapter.getBaseView().getGravity()).isEqualTo(Gravity.END | Gravity.CENTER_VERTICAL);
  }

  @Test
  public void testBind_setsTextAlignment_default() {
    Style style = Style.getDefaultInstance();
    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider);
    adapter.getBaseView().setGravity(Gravity.BOTTOM | Gravity.RIGHT);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);

    assertThat(adapter.getBaseView().getGravity()).isEqualTo(Gravity.START | Gravity.TOP);
  }

  @Test
  public void testBind_setsStylesOnlyIfBindingIsDefined() {
    int maxLines = 2;
    Style style = Style.newBuilder().setMaxLines(maxLines).build();
    StyleProvider styleProvider = new StyleProvider(style, mockAssetProvider);
    Element textElement = getBaseTextElement(styleProvider);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getBaseView().getMaxLines()).isEqualTo(maxLines);

    // Styles should not change on a re-bind
    adapter.unbindModel();
    StyleIdsStack otherStyle = StyleIdsStack.newBuilder().addStyleIds("ignored").build();
    textElement =
        getBaseTextElement()
            .toBuilder()
            .setTextElement(TextElement.newBuilder().setStyleReferences(otherStyle))
            .build();
    adapter.bindModel(textElement, frameContext);

    assertThat(adapter.getBaseView().getMaxLines()).isEqualTo(maxLines);
    verify(frameContext, never()).makeStyleFor(otherStyle);

    // Styles only change if new model has style bindings
    adapter.unbindModel();
    StyleIdsStack otherStyleWithBinding =
        StyleIdsStack.newBuilder()
            .setStyleBinding(StyleBindingRef.newBuilder().setBindingId("prionailurus"))
            .build();
    textElement =
        getBaseTextElement()
            .toBuilder()
            .setTextElement(TextElement.newBuilder().setStyleReferences(otherStyleWithBinding))
            .build();
    adapter.bindModel(textElement, frameContext);

    verify(frameContext).makeStyleFor(otherStyleWithBinding);
  }

  @Test
  public void testUnbind() {
    Element textElement = getBaseTextElement(null);
    adapter.createAdapter(textElement, frameContext);
    adapter.bindModel(textElement, frameContext);

    TextView adapterView = adapter.getBaseView();
    adapterView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    adapterView.setText("OLD TEXT");

    adapter.unbindModel();

    assertThat(adapter.getBaseView()).isSameAs(adapterView);
    assertThat(adapterView.getTextAlignment()).isEqualTo(View.TEXT_ALIGNMENT_GRAVITY);
    assertThat(adapterView.getText().toString()).isEmpty();
  }

  @Test
  public void testGetStyles() {
    StyleIdsStack elementStyles = StyleIdsStack.newBuilder().addStyleIds("hair").build();
    when(mockStyleProvider.getFont()).thenReturn(Font.getDefaultInstance());
    Element textElement =
        getBaseTextElement(mockStyleProvider)
            .toBuilder()
            .setTextElement(TextElement.newBuilder().setStyleReferences(elementStyles))
            .build();

    adapter.createAdapter(textElement, frameContext);

    assertThat(adapter.getElementStyleIdsStack()).isSameAs(elementStyles);
  }

  @Test
  public void testGetModelFromElement() {
    TextElement model =
        TextElement.newBuilder()
            .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds("spacer"))
            .build();

    Element elementWithModel = Element.newBuilder().setTextElement(model).build();
    assertThat(adapter.getModelFromElement(elementWithModel)).isSameAs(model);

    Element elementWithWrongModel =
        Element.newBuilder().setCustomElement(CustomElement.getDefaultInstance()).build();
    assertThatRunnable(() -> adapter.getModelFromElement(elementWithWrongModel))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing TextElement");

    Element emptyElement = Element.getDefaultInstance();
    assertThatRunnable(() -> adapter.getModelFromElement(emptyElement))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing TextElement");
  }

  private Element getBaseTextElement() {
    return getBaseTextElement(null);
  }

  private Element getBaseTextElement(/*@Nullable*/ StyleProvider styleProvider) {
    StyleProvider sp =
        styleProvider != null ? styleProvider : adapterParameters.defaultStyleProvider;
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(sp);

    return Element.newBuilder().setTextElement(TextElement.getDefaultInstance()).build();
  }

  private static class TestTextElementAdapter extends TextElementAdapter {
    TestTextElementAdapter(Context context, AdapterParameters parameters) {
      super(context, parameters);
    }

    @Override
    void setTextOnView(FrameContext frameContext, TextElement textElement) {
      return;
    }

    @Override
    TextElementKey createKey(Font font) {
      return new TextElementKey(font);
    }
  }
}
