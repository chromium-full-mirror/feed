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

import static com.google.android.libraries.feed.piet.StyleProvider.DEFAULT_STYLE;
import static com.google.android.libraries.feed.piet.StyleProvider.DEFAULT_STYLE_PROVIDER;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.ui.RoundedCornerColorDrawable;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ChunkedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.CustomBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.GridCellWidthBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ImageBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ParameterizedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.BindingValue.Visibility;
import com.google.search.now.ui.piet.ElementsProto.CustomElementData;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.BoundStyle;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ChunkedText;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * This is a {@link FrameContext} that is bound. Binding means we have a {@code Frame}, an optional
 * map of {@code BindingValues} and a {@code StyleProvider}.
 */
class FrameContext {
  private static final String TAG = "FrameContext";

  /** What kind of error are we reporting when calling {@link #reportError(ErrorType, String)}. */
  @IntDef({ErrorType.ERROR, ErrorType.WARNING})
  @interface ErrorType {
    int ERROR = 1;
    int WARNING = 2;
  }

  private final PietStylesHelper stylesHelper;
  private final AssetProvider assetProvider;
  private final CustomElementProvider customElementProvider;
  private final Map<String, Template> frameTemplates = new HashMap<>();

  // This is the Frame which contains all the slices being processed.
  private final Frame currentFrame;

  @VisibleForTesting final List<String> errors = new ArrayList<>();

  // The Current Stylesheet as a map from style_id to Style.
  private final Map<String, Style> stylesheet;

  // The in-scope bindings as a map from state_id to binding value
  private final Map<String, BindingValue> bindingValues;

  // This is the current style provider
  private final StyleProvider currentStyleProvider;

  // The base / default style; inherited from the template or frame.
  private final Style baseStyle;

  FrameContext(
      Frame frame,
      Map<String, Style> stylesheet,
      Style baseStyle,
      StyleProvider styleProvider,
      PietStylesHelper stylesHelper,
      AssetProvider assetProvider,
      CustomElementProvider customElementProvider) {
    this(
        frame,
        stylesheet,
        null,
        baseStyle,
        styleProvider,
        stylesHelper,
        assetProvider,
        customElementProvider);
  }

  FrameContext(
      Frame frame,
      Map<String, Style> stylesheet,
      @Nullable Map<String, BindingValue> bindingValues,
      Style baseStyle,
      StyleProvider styleProvider,
      PietStylesHelper stylesHelper,
      AssetProvider assetProvider,
      CustomElementProvider customElementProvider) {
    currentFrame = frame;
    this.stylesheet = stylesheet;
    this.bindingValues = bindingValues == null ? new HashMap<>() : bindingValues;
    this.baseStyle = baseStyle;
    currentStyleProvider = styleProvider;
    this.stylesHelper = stylesHelper;
    this.assetProvider = assetProvider;
    this.customElementProvider = customElementProvider;

    if (frame.getTemplatesCount() > 0) {
      for (Template template : frame.getTemplatesList()) {
        frameTemplates.put(template.getTemplateId(), template);
      }
    }
  }

  public AssetProvider getAssetProvider() {
    return assetProvider;
  }

  public CustomElementProvider getCustomElementProvider() {
    return customElementProvider;
  }

  public Frame getFrame() {
    return currentFrame;
  }

  @Nullable
  public Template getTemplate(String templateId) {
    Template template = frameTemplates.get(templateId);
    return (template != null) ? template : stylesHelper.getTemplate(templateId);
  }

  @Nullable
  public PietSharedState getPietSharedState() {
    return stylesHelper.getCurrentPietSharedState();
  }

