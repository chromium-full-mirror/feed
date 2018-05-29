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
import android.view.View;
import android.view.ViewGroup;
import com.google.android.libraries.feed.piet.TemplateInstanceAdapter.TemplateAdapterModel;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.BindingValue.Visibility;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.PietProto.Template;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for adapters that act as containers for other adapters, such as ElementList and
 * GridRow. Ensures that lifecycle methods are called on child adapters when the parent class binds,
 * unbinds, or releases.
 */
abstract class ElementContainerAdapter<V extends ViewGroup, M> extends ElementAdapter<V, M> {

  @VisibleForTesting final List<ElementAdapter<? extends View, ?>> childAdapters;
  @VisibleForTesting int[] adaptersPerContent = new int[0];

  /** How many views come in the base view before the first adapter's view; used for gravity. */
  private int firstAdapterViewOffset = 0;

  /** Cached reference to the factory for convenience. */
  private final ElementAdapterFactory factory;

  ElementContainerAdapter(Context context, AdapterParameters parameters, V view, RecyclerKey key) {
    super(context, parameters, view, key);
    childAdapters = new ArrayList<>();
    factory = parameters.elementAdapterFactory;
  }

  ElementContainerAdapter(Context context, AdapterParameters parameters, V view) {
    super(context, parameters, view);
    childAdapters = new ArrayList<>();
    factory = parameters.elementAdapterFactory;
  }

  /** Unbind the model and release child adapters. Be sure to call this in any overrides. */
  @Override
  void onUnbindModel() {
    for (ElementAdapter<?, ?> childAdapter : childAdapters) {
      childAdapter.unbindModel();
    }
    super.onUnbindModel();
  }

  @Override
  void onReleaseAdapter() {
    V containerView = getBaseView();
    if (containerView != null) {
      containerView.removeAllViews();
    }
    for (ElementAdapter<?, ?> childAdapter : childAdapters) {
      factory.releaseAdapter(childAdapter);
    }
    childAdapters.clear();
    setFirstAdapterViewOffset(0);
  }

  /** Creates and adds adapters for all inline Content items. */
  void createInlineChildAdapters(List<Content> contents, FrameContext frameContext) {
    adaptersPerContent = new int[contents.size()];

    checkState(
        childAdapters.isEmpty(),
        "Child adapters is not empty (has %s elements); release adapter before creating.",
        childAdapters.size());
    // Could also check that getBaseView has no children, but it may have children due to alignment
    // padding elements.

    for (int contentIndex = 0; contentIndex < contents.size(); contentIndex++) {
      Content content = contents.get(contentIndex);
      switch (content.getContentTypeCase()) {
        case ELEMENT:
          adaptersPerContent[contentIndex] =
              createAndAddElementAdapter(content.getElement(), frameContext);
          break;
        case TEMPLATE_INVOCATION:
          adaptersPerContent[contentIndex] =
              createAndAddTemplateAdapters(content.getTemplateInvocation(), frameContext);
          break;
        case BOUND_ELEMENT:
        case TEMPLATE_BINDING:
          // Do nothing; create these adapters in the bindModel call.
          adaptersPerContent[contentIndex] = 0;
          continue;
        default:
          throw new IllegalArgumentException(
              String.format("Unhandled Content type: %s", content.getContentTypeCase()));
      }
    }
  }

  /**
   * Create an adapter for the element and add it to this container's layout.
   *
   * @return number of adapters created
   */
  private int createAndAddElementAdapter(Element element, FrameContext frameContext) {
    ElementAdapter<? extends View, ?> adapter =
        factory.createAdapterForElement(element, frameContext);
    adapter.createAdapter(element, frameContext);
    addChildAdapter(adapter);
    getBaseView().addView(adapter.getView());
    return 1;
  }

