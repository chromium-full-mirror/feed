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
import android.support.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ParameterizedTextEvaluator;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityHorizontal;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * ElementAdapter provides a base class for the various adapters created by UiElement, and provides
 * handling for overlays and any other general UiElement features. This class enforces the life
 * cycle of the BoundViewAdapter.
 *
 * @param <V> A subclass of an Android View. This is the root level view the adapter is responsible
 *     for creating. Once created, it should have the same lifetime as the adapter.
 * @param <M> The model. This is the model type which is bound to the view by the adapter.
 */
public abstract class ElementAdapter<V extends View, M> implements BoundViewAdapter<M> {
  private static final String TAG = "ElementAdapter";

  // Singleton default UiElement that we can share among all instances (since protos are immutable)
  // This has no parameters set, so should act like not having an associated UiElement.
  private static final Element DEFAULT_ELEMENT = Element.getDefaultInstance();

  private final Context context;
  private final AdapterParameters parameters;
  @Nullable private V view;
  @Nullable private M model;
  private Element baseElement = DEFAULT_ELEMENT;
  private RecyclerKey key;
  private FrameContext frameContext;

  @Nullable private FrameLayout overlayView = null;
  private final List<ElementListAdapter> overlays = new ArrayList<>();

  /**
   * Desired width of this element. {@link #DIMENSION_NOT_SET} indicates no explicit width; let the
   * parent view decide.
   */
  protected int widthPx = DIMENSION_NOT_SET;
  /**
   * Desired height of this element. {@link #DIMENSION_NOT_SET} indicates no explicit height; let
   * the parent view decide.
   */
  protected int heightPx = DIMENSION_NOT_SET;

  ElementAdapter(Context context, AdapterParameters parameters) {
    this.context = context;
    this.parameters = parameters;
  }

  /** Returns a view for external use, such as adding to a layout. */
  @Override
  @Nullable
  public View getView() {
    if (overlayView != null) {
      return overlayView;
    } else {
      return view;
    }
  }

  /**
   * Returns the base view of the adapter; the view typed to the subclass's content. This returns a
   * different result than getView when there is a wrapper FrameLayout to support overlays.
   */
  @Nullable
  protected V getBaseView() {
    return view;
  }

  /**
   * Sets the model and initializes Element-level features (ex. overlays). Do not override; override
   * {@link #onBindModel()} instead.
   */
  @Override
  public void bindModel(M model, FrameContext frameContext) {
    bindModel(model, Element.getDefaultInstance(), frameContext);
  }

  /** Binds to an Element, allowing the subclass to pick out the relevant model from the oneof. */
  public void bindModel(Element baseElement, FrameContext frameContext) {
    bindModel(getModelFromElement(baseElement), baseElement, frameContext);
  }

  /** Bind the model and initialize overlays. Calls child {@link #onBindModel()} methods. */
  void bindModel(M model, Element baseElement, FrameContext frameContext) {
    this.model = model;
    this.baseElement = baseElement;
    this.frameContext = frameContext.bindNewStyle(getElementStyleIdsStack());

    onBindModel(model, baseElement, frameContext);

    checkNotNull(view, "View is null; call initializeAdapter first!");
    initializeOverlays(frameContext);

    initializeActions(frameContext);

    initializeOverflow(baseElement);
  }

  /** Extracts the relevant model from the Element oneof. Throws IllegalArgument if not found. */
  protected abstract M getModelFromElement(Element baseElement);

  /** Child-adapter-specific binding behavior; please override. */
  protected void onBindModel(M model, Element baseElement, FrameContext frameContext) {}

  private void initializeOverlays(FrameContext frameContext) {
    if (baseElement.getOverlayElementsCount() > 0) {
      overlayView = new FrameLayout(getContext());
      overlayView.addView(view);

      for (ElementList overlayList : baseElement.getOverlayElementsList()) {
        ElementListAdapter overlayAdapter =
            getParameters().adapterFactory.createElementListAdapter(overlayList, frameContext);
        overlays.add(overlayAdapter);
        overlayView.addView(overlayAdapter.getView());

        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        switch (overlayAdapter.getVerticalGravity()) {
          case GRAVITY_BOTTOM:
            params.gravity = Gravity.BOTTOM;
            break;
          case GRAVITY_MIDDLE:
            params.gravity = Gravity.CENTER_VERTICAL;
            break;
          case GRAVITY_TOP:
            params.gravity = Gravity.TOP;
            break;
          default:
            params.gravity = Gravity.NO_GRAVITY;
        }
        overlayAdapter.getElementStyle().setMargins(getContext(), params);
        overlayAdapter.setLayoutParams(params);
      }
    }
  }