  /**
   * Creates a new FrameContext which is scoped properly for the Template. The Frame's stylesheet is
   * discarded and replaced by the Template's stylesheet. In addition, a Template can define a root
   * level style which applies to all child elements.
   */
  // TODO: Why do we allow bindingContext to be null if it's an error?
  FrameContext bindTemplate(Template template, @Nullable BindingContext bindingContext) {
    if (bindingContext == null) {
      Logger.w(TAG, reportError(ErrorType.WARNING, "Template BindingContext was not provided"));
      return this;
    }
    Map<String, BindingValue> binding = createBindingValueMap(bindingContext);
    Map<String, Style> localStylesheet;

    switch (template.getTemplateStylesheetCase()) {
      case STYLESHEET:
        localStylesheet = PietStylesHelper.createMapFromStylesheet(template.getStylesheet());
        break;
      case STYLESHEET_ID:
        localStylesheet = stylesHelper.getStylesheet(template.getStylesheetId());
        break;
      default:
        localStylesheet = new HashMap<>();
    }

    Style templateBaseStyle;
    StyleProvider templateStyleProvider;
    if (template.hasChildDefaultStyleIds()) {
      templateBaseStyle =
          PietStylesHelper.mergeStyleIdsStack(
              DEFAULT_STYLE, template.getChildDefaultStyleIds(), localStylesheet, this, false);
      templateStyleProvider = new StyleProvider(templateBaseStyle);
    } else {
      templateBaseStyle = DEFAULT_STYLE;
      templateStyleProvider = DEFAULT_STYLE_PROVIDER;
    }
    return new FrameContext(
        currentFrame,
        localStylesheet,
        binding,
        templateBaseStyle,
        templateStyleProvider,
        stylesHelper,
        assetProvider,
        customElementProvider);
  }

  /**
   * Creates a new FrameContext from the existing FrameContext using {@code styleId} to lookup the
   * new Style.
   */
  FrameContext bindNewStyle(StyleIdsStack styles) {
    if (styles.getStyleIdsList().isEmpty()) {
      return this;
    }
    StyleProvider styleProvider = makeStyleFor(styles);
    return styleProvider != null
        ? new FrameContext(
            currentFrame,
            stylesheet,
            bindingValues,
            baseStyle,
            styleProvider,
            stylesHelper,
            assetProvider,
            customElementProvider)
        : this;
  }

  /** Return the current {@link StyleProvider}. */
  public StyleProvider getCurrentStyle() {
    return currentStyleProvider;
  }

  /** Return a {@link StyleProvider} for the style. */
  public StyleProvider makeStyleFor(StyleIdsStack styles) {
    return makeStyleFor(styles, false);
  }

  /** Return a {@link StyleProvider} for the style, and allow errors to be suppressed */
  public StyleProvider makeStyleFor(StyleIdsStack styles, boolean suppressErrors) {
    if (styles.getStyleIdsList().isEmpty()) {
      return currentStyleProvider;
    }
    Style style =
        PietStylesHelper.mergeStyleIdsStack(baseStyle, styles, stylesheet, this, suppressErrors);
    if (style == null) {
      if (!suppressErrors) {
        Logger.e(
            TAG,
            reportError(
                ErrorType.ERROR,
                String.format(
                    "Unable to bind styles [%s], style not found in Stylesheet", styles)));
      }
      return currentStyleProvider;
    }
    return new StyleProvider(style);
  }

  /**
   * Return a {@link Drawable} with the fill and rounded corners defined on the style; returns
   * {@code null} if the background has no color defined.
   */
  @Nullable
  Drawable createBackground(StyleProvider style, Context context) {
    Fill background = style.getBackground();
    if (!background.hasColor()) {
      return null;
    }
    RoundedCornerColorDrawable bgDrawable =
        new RoundedCornerColorDrawable(
            background.getColor(), getRadius(style.getRoundedCorners(), context));
    if (style.hasRoundedCorners()) {
      RoundedCorners roundedCorners = style.getRoundedCorners();
      // If we defined rounded corners the default bitmask is rounding all the corners.
      int bitmask =
          roundedCorners.hasBitmask()
              ? roundedCorners.getBitmask()
              : RoundedCornerColorDrawable.TOP_CORNERS | RoundedCornerColorDrawable.BOTTOM_CORNERS;
      bgDrawable.setRoundedCorners(bitmask);
    }
    return bgDrawable;
  }

  /**
   * Return a {@link Image} for the binding if there is one defined; otherwise returns {@code null}.
   */
  Image getImageFromBinding(ImageBindingRef binding) {
    return getImageBindingValue(binding).getImage();
  }

