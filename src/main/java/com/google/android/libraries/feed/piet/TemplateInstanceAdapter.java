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
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.TemplateInstanceAdapter.TemplateAdapterModel;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.GravityHorizontal;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.List;

/**
 * A wrapper adapter that manages a single instance of a template.
 *
 * <p>This adapter doesn't have its own view; it just delegates everything to its child adapter.
 */
class TemplateInstanceAdapter extends ElementAdapter<View, TemplateAdapterModel> {

  private static final String TAG = "TemplateInstanceAdapter";

  /*@Nullable*/ private Template template = null;
  /*@Nullable*/ private List<PietSharedState> pietSharedStates = null;

  /*@Nullable*/ @VisibleForTesting ElementAdapter<? extends View, ?> childAdapter = null;

  TemplateInstanceAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context));
  }

  @Override
  TemplateAdapterModel getModelFromElement(Element baseElement) {
    throw new IllegalArgumentException(
        "Templates must be re-bound; cannot be extracted from an Element.");
  }

  @Override
  public void createAdapter(
      TemplateAdapterModel model, Element baseElement, FrameContext frameContext) {
    super.createAdapter(model, baseElement, frameContext);

    Template modelTemplate = model.getTemplate();

    // Adapter has already been created.
    if (template != null) {
      if (!templateEquals(template, modelTemplate)) {
        throw new IllegalArgumentException(
            "Adapter was constructed already with different template");
      } else {
        // We're recycling something with a compatible template; return without doing anything.
        return;
      }
    }

    template = modelTemplate;
    pietSharedStates = frameContext.getPietSharedStates();
    setKey(createKey());

    BindingContext bindingContext = model.getBindingContext();
    FrameContext localFrameContext = frameContext.bindTemplate(modelTemplate, bindingContext);
    childAdapter =
        getParameters()
            .elementAdapterFactory
            .createAdapterForElement(modelTemplate.getElement(), localFrameContext);
    childAdapter.createAdapter(modelTemplate.getElement(), localFrameContext);
    setLayoutParamsOnChild(childAdapter);
  }

  @Override
  public void bindModel(
      TemplateAdapterModel model, Element baseElement, FrameContext frameContext) {
    super.bindModel(model, baseElement, frameContext);

    Template modelTemplate = model.getTemplate();
    if (!templateEquals(modelTemplate, template)) {
      throw new IllegalArgumentException("Model template does not match adapter template");
    }

    BindingContext bindingContext = model.getBindingContext();
    FrameContext localFrameContext = frameContext.bindTemplate(modelTemplate, bindingContext);
    getChildAdapter().bindModel(modelTemplate.getElement(), localFrameContext);
  }

  @Override
  public void onUnbindModel() {
    if (childAdapter != null) {
      childAdapter.unbindModel();
    }
  }

  @Override
  public void onReleaseAdapter() {
    // Because we recycle templates as a unit, we don't actually want to do anything here - we want
    // to keep the existing layout and styles so we can quickly re-bind content to the template.
  }

  private static View createView(Context context) {
    // This is just a dummy view; should never actually be referenced.
    return new View(context);
  }

  @Override
  public View getView() {
    return getChildAdapter().getView();
  }

  @Override
  protected View getBaseView() {
    return getChildAdapter().getBaseView();
  }

  @Override
  GravityVertical getVerticalGravity() {
    return getChildAdapter().getVerticalGravity();
  }

  @Override
  GravityHorizontal getHorizontalGravity() {
    return getChildAdapter().getHorizontalGravity();
  }

  @Override
  int getComputedWidthPx() {
    return getChildAdapter().getComputedWidthPx();
  }

  @Override
  int getComputedHeightPx() {
    return getChildAdapter().getComputedHeightPx();
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getTemplate().getElement().getStyleReferences();
  }

  @Override
  public StyleProvider getElementStyle() {
    return getChildAdapter().getElementStyle();
  }

  @Override
  public void triggerViewActions(View viewport) {
    getChildAdapter().triggerViewActions(viewport);
  }

  private ElementAdapter<?, ?> getChildAdapter() {
    return checkNotNull(childAdapter, "Child adapter is null; not created yet?");
  }

  private RecyclerKey createKey() {
    return new TemplateKey(
        checkNotNull(template, "Template is null; not created yet?"), pietSharedStates);
  }

  private void setLayoutParamsOnChild(ElementAdapter<?, ?> childAdapter) {
    int width = childAdapter.getComputedWidthPx();
    width = width == ElementAdapter.DIMENSION_NOT_SET ? LayoutParams.MATCH_PARENT : width;
    int height = childAdapter.getComputedHeightPx();
    height = height == ElementAdapter.DIMENSION_NOT_SET ? LayoutParams.WRAP_CONTENT : height;

    childAdapter.setLayoutParams(new LinearLayout.LayoutParams(width, height));
  }

  /**
   * Determines whether two templates are compatible for recycling. We're going to call the hash
   * code good enough for performance reasons (.equals() is expensive), and hope we don't get a lot
   * of collisions.
   */
  @SuppressWarnings("ReferenceEquality")
  static boolean templateEquals(/*@Nullable*/ Template template1, /*@Nullable*/ Template template2) {
    if (template1 == template2) {
      return true;
    } else if (template1 == null || template2 == null) {
      return false;
    }
    return template1.hashCode() == template2.hashCode();
  }

  static class TemplateKeySupplier
      implements AdapterKeySupplier<TemplateInstanceAdapter, TemplateAdapterModel> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public TemplateInstanceAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new TemplateInstanceAdapter(context, parameters);
    }

    @Override
    public RecyclerKey getKey(FrameContext frameContext, TemplateAdapterModel model) {
      return new TemplateKey(model.getTemplate(), frameContext.getPietSharedStates());
    }
  }

  /** Wrap the Template proto object as the recycler key. */
  static class TemplateKey extends RecyclerKey {
    private final Template template;
    /*@Nullable*/ private final List<PietSharedState> pietSharedStates;

    TemplateKey(Template template, /*@Nullable*/ List<PietSharedState> pietSharedStates) {
      this.template = template;
      this.pietSharedStates = pietSharedStates;
    }

    /** Equals checks the hashCode of template and sharedState to avoid expensive proto equals. */
    @SuppressWarnings("ReferenceEquality")
    @Override
    public boolean equals(/*@Nullable*/ Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TemplateKey that = (TemplateKey) o;

      if (!templateEquals(template, that.template)) {
        return false;
      } else if (that.pietSharedStates == null || this.pietSharedStates == null) {
        return this.pietSharedStates == that.pietSharedStates;
      } else {
        return this.pietSharedStates.size() == that.pietSharedStates.size()
            && this.pietSharedStates.hashCode() == that.pietSharedStates.hashCode();
      }
    }

    @Override
    public int hashCode() {
      int result = template.hashCode();
      result = 31 * result + (pietSharedStates != null ? pietSharedStates.hashCode() : 0);
      return result;
    }
  }

  static class TemplateAdapterModel {
    private final Template template;
    private final BindingContext bindingContext;

    TemplateAdapterModel(Template template, BindingContext bindingContext) {
      this.template = template;
      this.bindingContext = bindingContext;
    }

    TemplateAdapterModel(
        String templateId, FrameContext frameContext, BindingContext bindingContext) {
      this.template =
          checkNotNull(
              frameContext.getTemplate(templateId), "Template was not found: %s", templateId);
      this.bindingContext = bindingContext;
    }

    public Template getTemplate() {
      return template;
    }

    BindingContext getBindingContext() {
      return bindingContext;
    }

    @SuppressWarnings("ReferenceEquality")
    @Override
    public boolean equals(/*@Nullable*/ Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TemplateAdapterModel that = (TemplateAdapterModel) o;

      return templateEquals(template, that.template)
          && (bindingContext == that.bindingContext
              || (bindingContext.getBindingValuesCount()
                      == that.bindingContext.getBindingValuesCount()
                  && bindingContext.hashCode() == that.bindingContext.hashCode()));
    }

    @Override
    public int hashCode() {
      int result = template.hashCode();
      result = 31 * result + bindingContext.hashCode();
      return result;
    }
  }
}