  protected void initializeActions(FrameContext frameContext) {
    // Set up actions
    switch (baseElement.getActionsDataCase()) {
      case ACTIONS:
        ViewUtils.setOnClickActions(
            baseElement.getActions(), getView(), getParameters().actionHandler, frameContext);
        break;
      case ACTIONS_BINDING:
        ViewUtils.setOnClickActions(
            baseElement.getActionsBinding(),
            getView(),
            getParameters().actionHandler,
            frameContext);
        break;
      default:
        ViewUtils.clearOnClickActions(getView());
    }
  }

  private void initializeOverflow(Element baseElement) {
    switch (baseElement.getHorizontalOverflow()) {
      case OVERFLOW_HIDDEN:
      case OVERFLOW_UNSPECIFIED:
        // These are the default behavior
        break;
      default:
        // TODO: Use the standard Piet error codes: ERR_UNSUPPORTED_FEATURE
        Logger.w(TAG, "Unsupported overflow behavior: %s", baseElement.getHorizontalOverflow());
    }
  }

  /**
   * Gets the style of the Element's content (ex. TextElement). Only valid after model has been
   * bound.
   */
  public StyleProvider getElementStyle() {
    return frameContext.getCurrentStyle();
  }

  /**
   * Returns any styles associated with the bound Element.
   *
   * <p>Element doesn't have any styles, but most sub-elements do, so should be overridden wherever
   * possible.
   */
  protected StyleIdsStack getElementStyleIdsStack() {
    return StyleIdsStack.getDefaultInstance();
  }

  /** Unsets the model. Do not override; override onUnbindModel instead. */
  @Override
  public void unbindModel() {
    onUnbindModel();
    model = null;
    baseElement = DEFAULT_ELEMENT;

    // Unset actions
    ViewUtils.clearOnClickActions(getView());
    if (getView() != getBaseView()) {
      ViewUtils.clearOnClickActions(getBaseView());
    }

    // Destroy overlays
    if (overlayView != null) {
      overlayView.removeAllViews();
      overlayView = null;
    }
    for (ElementListAdapter overlayAdapter : overlays) {
      getParameters().adapterFactory.releaseAdapter(overlayAdapter);
    }
    overlays.clear();
  }

  protected void onUnbindModel() {};

  /**
   * Subclasses will call this method when they are first bound to a Model to set the View created
   * by the Adapter and the Key based upon the Model type. All Views will have a Tag point to their
   * Adapter.
   */
  void initializeAdapter(V view, RecyclerKey key) {
    this.view = view;
    this.key = key;
  }

  @Override
  public RecyclerKey getKey() {
    // There are Adapters which don't hold on to their views.  How do we want to mark them?
    return key;
  }

  /**
   * Returns the desired width of the adapter, computed from the model at bind time, or {@link
   * #DIMENSION_NOT_SET} which implies that the parent can choose the width.
   */
  @Override
  public int getComputedWidthPx() {
    return widthPx;
  }

  /**
   * Returns the desired height of the adapter, computed from the model at bind time, or {@link
   * #DIMENSION_NOT_SET} which implies that the parent can choose the height.
   */
  @Override
  public int getComputedHeightPx() {
    return heightPx;
  }

  @Override
  public void setLayoutParams(LayoutParams layoutParams) {
    if (getView() == null) {
      return;
    }
    getView().setLayoutParams(layoutParams);
  }

  protected AdapterParameters getParameters() {
    return parameters;
  }

  protected Context getContext() {
    return context;
  }

  /** Returns the {@code model}, but is only legal to call when the Adapter is bound to a model */
  protected M getModel() {
    return checkNotNull(model);
  }

  GravityHorizontal getHorizontalGravity() {
    return baseElement.getGravityHorizontal();
  }

  /** Returns the {@code model}, this is to support testing, it can be called at any time */
  @VisibleForTesting
  @Nullable
  M getRawModel() {
    return model;
  }

  protected Frame getFrame() {
    return frameContext.getFrame();
  }

  protected FrameContext getFrameContext() {
    return frameContext;
  }

  protected ParameterizedTextEvaluator getTemplatedStringEvaluator() {
    return parameters.templatedStringEvaluator;
  }
}