  /**
   * Create adapters for the template invocation and add them to this container's layout.
   *
   * @return number of adapters created
   */
  private int createAndAddTemplateAdapters(
      TemplateInvocation templateInvocation, FrameContext frameContext) {
    Template template = frameContext.getTemplate(templateInvocation.getTemplateId());
    if (template == null) {
      return 0;
    }
    for (int templateIndex = 0;
        templateIndex < templateInvocation.getBindingContextsCount();
        templateIndex++) {
      TemplateAdapterModel templateModel =
          new TemplateAdapterModel(template, templateInvocation.getBindingContexts(templateIndex));
      TemplateInstanceAdapter templateAdapter =
          factory.createTemplateAdapter(templateModel, frameContext);
      templateAdapter.createAdapter(templateModel, frameContext);
      addChildAdapter(templateAdapter);
      getBaseView().addView(templateAdapter.getView());
    }
    return templateInvocation.getBindingContextsCount();
  }

  /**
   * Tells this adapter that there exist (non-adapter) views in the ViewGroup that come before the
   * first adapter view. (ex. spacer views to allow filling parent + gravity in LinearLayouts)
   */
  void setFirstAdapterViewOffset(int firstAdapterViewOffset) {
    this.firstAdapterViewOffset = firstAdapterViewOffset;
  }

  int getFirstAdapterViewOffset() {
    return firstAdapterViewOffset;
  }

  /**
   * Binds all static adapters, and creates+binds+adds adapters for Content bindings.
   *
   * <p>There are a few inefficient O(N^2) mid-list inserts in this method, but since we're dealing
   * with views here, N should always be small (probably less than ~10).
   */
  void bindChildAdapters(List<Content> contents, FrameContext frameContext) {
    checkState(
        adaptersPerContent.length == contents.size(),
        "Internal error in adapters per content (%s != %s). Adapter has not been created?",
        adaptersPerContent.length,
        contents.size());
    int adapterIndex = 0;
    int viewIndex = firstAdapterViewOffset;
    for (int contentIndex = 0; contentIndex < contents.size(); contentIndex++) {
      Content content = contents.get(contentIndex);
      switch (content.getContentTypeCase()) {
        case ELEMENT:
          // An Element generates exactly one adapter+view; bind it here.
          childAdapters.get(adapterIndex).bindModel(content.getElement(), frameContext);
          adapterIndex++;
          viewIndex++;
          break;
        case TEMPLATE_INVOCATION:
          // Bind one TemplateInstanceAdapter for each BindingContext.
          TemplateInvocation templateInvocation = content.getTemplateInvocation();
          Template template = frameContext.getTemplate(templateInvocation.getTemplateId());
          if (template == null) {
            continue;
          }
          for (int templateIndex = 0;
              templateIndex < adaptersPerContent[contentIndex];
              templateIndex++) {
            TemplateInstanceAdapter templateAdapter =
                (TemplateInstanceAdapter) childAdapters.get(adapterIndex);
            templateAdapter.bindModel(
                new TemplateAdapterModel(
                    template, templateInvocation.getBindingContexts(templateIndex)),
                frameContext);
            adapterIndex++;
            viewIndex++;
          }
          break;
        case BOUND_ELEMENT:
          // Look up the binding, then create, bind, and add a single adapter.
          BindingValue elementBinding =
              frameContext.getElementBindingValue(content.getBoundElement());
          if (!elementBinding.hasElement() || elementBinding.getVisibility() == Visibility.GONE) {
            continue;
          }
          Element element = elementBinding.getElement();
          ElementAdapter<? extends View, ?> adapter =
              factory.createAdapterForElement(element, frameContext);
          adapter.createAdapter(element, frameContext);
          adapter.bindModel(element, frameContext);
          childAdapters.add(adapterIndex++, adapter);
          getBaseView().addView(adapter.getView(), viewIndex++);
          adaptersPerContent[contentIndex] = 1;
          break;
        case TEMPLATE_BINDING:
          // Look up the binding, then create, bind, and add template adapters.
          BindingValue templateBindingValue =
              frameContext.getTemplateInvocationBindingValue(content.getTemplateBinding());
          if (!templateBindingValue.hasTemplateInvocation()) {
            continue;
          }
          TemplateInvocation boundTemplateInvocation = templateBindingValue.getTemplateInvocation();
          Template boundTemplate =
              frameContext.getTemplate(boundTemplateInvocation.getTemplateId());
          if (boundTemplate == null) {
            continue;
          }
          adaptersPerContent[contentIndex] = boundTemplateInvocation.getBindingContextsCount();
          for (int templateIndex = 0;
              templateIndex < boundTemplateInvocation.getBindingContextsCount();
              templateIndex++) {
            TemplateAdapterModel templateModel =
                new TemplateAdapterModel(
                    boundTemplate, boundTemplateInvocation.getBindingContexts(templateIndex));
            TemplateInstanceAdapter boundTemplateAdapter =
                factory.createTemplateAdapter(templateModel, frameContext);
            boundTemplateAdapter.createAdapter(templateModel, frameContext);
            boundTemplateAdapter.bindModel(templateModel, frameContext);
            childAdapters.add(adapterIndex++, boundTemplateAdapter);
            getBaseView().addView(boundTemplateAdapter.getView(), viewIndex++);
          }
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unhandled Content type: %s", content.getContentTypeCase()));
      }
    }
  }

