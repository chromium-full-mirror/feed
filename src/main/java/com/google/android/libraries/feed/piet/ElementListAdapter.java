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

import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/** An {@link ElementContainerAdapter} which manages vertical lists of elements. */
class ElementListAdapter
    extends ElementContainerAdapter<LinearLayout, ElementAdapter<? extends View, ?>, ElementList> {
  private static final String TAG = "ElementListAdapter";

  private ElementListAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context), KeySupplier.SINGLETON_KEY);
  }

  @Override
  ElementList getModelFromElement(Element baseElement) {
    // Will also throw for ElementListBindingRef; looking up bindings is not supported here.
    if (!baseElement.hasElementList()) {
      throw new IllegalArgumentException(
          String.format("Missing ElementList; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getElementList();
  }

  @Override
  void onCreateAdapter(ElementList model, Element baseElement, FrameContext frameContext) {
    for (Element line : model.getElementsList()) {
      ElementAdapter<? extends View, ?> adapter = getAdapterForChildElement(line, frameContext);
      getBaseView().addView(adapter.getView());

      int width = adapter.getComputedWidthPx();
      width =
          width == ElementAdapter.DIMENSION_NOT_SET ? ViewGroup.LayoutParams.MATCH_PARENT : width;
      int height = adapter.getComputedHeightPx();
      height =
          height == ElementAdapter.DIMENSION_NOT_SET ? ViewGroup.LayoutParams.WRAP_CONTENT : height;

      LayoutParams params = new LayoutParams(width, height);

      adapter.getElementStyle().setMargins(getContext(), params);

      switch (adapter.getHorizontalGravity()) {
        case GRAVITY_START:
          params.gravity = Gravity.START;
          break;
        case GRAVITY_CENTER:
          params.gravity = Gravity.CENTER_HORIZONTAL;
          break;
        case GRAVITY_END:
          params.gravity = Gravity.END;
          break;
        default:
          params.gravity = Gravity.START;
      }

      adapter.setLayoutParams(params);
    }
  }

  @Override
  void onBindModel(ElementList model, Element baseElement, FrameContext frameContext) {
    checkState(
        model.getElementsCount() == childAdapters.size(),
        "Number of elements mismatch: got %s elements, had %s adapters",
        model.getElementsCount(),
        childAdapters.size());
    for (int i = 0; i < model.getElementsCount(); i++) {
      Element cellModel = model.getElements(i);
      if (cellModel.hasElementListBinding()) {
        createAndBindChildListAdapter(
            (ElementListAdapter) childAdapters.get(i),
            cellModel.getElementListBinding(),
            frameContext);
      } else {
        childAdapters.get(i).bindModel(model.getElements(i), frameContext);
      }
    }
  }

  @Override
  void bindActions(FrameContext frameContext) {
    switch (getModel().getActionsDataCase()) {
      case ACTIONS:
        ViewUtils.setOnClickActions(
            getModel().getActions(), getView(), getParameters().actionHandler, frameContext);
        break;
      case ACTIONS_BINDING:
        ViewUtils.setOnClickActions(
            getModel().getActionsBinding(), getView(), getParameters().actionHandler, frameContext);
        break;
      default:
        super.bindActions(frameContext);
    }
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  @Override
  void onUnbindModel() {
    ViewUtils.clearOnClickActions(getView());
    super.onUnbindModel();

    // Bound item layout is created at bind time. We can't reuse the layout in child adapters, so we
    // call release at unbind time so that the child adapter's children can be recycled.
    ElementList model = getRawModel();
    if (model != null) {
      for (int i = 0; i < model.getElementsCount(); i++) {
        Element element = model.getElements(i);
        if (element.hasElementListBinding()) {
          childAdapters.get(i).releaseAdapter();
        }
      }
    }
  }

  GravityVertical getVerticalGravity() {
    return getModel().getGravityVertical();
  }

  private ElementAdapter<? extends View, ?> getAdapterForChildElement(
      Element contentLine, FrameContext frameContext) {
    ElementAdapter<? extends View, ?> adapter;
    if (contentLine.hasElementListBinding()) {
      adapter =
          getParameters()
              .elementAdapterFactory
              .createElementListAdapter(ElementList.getDefaultInstance(), frameContext);
      // Immediately release the adapter, since the first thing we do in bind is to recreate it.
      adapter.releaseAdapter();
    } else {
      adapter =
          getParameters().elementAdapterFactory.createAdapterForElement(contentLine, frameContext);
    }
    addChildAdapter(adapter);
    return adapter;
  }

  @VisibleForTesting
  static LinearLayout createView(Context context) {
    LinearLayout viewGroup = new LinearLayout(context);
    viewGroup.setOrientation(LinearLayout.VERTICAL);
    viewGroup.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    return viewGroup;
  }

  static class KeySupplier extends SingletonKeySupplier<ElementListAdapter, ElementList> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public ElementListAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new ElementListAdapter(context, parameters);
    }
  }
}
