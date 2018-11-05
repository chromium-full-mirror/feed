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
import android.graphics.Typeface;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.ErrorsProto.ErrorCode;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Font.FontWeight;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/**
 * Base {@link ElementAdapter} to extend to manage {@code ChunkedText} and {@code ParameterizedText}
 * elements.
 */
abstract class TextElementAdapter extends ElementAdapter<TextView, TextElement> {
  private static final String TAG = "TextElementAdapter";

  TextElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, new TextView(context));
  }

  @Override
  protected TextElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasTextElement()) {
      throw new PietFatalException(
          ErrorCode.ERR_MISSING_ELEMENT_CONTENTS,
          String.format("Missing TextElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getTextElement();
  }

  @Override
  void onCreateAdapter(TextElement textLine, Element baseElement, FrameContext frameContext) {
    if (getKey() == null) {
      TextElementKey key = createKey(getElementStyle().getFont());
      setKey(key);
      setValuesUsedInRecyclerKey(key);
    }

    // Setup the layout of the text lines, including all properties not in the recycler key.
    updateTextStyle();
  }

  private void updateTextStyle() {
    TextView textView = getBaseView();
    StyleProvider textStyle = getElementStyle();
    textView.setTextColor(textStyle.getColor());
    textView.setLineSpacing(/* add= */ 0, textStyle.getFont().getLineHeightRatio());
    // Letter spacing is currently untested due to lack of Robolectric support - don't delete this!
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      textView.setLetterSpacing(textStyle.getFont().getLetterSpacing());
    } else {
      textView.setTextScaleX(1.0f + textStyle.getFont().getLetterSpacing());
    }
    if (textStyle.getMaxLines() > 0) {
      textView.setMaxLines(textStyle.getMaxLines());
      textView.setEllipsize(TextUtils.TruncateAt.END);
    } else {
      // MAX_VALUE is the value used in the Android implementation for the default
      textView.setMaxLines(Integer.MAX_VALUE);
    }
    textView.setGravity(
        ViewUtils.pietGravityToGravity(getHorizontalGravity(), getVerticalGravity()));
  }

  @Override
  void onBindModel(TextElement textLine, Element baseElement, FrameContext frameContext) {

    // Set the initial state for the TextView
    // No bindings found, so use the inlined value (or empty if not set)
    setTextOnView(frameContext, textLine);

    if (textLine.getStyleReferences().hasStyleBinding()) {
      updateTextStyle();
    }
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  abstract void setTextOnView(FrameContext frameContext, TextElement textElement);

  @Override
  void onUnbindModel() {
    TextView textView = getBaseView();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
    }
    textView.setText("");
  }

  /** Return the height of the text in this view for line spacing calculations. */
  float getTextHeightPx() {
    return getBaseView().getTextSize();
  }

  @VisibleForTesting
  // LINT.IfChange
  void setValuesUsedInRecyclerKey(TextElementKey fontKey) {
    // TODO: Implement typefaces
    TextView textView = getBaseView();
    textView.setTextSize(fontKey.getSize());
    boolean bold = false;
    switch (fontKey.getFontWeight()) {
      case REGULAR:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_regular);
        break;
      case THIN:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_thin);
        break;
      case LIGHT:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_light);
        break;
      case MEDIUM:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_medium);
        break;
      case BOLD:
        bold = true;
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_regular);
        break;
      case BLACK:
        bold = true;
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_black);
        break;
      default:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_regular);
        break;
    }
    if (bold || fontKey.isItalic()) {
      if (bold && fontKey.isItalic()) {
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
      } else if (bold) {
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
      } else if (fontKey.isItalic()) {
        textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
      }
    } else {
      textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.NORMAL));
    }
  }
  // LINT.ThenChange

  TextElementKey createKey(Font font) {
    return new TextElementKey(font);
  }

  abstract static class TextElementKeySupplier<A extends TextElementAdapter>
      implements AdapterKeySupplier<A, TextElement> {
    @Override
    public TextElementKey getKey(FrameContext frameContext, TextElement model) {
      StyleProvider styleProvider = frameContext.makeStyleFor(model.getStyleReferences());
      return new TextElementKey(styleProvider.getFont());
    }
  }

  /** We will Key TextViews off of the Ellipsizing, Font Size and FontWeight, and Italics. */
  // LINT.IfChange
  static class TextElementKey extends RecyclerKey {
    private final int size;
    private final FontWeight fontWeight;
    private final boolean italic;

    TextElementKey(Font font) {
      size = font.getSize();
      fontWeight = font.getWeight();
      italic = font.getItalic();
    }

    public int getSize() {
      return size;
    }

    public FontWeight getFontWeight() {
      return fontWeight;
    }

    public boolean isItalic() {
      return italic;
    }

    @Override
    public int hashCode() {
      // Can't use Objects.hash() as it is only available in KK+ and can't use Guava's impl either.
      int result = size;
      result = 31 * result + fontWeight.getNumber();
      result = 31 * result + (italic ? 1 : 0);
      return result;
    }

    @Override
    public boolean equals(/*@Nullable*/ Object obj) {
      if (obj == this) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (!(obj instanceof TextElementKey)) {
        return false;
      }

      TextElementKey key = (TextElementKey) obj;
      return key.size == size && key.fontWeight == fontWeight && key.italic == italic;
    }
  }
  // LINT.ThenChange
}
