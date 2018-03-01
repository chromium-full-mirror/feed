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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Template;
import javax.annotation.Nullable;

/** A {@link ViewGroupBoundViewAdapter} which manages {@code TemplateInvocation} slices. */
public class TemplateInvocationAdapter
    extends ViewGroupBoundViewAdapter<LinearLayout, TemplateInvocation> {

  private static final String TAG = "TemplateInvocationAdapter";

  TemplateInvocationAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected TemplateInvocation getModelFromElement(Element baseElement) {
    throw new IllegalArgumentException(
        "Templates must be re-bound; cannot be extracted from an Element.");
  }

  @Override
  public void onBindModel(
      TemplateInvocation model, Element baseElement, FrameContext frameContext) {
    onBindModel(model, frameContext, null);
  }

  public void onBindModel(
      TemplateInvocation model,
      FrameContext frameContext,
      @Nullable ShardingControl shardingControl) {
    initialBind();
    Template template = frameContext.getTemplate(model.getTemplateId());
    if (template == null) {
      // TODO: How do we handle errors
      Logger.e(TAG, "Template was not found: %s", model.getTemplateId());
      return;
    }

    // TODO: Need to support Sharding.
    if (model.getBindingContextsCount() > 0 && template.hasElementList()) {
      ViewGroup viewGroup = checkNotNull(getBaseView());
      ElementListAdapter listAdapter = null;
      for (BindingContext bindingContext : model.getBindingContextsList()) {
        // Create an ElementListAdapter for each of the bindingContexts - this adapter holds a list
        // of lists
        FrameContext localFrameContext = frameContext.bindTemplate(template, bindingContext);
        listAdapter = getAdapterForChildList(template.getElementList(), localFrameContext);
        setLayoutParamsOnChild(listAdapter);
        if (listAdapter.getView() != null) {
          viewGroup.addView(listAdapter.getView());
        }
      }
    }
  }

  /** Create an Adapter for the {@code ElementList}. */
  private ElementListAdapter getAdapterForChildList(ElementList list, FrameContext frameContext) {
    ElementListAdapter listAdapter =
        getParameters().adapterFactory.createElementListAdapter(list, frameContext);
    addChildAdapter(listAdapter);
    return listAdapter;
  }

  @Override
  protected LinearLayout createViewGroup() {
    LinearLayout viewGroup = new LinearLayout(getContext());
    viewGroup.setOrientation(LinearLayout.VERTICAL);
    viewGroup.setLayoutParams(
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    return viewGroup;
  }

  // TODO: Create a unique key based on the template.
  @Override
  protected RecyclerKey createKey() {
    return KeySupplier.SINGLETON_KEY;
  }

  protected void setLayoutParamsOnChild(BoundViewAdapter<?> childAdapter) {
    int width = childAdapter.getComputedWidthPx();
    width = width == BoundViewAdapter.DIMENSION_NOT_SET ? LayoutParams.MATCH_PARENT : width;
    int height = childAdapter.getComputedHeightPx();
    height = height == BoundViewAdapter.DIMENSION_NOT_SET ? LayoutParams.WRAP_CONTENT : height;

    childAdapter.setLayoutParams(new LinearLayout.LayoutParams(width, height));
  }

  // TODO: Create some kind of key which determines whether a TemplateInvocationAdapter
  // can be recycled and rebound to a given model. This could be tricky because different frames
  // could have templates with the same id, and the shared state can be updated with new templates
  // that share an ID.
  static class KeySupplier
      extends SingletonKeySupplier<TemplateInvocationAdapter, TemplateInvocation> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public TemplateInvocationAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new TemplateInvocationAdapter(context, parameters);
    }
  }
}
