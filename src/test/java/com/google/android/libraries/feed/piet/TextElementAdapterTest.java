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

import static com.google.android.libraries.feed.piet.StyleProviderImpl.DEFAULT_STYLE_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static com.google.search.now.ui.piet.ElementsProto.BindingValue.Visibility.GONE;
import static com.google.search.now.ui.piet.ElementsProto.BindingValue.Visibility.INVISIBLE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ParameterizedTextEvaluator;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.BindingRefsProto.ParameterizedTextBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Font.FontWeight;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import org.robolectric.RobolectricTestRunner;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link TextElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class TextElementAdapterTest {
  private static final String TEXT_LINE_CONTENT = "Content";
  private static final String BINDING = "binding";

  @Mock private FrameContext frameContext;
  @Mock private StyleProvider mockStyleProvider;

  private AdapterParameters adapterParameters;

  private Context context;

  private TextElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    adapterParameters =
        new AdapterParameters(null, null, null, new ParameterizedTextEvaluator(), null);

    adapter = new TextElementAdapter.KeySupplier().getAdapter(context, adapterParameters);
  }

  @Test
  public void testCreate() {
    assertThat(adapter).isNotNull();
  }

  @Test
  public void testBindModel_basic() {
    TextElement textElement = getBaseTextElement();
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getText()).isEqualTo(TEXT_LINE_CONTENT);
  }

  @Test
  public void testBindModel_withBinding_someText() {
    ParameterizedText parameterizedText =
        ParameterizedText.newBuilder().setText(TEXT_LINE_CONTENT).build();
    BindingValue bindingValue =
        BindingValue.newBuilder().setParameterizedText(parameterizedText).build();
    when(frameContext.getTextBindingValue(any(ParameterizedTextBindingRef.class)))
        .thenReturn(bindingValue);

    TextElement textElement = getBindingTextElement(null);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getText()).isEqualTo(TEXT_LINE_CONTENT);
  }

  @Test
  public void testBindModel_withBinding_noBinding() {
    when(frameContext.getTextBindingValue(any(ParameterizedTextBindingRef.class))).thenReturn(null);

    TextElement textElement = getBindingTextElement(null);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getText()).isEqualTo("");
  }

  @Test
  public void testBindModel_withBinding_visibilityGone() {
    BindingValue bindingValue = BindingValue.newBuilder().setVisibility(GONE).build();
    when(frameContext.getTextBindingValue(any(ParameterizedTextBindingRef.class)))
        .thenReturn(bindingValue);

    TextElement textElement = getBindingTextElement(null /* StyleProvider*/);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void testBindModel_withBinding_visibilityInvisible() {
    BindingValue bindingValue = BindingValue.newBuilder().setVisibility(INVISIBLE).build();
    when(frameContext.getTextBindingValue(any(ParameterizedTextBindingRef.class)))
        .thenReturn(bindingValue);

    TextElement textElement = getBindingTextElement(null /* StyleProvider*/);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testStyles_padding() {

    TextElement textElement =
        TextElement.newBuilder().setStyleReferences(StyleIdsStack.getDefaultInstance()).build();

    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(mockStyleProvider);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean()))
        .thenReturn(mockStyleProvider);
    when(frameContext.getCurrentStyle()).thenReturn(mockStyleProvider);

    when(mockStyleProvider.getFont()).thenReturn(Font.getDefaultInstance());

    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    verify(mockStyleProvider).setElementStyles(context, frameContext, adapter.getView());
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
  }

  @Test
  public void testStyles_unbindView() {
    int maxLines = 2;
    Style style =
        Style.newBuilder()
            .setMaxLines(maxLines)
            .build();
    StyleProvider styleProvider = new StyleProviderImpl(style);
    TextElement textElement = getBaseTextElement(styleProvider);
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    TextView textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getMaxLines()).isEqualTo(maxLines);

    // Max sure that no styles overrides are still set.
    adapter.unbindModel();
    textElement = getBaseTextElement();
    adapter.bindModel(textElement, frameContext);
    assertThat(adapter.getView()).isNotNull();
    textView = adapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textView.getMaxLines()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void testCreateTextView_default() {
    Font font = Font.getDefaultInstance();
    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean()))
        .thenReturn(mockStyleProvider);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(mockStyleProvider);
    when(mockStyleProvider.getFont()).thenReturn(font);
    TextElement textElement = TextElement.getDefaultInstance();

    TextView textView = adapter.createTextView(textElement, font);
    assertThat(textView).isNotNull();
  }

  @Test
  public void testCreateTextView_boldFont() {
    Font.Builder font = Font.newBuilder();
    font.setWeight(FontWeight.BOLD);

    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean()))
        .thenReturn(mockStyleProvider);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(mockStyleProvider);
    when(mockStyleProvider.getFont()).thenReturn(font.build());

    TextElement textElement = TextElement.getDefaultInstance();
    TextView textView = adapter.createTextView(textElement, font.build());
    Typeface typeface = textView.getTypeface();
    // Typeface.isBold and Typeface.isItalic don't work properly in roboelectric.
    assertThat(typeface.getStyle() & Typeface.BOLD).isGreaterThan(0);
    assertThat(typeface.getStyle() & Typeface.ITALIC).isEqualTo(0);
  }

  @Test
  public void testCreateTextView_italics() {
    Font.Builder font = Font.newBuilder();
    font.setItalic(true);

    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean()))
        .thenReturn(mockStyleProvider);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(mockStyleProvider);
    when(mockStyleProvider.getFont()).thenReturn(font.build());

    TextElement textElement = TextElement.getDefaultInstance();
    TextView textView = adapter.createTextView(textElement, font.build());
    Typeface typeface = textView.getTypeface();
    // Typeface.isBold and Typeface.isItalic don't work properly in roboelectric.
    assertThat(typeface.getStyle() & Typeface.BOLD).isEqualTo(0);
    assertThat(typeface.getStyle() & Typeface.ITALIC).isGreaterThan(0);
  }

  private TextElement getBindingTextElement(@Nullable StyleProvider styleProvider) {
    StyleProvider sp = styleProvider != null ? styleProvider : DEFAULT_STYLE_PROVIDER;
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(sp);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean())).thenReturn(sp);
    when(frameContext.getCurrentStyle()).thenReturn(sp);
    return TextElement.newBuilder()
        .setParameterizedTextBinding(
            ParameterizedTextBindingRef.newBuilder().setBindingId(BINDING).build())
        .build();
  }

  private TextElement getBaseTextElement() {
    return getBaseTextElement(null);
  }

  private TextElement getBaseTextElement(@Nullable StyleProvider styleProvider) {
    StyleProvider sp = styleProvider != null ? styleProvider : DEFAULT_STYLE_PROVIDER;
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(sp);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean())).thenReturn(sp);
    when(frameContext.getCurrentStyle()).thenReturn(sp);

    TextElement.Builder textElement = TextElement.newBuilder();
    ParameterizedText text = ParameterizedText.newBuilder().setText(TEXT_LINE_CONTENT).build();
    textElement.setParameterizedText(text);
    return textElement.build();
  }
}
