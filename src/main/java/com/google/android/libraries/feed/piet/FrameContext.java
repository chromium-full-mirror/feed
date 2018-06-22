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

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.piet.DebugLogger.MessageType;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ChunkedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.CustomBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.GridCellWidthBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ImageBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ParameterizedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.BoundStyle;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a {@link FrameContext} that is bound. Binding means we have a {@code Frame}, an optional
 * map of {@code BindingValues} and a {@code StyleProvider}.
 */
class FrameContext {
  private static final String TAG = "FrameContext";

  private final PietStylesHelper stylesHelper;
  private final List<PietSharedState> pietSharedStates;
  private final DebugBehavior debugBehavior;
  private final DebugLogger debugLogger;
  private final ActionHandler actionHandler;
  private final HostBindingProvider hostBindingProvider;
  private final Map<String, Template> templates;

  // This is the Frame which contains all the slices being processed.
  private final Frame currentFrame;

  // The Current Stylesheet as a map from style_id to Style.
  private final Map<String, Style> stylesheet;

  // The in-scope bindings as a map from state_id to binding value
  private final Map<String, BindingValue> bindingValues;

  // The base / default style; set to DEFAULT_STYLE for Frame or childDefaultStyleId for Template.
  private final Style baseStyle;

  /** Initialize a FrameContext for the first time from a top-level Frame. */
  @VisibleForTesting
  FrameContext(
      Frame frame,
      Map<String, Style> stylesheet,
      List<PietSharedState> pietSharedStates,
      PietStylesHelper pietStylesHelper,
      DebugBehavior debugBehavior,
      DebugLogger debugLogger,
      ActionHandler actionHandler,
      HostBindingProvider hostBindingProvider) {
    this(
        frame,
        stylesheet,
        new ThrowingEmptyMap(),
        DEFAULT_STYLE,
        pietSharedStates,
        pietStylesHelper,
        debugBehavior,
        debugLogger,
        actionHandler,
        hostBindingProvider,
        new NoKeyOverwriteHashMap<>("Template"));

    stylesHelper.addSharedStateTemplatesToFrame(templates);
    if (frame.getTemplatesCount() > 0) {
      for (Template template : frame.getTemplatesList()) {
        templates.put(template.getTemplateId(), template);
      }
    }
  }

  /** Set up a new FrameContext; typically called to copy an existing FrameContext. */
  FrameContext(
      Frame frame,
      Map<String, Style> stylesheet,
      Map<String, BindingValue> bindingValues,
      Style baseStyle,
      List<PietSharedState> pietSharedStates,
      PietStylesHelper pietStylesHelper,
      DebugBehavior debugBehavior,
      DebugLogger debugLogger,
      ActionHandler actionHandler,
      HostBindingProvider hostBindingProvider,
      Map<String, Template> templates) {
    currentFrame = frame;
    this.stylesheet = stylesheet;
    this.bindingValues = bindingValues;
    this.baseStyle = baseStyle;
    this.stylesHelper = pietStylesHelper;
    this.pietSharedStates = pietSharedStates;
    this.debugBehavior = debugBehavior;
    this.debugLogger = debugLogger;
    this.actionHandler = actionHandler;
    this.hostBindingProvider = hostBindingProvider;
    this.templates = templates;
  }

  /**
   * Creates a {@link FrameContext} and constructs the stylesheets from the frame and the list of
   * {@link PietSharedState}. Any errors found with the styling will be reported.
   */
  public static FrameContext createFrameContext(
      Frame frame,
      List<PietSharedState> pietSharedStates,
      DebugBehavior debugBehavior,
      DebugLogger debugLogger,
      ActionHandler actionHandler,
      HostBindingProvider hostBindingProvider) {
    PietStylesHelper pietStylesHelper = new PietStylesHelper(pietSharedStates);
    NoKeyOverwriteHashMap<String, Style> styleMap = null;
    if (frame.hasStylesheetId()) {
      styleMap = pietStylesHelper.getStylesheet(frame.getStylesheetId());
    } else if (frame.hasStylesheet()) {
      styleMap = PietStylesHelper.createMapFromStylesheet(frame.getStylesheet());
    }

    if (styleMap == null) {
      styleMap = new NoKeyOverwriteHashMap<>("Style");
    }
    FrameContext frameContext =
        new FrameContext(
            frame,
            styleMap,
            pietSharedStates,
            pietStylesHelper,
            debugBehavior,
            debugLogger,
            actionHandler,
            hostBindingProvider);
    frameContext.computeErrors();

    return frameContext;
  }

