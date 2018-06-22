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
import static com.google.android.libraries.feed.common.Validators.checkState;

import android.view.View;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import java.util.List;

/**
 * Methods to enable creation and binding of Templates.
 *
 * <p>This class creates and binds adapters for templates allowing for reuse of full template
 * layouts as a unit. Release and recycling is handled by the ElementAdapterFactory.
 */
class TemplateBinder {

  private final KeyedRecyclerPool<ElementAdapter<? extends View, ?>> templateRecyclerPool;
  private final ElementAdapterFactory adapterFactory;

  TemplateBinder(
      KeyedRecyclerPool<ElementAdapter<? extends View, ?>> templateRecyclerPool,
      ElementAdapterFactory adapterFactory) {
    this.templateRecyclerPool = templateRecyclerPool;
    this.adapterFactory = adapterFactory;
  }

  /** Higher-performance method that creates and binds an adapter for a template all at once. */
  ElementAdapter<? extends View, ?> createAndBindTemplateAdapter(
      TemplateAdapterModel model, FrameContext frameContext) {
    // Create the template context for use in both create and bind
    // This is a modestly expensive operation, and calling createTemplateAdapter followed by
    // bindTemplateAdapter would do it twice.
    FrameContext templateContext =
        frameContext.createTemplateContext(model.getTemplate(), model.getBindingContext());

    TemplateKey templateKey =
        new TemplateKey(model.getTemplate(), frameContext.getPietSharedStates());
    ElementAdapter<? extends View, ?> adapter = templateRecyclerPool.get(templateKey);

    if (adapter == null) {
      // Create new adapter
      adapter =
          adapterFactory.createAdapterForElement(model.getTemplate().getElement(), templateContext);

      adapter.setKey(templateKey);
    }

    adapter.bindModel(model.getTemplate().getElement(), templateContext);

    return adapter;
  }

  ElementAdapter<? extends View, ?> createTemplateAdapter(
      TemplateAdapterModel model, FrameContext frameContext) {
    TemplateKey templateKey =
        new TemplateKey(model.getTemplate(), frameContext.getPietSharedStates());
    ElementAdapter<? extends View, ?> adapter = templateRecyclerPool.get(templateKey);

    if (adapter == null) {
      // Make new FrameContext here
      FrameContext templateContext =
          frameContext.createTemplateContext(model.getTemplate(), model.getBindingContext());

      // Create new adapter
      adapter =
          adapterFactory.createAdapterForElement(model.getTemplate().getElement(), templateContext);

      adapter.setKey(templateKey);
    }

    return adapter;
  }

  void bindTemplateAdapter(
      ElementAdapter<? extends View, ?> adapter,
      TemplateAdapterModel model,
      FrameContext frameContext) {
    TemplateKey templateKey =
        new TemplateKey(model.getTemplate(), frameContext.getPietSharedStates());

    checkNotNull(adapter.getKey(), "Adapter key was null; not initialized correctly?");
    checkState(
        adapter.getKey() instanceof TemplateKey,
        "bindTemplateAdapter only applicable for template adapters");
    checkState(templateKey.equals(adapter.getKey()), "Template keys did not match");

    FrameContext templateContext =
        frameContext.createTemplateContext(model.getTemplate(), model.getBindingContext());

    adapter.bindModel(model.getTemplate().getElement(), templateContext);
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