  /** Unbind all inline adapters, and destroy all bound adapters. */
  void unbindChildAdapters(List<Content> contents) {
    int adapterIndex = 0;
    int viewIndex = firstAdapterViewOffset;
    for (int contentIndex = 0; contentIndex < contents.size(); contentIndex++) {
      Content content = contents.get(contentIndex);
      switch (content.getContentTypeCase()) {
        case ELEMENT:
        case TEMPLATE_INVOCATION:
          // For inline content, just unbind to allow re-binding in the future.
          for (int i = 0; i < adaptersPerContent[contentIndex]; i++) {
            childAdapters.get(adapterIndex).unbindModel();
            adapterIndex++;
            viewIndex++;
          }
          break;
        case BOUND_ELEMENT:
        case TEMPLATE_BINDING:
          // For bound content, release, recycle, and remove adapters.
          for (int i = 0; i < adaptersPerContent[contentIndex]; i++) {
            factory.releaseAdapter(childAdapters.get(adapterIndex));
            childAdapters.remove(adapterIndex);
            getBaseView().removeViewAt(viewIndex);
            // Don't increment adapterIndex or viewIndex because we removed this adapter/view.
          }
          adaptersPerContent[contentIndex] = 0;
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unhandled Content type: %s", content.getContentTypeCase()));
      }
    }
  }

  /**
   * When we bind an item that has an ElementListBinding, the adapter hasn't been initialized yet so
   * we need to both create and bind it.
   */
  @Deprecated
  void createAndBindChildListAdapter(
      ElementListAdapter adapter,
      ElementListBindingRef listBindingRef,
      /*@Nullable*/ Element optionalBaseElement,
      FrameContext frameContext) {
    Element baseElement =
        optionalBaseElement == null ? Element.getDefaultInstance() : optionalBaseElement;
    BindingValue listBinding = frameContext.getElementListBindingValue(listBindingRef);
    adapter.setVisibility(listBinding.getVisibility());
    if (!listBinding.hasElementList()) {
      if (listBinding.getVisibility() == Visibility.GONE || listBindingRef.getIsOptional()) {
        adapter.setVisibility(Visibility.GONE);
        return;
      } else {
        throw new IllegalArgumentException(
            String.format("ElementList binding %s had no content", listBinding.getBindingId()));
      }
    }
    ElementList list = listBinding.getElementList();
    // If this is a re-bind, we assume this adapter has been released in onUnbindModel
    adapter.createAdapter(list, baseElement, frameContext);
    adapter.bindModel(list, baseElement, frameContext);
  }

  void addChildAdapter(ElementAdapter<? extends View, ?> adapter) {
    childAdapters.add(adapter);
  }

  @Override
  public void triggerViewActions(View viewport) {
    super.triggerViewActions(viewport);
    for (ElementAdapter<?, ?> childAdapter : childAdapters) {
      childAdapter.triggerViewActions(viewport);
    }
  }
}