  /**
   * Creates a new FrameContext which is scoped properly for the Template. The Frame's stylesheet is
   * discarded and replaced by the Template's stylesheet. In addition, a Template can define a root
   * level style which applies to all child elements.
   */
  FrameContext createTemplateContext(Template template, BindingContext bindingContext) {
    Map<String, BindingValue> bindingValues = createBindingValueMap(bindingContext);
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
    if (template.hasChildDefaultStyleIds()) {
      StyleIdsStack childDefaultStyleIds = template.getChildDefaultStyleIds();
      if (template.getChildDefaultStyleIds().hasStyleBinding()) {
        Logger.wtf(
            TAG,
            "Style bindings not supported for template default style (template: %s)",
            template.getTemplateId());
        childDefaultStyleIds = childDefaultStyleIds.toBuilder().clearStyleBinding().build();
      }
      templateBaseStyle =
          PietStylesHelper.mergeStyleIdsStack(
              DEFAULT_STYLE,
              childDefaultStyleIds,
              localStylesheet,
              null // Null because the new FrameContext has not been set up yet
              );
    } else {
      templateBaseStyle = DEFAULT_STYLE;
    }
    return new FrameContext(
        currentFrame,
        localStylesheet,
        bindingValues,
        templateBaseStyle,
        pietSharedStates,
        stylesHelper,
        debugBehavior,
        debugLogger,
        actionHandler,
        hostBindingProvider,
        templates);
  }

  public DebugBehavior getDebugBehavior() {
    return debugBehavior;
  }

  public DebugLogger getDebugLogger() {
    return debugLogger;
  }

  public Frame getFrame() {
    return currentFrame;
  }

  public ActionHandler getActionHandler() {
    return actionHandler;
  }

  public Template getTemplate(String templateId) {
    Template template = templates.get(templateId);
    if (template == null) {
      throw new IllegalArgumentException(
          reportError(MessageType.ERROR, String.format("Template '%s' not found", templateId)));
    }
    return template;
  }

  public List<PietSharedState> getPietSharedStates() {
    return pietSharedStates;
  }

  /** Return a {@link StyleProvider} for the style. */
  public StyleProvider makeStyleFor(StyleIdsStack styles) {
    return new StyleProvider(
        PietStylesHelper.mergeStyleIdsStack(baseStyle, styles, stylesheet, this));
  }

  /**
   * Return a {@link GridCellWidth} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  /*@Nullable*/
  GridCellWidth getGridCellWidthFromBinding(GridCellWidthBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    // Purposefully check for host binding and overwrite here as we want to perform the hasCellWidth
    // checks on host binding.   This allows the host to act more like the server.
    if (bindingValue != null && bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getGridCellWidthBindingForValue(bindingValue);
    }
    return bindingValue == null || !bindingValue.hasCellWidth()
        ? null
        : bindingValue.getCellWidth();
  }