  /**
   * Return a {@link ElementList} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  ElementList getElementListFromBinding(ElementListBindingRef binding) {
    return getElementListBindingValue(binding).getElementList();
  }

  /**
   * Return a {@link ParameterizedText} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  ParameterizedText getParameterizedTextFromBinding(ParameterizedTextBindingRef binding) {
    return getParameterizedTextBindingValue(binding).getParameterizedText();
  }

  /**
   * Return a {@link GridCellWidth} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  GridCellWidth getGridCellWidthFromBinding(GridCellWidthBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasCellWidth()
        ? null
        : bindingValue.getCellWidth();
  }

  /**
   * Return an {@link Actions} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  @Nullable
  Actions getActionsFromBinding(ActionsBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasActions() ? null : bindingValue.getActions();
  }

  /**
   * Return a {@link CustomElementData} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  CustomElementData getCustomElementFromBinding(CustomBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasCustomElementData()
        ? null
        : bindingValue.getCustomElementData();
  }

  /**
   * Return a {@link BoundStyle} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  @Nullable
  BoundStyle getStyleFromBinding(StyleBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasBoundStyle()
        ? null
        : bindingValue.getBoundStyle();
  }

  /**
   * Return a {@link TemplateInvocation} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  TemplateInvocation getTemplateInvocationFromBinding(TemplateBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasTemplateInvocation()
        ? null
        : bindingValue.getTemplateInvocation();
  }

  @Nullable
  ChunkedText getChunkedTextFromBinding(ChunkedTextBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    return bindingValue == null || !bindingValue.hasChunkedText()
        ? null
        : bindingValue.getChunkedText();
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a text resource.
   */
  BindingValue getParameterizedTextBindingValue(ParameterizedTextBindingRef binding) {
    BindingValue returnValue = bindingValues.get(binding.getBindingId());
    if (returnValue == null
        || (!returnValue.hasParameterizedText()
            && !returnValue.getVisibility().equals(Visibility.GONE))) {
      throw new IllegalStateException(
          String.format("Text binding not found for %s", binding.getBindingId()));
    } else {
      return returnValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a image resource.
   */
  BindingValue getImageBindingValue(ImageBindingRef binding) {
    BindingValue returnValue = bindingValues.get(binding.getBindingId());
    if (returnValue == null
        || (!returnValue.hasImage() && !returnValue.getVisibility().equals(Visibility.GONE))) {
      throw new IllegalStateException(
          String.format("Image binding not found for %s", binding.getBindingId()));
    } else {
      return returnValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a ElementList.
   */
  BindingValue getElementListBindingValue(ElementListBindingRef binding) {
    BindingValue returnValue = bindingValues.get(binding.getBindingId());
    if (returnValue == null
        || (!returnValue.hasElementList()
            && !returnValue.getVisibility().equals(Visibility.GONE))) {
      throw new IllegalStateException(
          String.format("ElementList binding not found for %s", binding.getBindingId()));
    } else {
      return returnValue;
    }
  }

  /**
   * Report user errors in frames. This will return the fully formed error so it can be logged at
   * the site of the error.
   */
  public String reportError(@ErrorType int errorType, String error) {
    String e =
        String.format(
            "[%s] %s - %s", currentFrame.getTag(), errorType == ErrorType.ERROR ? "E" : "W", error);
    errors.add(e);
    return e;
  }

  /**
   * Create a Linear Layout with all of the errors reported by {@link #reportError(String)}. This
   * will return {@code null} if the View shouldn't be displayed.
   */
  @Nullable
  View getErrorView(Context context) {
    // TODO: This needs to be controlled by a DEBUG flag, this should always return
    // null in production.
    if (errors.isEmpty()) {
      return null;
    }
    return new ErrorViewUtils(context).getErrorView(errors);
  }

  private int getRadius(RoundedCorners roundedCorner, Context context) {
    if (roundedCorner.hasRadius()) {
      return (int) ViewUtils.dpToPx(roundedCorner.getRadius(), context);
    } else {
      return assetProvider.getDefaultCornerRadius();
    }
  }

  private Map<String, BindingValue> createBindingValueMap(BindingContext bindingContext) {
    Map<String, BindingValue> bindingValueMap = new HashMap<>();
    if (bindingContext.getBindingValuesCount() == 0) {
      return bindingValueMap;
    }
    for (BindingValue bindingValue : bindingContext.getBindingValuesList()) {
      bindingValueMap.put(bindingValue.getBindingId(), bindingValue);
    }
    return bindingValueMap;
  }
}
