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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/** A {@link ViewGroupBoundViewAdapter} which manages vertical lists of elements. */
public class ElementListAdapter extends ViewGroupBoundViewAdapter<LinearLayout, ElementList> {
  private static final String TAG = "ElementListAdapter";

  private ElementListAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected ElementList getModelFromElement(Element baseElement) {
    // Will also throw for ElementListBindingRef; looking up bindings is not supported here.
    if (!baseElement.hasElementList()) {
      throw new IllegalArgumentException(
          String.format("Missing ElementList; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getElementList();
  }

  @Override
  public void onBindModel(ElementList model, Element baseElement, FrameContext frameContext) {
    initialBind();
    bindLinesToLayout(model, frameContext);
    frameContext
        .makeStyleFor(model.getStyleReferences())
        .setElementStyles(getContext(), frameContext, getView());
  }

  @Override
  protected void initializeActions(FrameContext frameContext) {
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
        super.initializeActions(frameContext);
    }
  }

  @Override
  protected StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  @Override
  public void onUnbindModel() {
    ViewUtils.clearOnClickActions(getView());
    super.onUnbindModel();
  }

  GravityVertical getVerticalGravity() {
    return getModel().getGravityVertical();
  }

  /** Creates each ui element in {@code list} and adds them to {@code views}. */
  private void bindLinesToLayout(ElementList list, FrameContext frameContext) {
    ViewGroup viewGroup = checkNotNull(getBaseView());
    for (Element line : list.getElementsList()) {
      // TODO: reuse existing adapter from childAdapters if rebinding template.
      ElementAdapter<? extends View, ?> adapter = getAdapterForChildElement(line, frameContext);
      View adapterView = adapter.getView();
      if (adapterView != null) {
        viewGroup.addView(adapterView);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(adapterView.getLayoutParams());

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
            params.gravity = Gravity.NO_GRAVITY;
        }

        adapterView.setLayoutParams(params);
      }
    }
  }

  private ElementAdapter<? extends View, ?> getAdapterForChildElement(
      Element contentLine, FrameContext frameContext) {
    ElementAdapter<? extends View, ?> adapter =
        getParameters().adapterFactory.createAdapterForElement(contentLine, frameContext);
    addChildAdapter(adapter);
    return adapter;
  }

  @Override
  protected LinearLayout createViewGroup() {
    LinearLayout viewGroup = new LinearLayout(getContext());
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