  /**
   * Return an {@link Actions} for the binding if there is one defined; otherwise returns the
   * default instance.
   */
  Actions getActionsFromBinding(ActionsBindingRef binding) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    if (bindingValue != null && bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getActionsBindingForValue(bindingValue);
    }
    if (bindingValue == null || !bindingValue.hasActions()) {
      Logger.w(TAG, "No actions found for binding %s", binding.getBindingId());
      return Actions.getDefaultInstance();
    }
    return bindingValue.getActions();
  }

  /**
   * Return a {@link BoundStyle} for the binding if there is one defined; otherwise returns the
   * default instance.
   */
  BoundStyle getStyleFromBinding(StyleBindingRef binding) {
    return getStyleFromBinding(binding, bindingValues);
  }

  /**
   * Return a {@link BoundStyle} for the binding if there is one defined; otherwise returns the
   * default instance.
   */
  BoundStyle getStyleFromBinding(StyleBindingRef binding, Map<String, BindingValue> bindingValues) {
    BindingValue bindingValue = bindingValues.get(binding.getBindingId());
    if (bindingValue != null && bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getStyleBindingForValue(bindingValue);
    }
    if (bindingValue == null || !bindingValue.hasBoundStyle()) {
      Logger.w(TAG, "No style found for binding %s", binding.getBindingId());
      return BoundStyle.getDefaultInstance();
    }
    return bindingValue.getBoundStyle();
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a template resource.
   */
  BindingValue getTemplateInvocationBindingValue(TemplateBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getTemplateBindingForValue(bindingValue);
    }
    if (bindingValue.hasVisibility()) {
      Logger.e(TAG, "Visibility not supported for templates; got %s", bindingValue.getVisibility());
    }
    if (!bindingValue.hasTemplateInvocation() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format("Template binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a custom element resource.
   */
  BindingValue getCustomElementBindingValue(CustomBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getCustomElementDataBindingForValue(bindingValue);
    }
    if (!bindingValue.hasCustomElementData() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format("Custom element binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a chunked text resource.
   */
  BindingValue getChunkedTextBindingValue(ChunkedTextBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getChunkedTextBindingForValue(bindingValue);
    }
    if (!bindingValue.hasChunkedText() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format("Chunked text binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a parameterized text resource.
   */
  BindingValue getParameterizedTextBindingValue(ParameterizedTextBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getParameterizedTextBindingForValue(bindingValue);
    }
    if (!bindingValue.hasParameterizedText() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format(
                  "Parameterized text binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a image resource.
   */
  BindingValue getImageBindingValue(ImageBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getImageBindingForValue(bindingValue);
    }
    if (!bindingValue.hasImage() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format("Image binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to an Element.
   */
  BindingValue getElementBindingValue(ElementBindingRef binding) {
    BindingValue bindingValue = getBindingValue(binding.getBindingId());
    if (bindingValue.hasHostBindingData()) {
      bindingValue = hostBindingProvider.getElementBindingForValue(bindingValue);
    }
    if (!bindingValue.hasElement() && !binding.getIsOptional()) {
      throw new IllegalStateException(
          reportError(
              MessageType.ERROR,
              String.format("Element binding not found for %s", binding.getBindingId())));
    } else {
      return bindingValue;
    }
  }

  private BindingValue getBindingValue(String bindingId) {
    BindingValue returnValue = bindingValues.get(bindingId);
    if (returnValue == null) {
      return BindingValue.getDefaultInstance();
    }
    return returnValue;
  }

  /**
   * Report user errors in frames. This will return the fully formed error so it can be logged at
   * the site of the error.
   */
  public String reportError(@MessageType int errorType, String error) {
    String e = String.format("[%s] %s", currentFrame.getTag(), error);
    debugLogger.recordMessage(errorType, e);
    return e;
  }

  private void computeErrors() {
    if (currentFrame.hasStylesheetId() && stylesheet.isEmpty()) {
      String error =
          String.format(
              "Stylesheet [%s] not found, no stylesheet used", currentFrame.getStylesheetId());
      Logger.w(TAG, reportError(MessageType.ERROR, error));
    } else if (!currentFrame.hasStylesheetId() && !currentFrame.hasStylesheet()) {
      String error = "Frame defined without any Stylesheet";
      Logger.w(TAG, reportError(MessageType.ERROR, error));
    }
  }

  private static Map<String, BindingValue> createBindingValueMap(BindingContext bindingContext) {
    Map<String, BindingValue> bindingValueMap = new NoKeyOverwriteHashMap<>("BindingValue");
    if (bindingContext.getBindingValuesCount() == 0) {
      return bindingValueMap;
    }
    for (BindingValue bindingValue : bindingContext.getBindingValuesList()) {
      bindingValueMap.put(bindingValue.getBindingId(), bindingValue);
    }
    return bindingValueMap;
  }

  /** Map that throws whenever you try to look anything up in it. */
  private static class ThrowingEmptyMap extends HashMap<String, BindingValue> {
    @Override
    public BindingValue get(/*@Nullable*/ Object key) {
      throw new IllegalArgumentException(
          "Looking up bindings not supported in this context; no BindingValues defined.");
    }
  }
}
