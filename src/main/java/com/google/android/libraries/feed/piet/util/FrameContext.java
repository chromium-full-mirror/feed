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
import android.graphics.drawable.Drawable;
import android.view.View;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.CustomBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.GridCellWidthBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ImageBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ParameterizedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.CustomElementData;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.BoundStyle;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import javax.annotation.Nullable;

/**
 * An interface providing access to the Stylesheet and Styles. In addition, we provide other host
 * provided implementations.
 */
public interface FrameContext {

  /** What kind of error are we reporting when calling {@link #reportError(ErrorType, String)}. */
  enum ErrorType {
    WARNING,
    ERROR
  }

  /** Returns the {@link AssetProvider} */
  AssetProvider getAssetProvider();

  /** Returns the {@link CustomElementProvider} */
  CustomElementProvider getCustomElementProvider();

  /** Return the Frame being built */
  Frame getFrame();

  /** Return a {@link Template} for the template id */
  @Nullable
  Template getTemplate(String templateId);

  /**
   * Creates a new FrameContext which is scoped properly for the Template. If a Template defines a
   * Stylesheet, it overrides the Frame defined Stylesheet. In addition, a Template can define a
   * root level Style.
   */
  FrameContext bindTemplate(Template template, BindingContext bindingContext);

  /**
   * Creates a new FrameContext from the existing FrameContext using {@code styleId} to lookup the
   * new Style. The {@code Slice} will remain the same.
   */
  FrameContext bindNewStyle(StyleIdsStack styles);

  /** Return the current {@link StyleProvider}. */
  StyleProvider getCurrentStyle();

  /** Return a {@link StyleProvider} for the style. */
  StyleProvider makeStyleFor(StyleIdsStack styles);

  /** Return a {@link StyleProvider} for the style, and allow errors to be suppressed */
  StyleProvider makeStyleFor(StyleIdsStack styles, boolean suppressErrors);

  /**
   * Return a {@link Drawable} with the fill and rounded corners defined on the style; returns
   * {@code null} if the background has no color defined.
   */
  @Nullable
  Drawable createBackground(StyleProvider style, Context context);

  /**
   * Return a {@link Image} for the binding if there is one defined; otherwise returns {@code null}.
   */
  @Nullable
  Image getImageFromBinding(ImageBindingRef binding);

  /**
   * Return a {@link ElementList} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  @Nullable
  ElementList getElementListFromBinding(ElementListBindingRef binding);

  /**
   * Return a {@link ParameterizedText} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  ParameterizedText getTextFromBinding(ParameterizedTextBindingRef binding);

  /**
   * Return a {@link GridCellWidth} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  GridCellWidth getGridCellWidthFromBinding(GridCellWidthBindingRef binding);

  /**
   * Return an {@link Actions} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  @Nullable
  Actions getActionsFromBinding(ActionsBindingRef binding);

  /**
   * Return a {@link CustomElementData} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  CustomElementData getCustomElementFromBinding(CustomBindingRef binding);

  /**
   * Return a {@link BoundStyle} for the binding if there is one defined; otherwise returns {@code
   * null}.
   */
  @Nullable
  BoundStyle getStyleFromBinding(StyleBindingRef binding);

  /**
   * Return a {@link TemplateInvocation} for the binding if there is one defined; otherwise returns
   * {@code null}.
   */
  @Nullable
  public TemplateInvocation getTemplateInvocationFromBinding(TemplateBindingRef binding);

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a text resource.
   */
  @Nullable
  BindingValue getTextBindingValue(ParameterizedTextBindingRef binding);

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a image resource.
   */
  @Nullable
  BindingValue getImageBindingValue(ImageBindingRef binding);

  /**
   * Returns the {@link BindingValue} for the BindingRef; throws IllegalStateException if binding id
   * does not point to a ElementList.
   */
  @Nullable
  BindingValue getElementListBindingValue(ElementListBindingRef binding);

  /**
   * Report user errors in frames. This will return the fully formed error so it can be logged at
   * the site of the error.
   */
  String reportError(ErrorType errorType, String error);

  /**
   * Create a Linear Layout with all of the errors reported by {@link #reportError(String)}. This
   * will return {@code null} if the View shouldn't be displayed.
   */
  @Nullable
  View getErrorView(Context context);
}
