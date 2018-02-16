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

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.FrameContext.ErrorType;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Font.FontWeight;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import javax.annotation.Nullable;

/** A {@link BoundViewAdapter} which manages {@code TextLine} slices. */
public class TextElementAdapter
    extends ElementAdapter<TextView, TextElement, TextElementAdapter.Key> {
  private static final String TAG = "TextElementAdapter";

  TextElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected TextElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasTextElement()) {
      throw new IllegalArgumentException(
          String.format("Missing TextElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getTextElement();
  }

  @Override
  public void onBindModel(TextElement textLine, Element baseElement, FrameContext frameContext) {
    initialBind(frameContext, textLine);

    // Set the initial state for the TextView
    Context context = getContext();
    TextView textView = checkNotNull(getBaseView());
    if (textLine.hasParameterizedTextBinding()) {
      BindingValue bindingValue =
          frameContext.getTextBindingValue(textLine.getParameterizedTextBinding());
      if (bindingValue == null) {
        // If no binding is specified, raise an error and hide the corresponding UI element.
        Logger.e(
            TAG,
            frameContext.reportError(
                ErrorType.ERROR,
                String.format(
                    "TextElement Binding not found for %s",
                    textLine.getParameterizedTextBinding())));
        textView.setVisibility(View.GONE);

      } else {
        if (bindingValue.hasVisibility()) {
          switch (bindingValue.getVisibility()) {
            case INVISIBLE:
              textView.setVisibility(View.INVISIBLE);
              break;
            case GONE:
              textView.setVisibility(View.GONE);
              break;
            case VISIBLE:
              // Nothing to do for default: visible. Included here for completeness.
              break;
            default:
              // Intentionally keeping the error string short, because all debug strings will be
              // included in the binary.
              throw new AssertionError("Unhandled");
          }
        }

        // If text is provided, or is null, set it even if visibility is set to GONE or INVISIBLE,
        // in case a later optimistic update operation changes the visibility but wants to retain
        // the previously-set text.
        setTextOnView(frameContext, textView, bindingValue.getParameterizedText());
      }
    } else {
      // No bindings found, so use the inlined value (or empty if not set)
      setTextOnView(
          frameContext,
          textView,
          textLine.hasParameterizedText() ? textLine.getParameterizedText() : null);
    }

    // Setup the layout of the text lines.
    StyleProvider textStyle = frameContext.makeStyleFor(textLine.getStyleReferences());
    textStyle.setElementStyles(context, frameContext, textView);
    textView.setTextColor(textStyle.getColor());
    if (textStyle.getMaxLines() > 0) {
      textView.setMaxLines(textStyle.getMaxLines());
      textView.setEllipsize(TextUtils.TruncateAt.END);
    } else {
      // MAX_VALUE is the value used in the Android implementation for the default
      textView.setMaxLines(Integer.MAX_VALUE);
    }
  }

  @Override
  protected StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  private void setTextOnView(
      FrameContext frameContext, TextView textView, @Nullable ParameterizedText parameterizedText) {
    if (parameterizedText == null) {
      Logger.e(
          TAG,
          frameContext.reportError(
              ErrorType.ERROR,
              "ParameterizedText missing for TextElement, setting to empty String"));
      textView.setText("");
      return;
    }

    textView.setText(getTemplatedStringEvaluator().evaluate(frameContext, parameterizedText));
  }

  @Override
  public void onUnbindModel() {
    TextView textView = checkNotNull(getBaseView());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
    }
    textView.setVisibility(View.VISIBLE);
  }

  private void initialBind(FrameContext frameContext, TextElement textLine) {
    if (getBaseView() != null) {
      return;
    }
    // Don't report errors, we will report them when we bind the model to the View.
    StyleProvider styleProvider = frameContext.makeStyleFor(textLine.getStyleReferences(), true);
    Key k = new Key(styleProvider.getFont());
    TextView v = createTextView(textLine, styleProvider.getFont());
    initializeAdapter(v, k);
  }

  @VisibleForTesting
  TextView createTextView(TextElement textElement, Font font) {
    // TODO: Line Height is currently not supported.
    // TODO: Implement typefaces
    TextView textView = new TextView(getContext());
    textView.setTextSize(font.getSize());
    boolean bold = false;
    switch (font.getWeight()) {
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
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_black);
        break;
      default:
        TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_regular);
        break;
    }
    if (bold || font.getItalic()) {
      if (bold && font.getItalic()) {
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
      } else if (bold) {
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
      } else if (font.getItalic()) {
        textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
      }
    }
    return textView;
  }

  static class KeySupplier implements AdapterKeySupplier<TextElementAdapter, TextElement, Key> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public TextElementAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new TextElementAdapter(context, parameters);
    }

    @Override
    public Key getKey(FrameContext frameContext, TextElement model) {
      StyleProvider styleProvider = frameContext.makeStyleFor(model.getStyleReferences(), true);
      return new Key(styleProvider.getFont());
    }

    @Override
    public boolean isSingletonKey() {
      return false;
    }
  }

  /** We will Key TextViews off of the Ellipsizing, Font Size and FontWeight, and Italics. */
  static class Key {
    private int size;

    private FontWeight fontWeight;

    private boolean italic;

    Key(Font font) {
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
    public boolean equals(@Nullable Object obj) {
      if (obj == this) {
        return true;
      }

      if (!(obj instanceof Key)) {
        return false;
      }

      if (obj == null) {
        return false;
      }

      Key key = (Key) obj;
      return key.size == size && key.fontWeight == fontWeight && key.italic == italic;
    }
  }
}
