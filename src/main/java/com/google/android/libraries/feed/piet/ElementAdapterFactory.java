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

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.FrameContext.ErrorType;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.ImagesProto.Image;

/** Provides methods to create various adapter types based on bindings. */
class ElementAdapterFactory {
  private final AdapterFactory<CustomElementAdapter, CustomElement, CustomElementAdapter.Key>
      customElementFactory;
  private final AdapterFactory<TextElementAdapter, TextElement, TextElementAdapter.Key>
      textElementFactory;
  private final AdapterFactory<ImageElementAdapter, ImageElement, ImageElementAdapter.Key>
      imageElementFactory;
  private final AdapterFactory<SpacerElementAdapter, SpacerElement, SpacerElementAdapter.Key>
      spacerElementFactory;
  private final AdapterFactory<GridRowAdapter, GridRow, GridRowAdapter.Key> gridRowFactory;
  private final AdapterFactory<ElementListAdapter, ElementList, ElementListAdapter.Key>
      elementListFactory;
  private final AdapterFactory<
          TemplateInvocationAdapter, TemplateInvocation, TemplateInvocationAdapter.Key>
      templateInvocationFactory;

  ElementAdapterFactory(Context context, AdapterParameters parameters) {
    customElementFactory =
        new AdapterFactory<>(context, parameters, new CustomElementAdapter.KeySupplier());
    textElementFactory =
        new AdapterFactory<>(context, parameters, new TextElementAdapter.KeySupplier());
    imageElementFactory =
        new AdapterFactory<>(context, parameters, new ImageElementAdapter.KeySupplier());
    spacerElementFactory =
        new AdapterFactory<>(context, parameters, new SpacerElementAdapter.KeySupplier());
    gridRowFactory = new AdapterFactory<>(context, parameters, new GridRowAdapter.KeySupplier());
    elementListFactory =
        new AdapterFactory<>(context, parameters, new ElementListAdapter.KeySupplier());
    templateInvocationFactory =
        new AdapterFactory<>(context, parameters, new TemplateInvocationAdapter.KeySupplier());
  }

  /** Testing-only constructor for mocking the factories. */
  @VisibleForTesting
  ElementAdapterFactory(
      AdapterFactory<CustomElementAdapter, CustomElement, CustomElementAdapter.Key>
          customElementFactory,
      AdapterFactory<TextElementAdapter, TextElement, TextElementAdapter.Key> textElementFactory,
      AdapterFactory<ImageElementAdapter, ImageElement, ImageElementAdapter.Key>
          imageElementFactory,
      AdapterFactory<SpacerElementAdapter, SpacerElement, SpacerElementAdapter.Key>
          spacerElementFactory,
      AdapterFactory<GridRowAdapter, GridRow, GridRowAdapter.Key> gridRowFactory,
      AdapterFactory<ElementListAdapter, ElementList, ElementListAdapter.Key> elementListFactory,
      AdapterFactory<TemplateInvocationAdapter, TemplateInvocation, TemplateInvocationAdapter.Key>
          templateInvocationFactory) {
    this.customElementFactory = customElementFactory;
    this.textElementFactory = textElementFactory;
    this.imageElementFactory = imageElementFactory;
    this.spacerElementFactory = spacerElementFactory;
    this.gridRowFactory = gridRowFactory;
    this.elementListFactory = elementListFactory;
    this.templateInvocationFactory = templateInvocationFactory;
  }

  ElementAdapter<? extends View, ?, ?> createAdapterForElement(
      Element element, FrameContext frameContext) {
    switch (element.getElementsCase()) {
      case CUSTOM_ELEMENT:
        return customElementFactory.get(element.getCustomElement(), element, frameContext);
      case TEXT_ELEMENT:
        return textElementFactory.get(element.getTextElement(), element, frameContext);
      case IMAGE_ELEMENT:
        return imageElementFactory.get(element.getImageElement(), element, frameContext);
      case SPACER_ELEMENT:
        return spacerElementFactory.get(element.getSpacerElement(), element, frameContext);
      case GRID_ROW:
        return gridRowFactory.get(element.getGridRow(), element, frameContext);
      case ELEMENT_LIST:
        return elementListFactory.get(element.getElementList(), element, frameContext);
      case ELEMENT_LIST_BINDING:
        BindingValue elementListBindingValue =
            frameContext.getElementListBindingValue(element.getElementListBinding());
        // TODO: support the various bits of metadata in BindingValue
        return elementListFactory.get(
            elementListBindingValue.getElementList(), element, frameContext);
      case TEMPLATE_BINDING:
        TemplateInvocation templateInvocation =
            frameContext.getTemplateInvocationFromBinding(element.getTemplateBinding());
        if (templateInvocation != null) {
          return templateInvocationFactory.get(templateInvocation, element, frameContext);
        } else {
          frameContext.reportError(
              ErrorType.ERROR,
              String.format("Could not get template for %s", element.getTemplateBinding()));
          return templateInvocationFactory.get(
              TemplateInvocation.getDefaultInstance(), element, frameContext);
        }
      case ELEMENTS_NOT_SET:
      default:
        throw new IllegalArgumentException(
            String.format("Unsupported Element type: %s", element.getElementsCase()));
    }
  }

  ElementListAdapter createElementListAdapter(ElementList elementList, FrameContext frameContext) {
    return elementListFactory.get(elementList, frameContext);
  }

  TemplateInvocationAdapter createTemplateAdapter(
      TemplateInvocation templateInvocation, FrameContext frameContext) {
    return templateInvocationFactory.get(templateInvocation, frameContext);
  }

  void releaseAdapter(ElementAdapter<? extends View, ?, ?> adapter) {
    if (adapter instanceof CustomElementAdapter) {
      customElementFactory.release((CustomElementAdapter) adapter);
    } else if (adapter instanceof TextElementAdapter) {
      textElementFactory.release((TextElementAdapter) adapter);
    } else if (adapter instanceof ImageElementAdapter) {
      imageElementFactory.release((ImageElementAdapter) adapter);
    } else if (adapter instanceof SpacerElementAdapter) {
      spacerElementFactory.release((SpacerElementAdapter) adapter);
    } else if (adapter instanceof GridRowAdapter) {
      gridRowFactory.release((GridRowAdapter) adapter);
    } else if (adapter instanceof ElementListAdapter) {
      elementListFactory.release((ElementListAdapter) adapter);
    } else if (adapter instanceof TemplateInvocationAdapter) {
      templateInvocationFactory.release((TemplateInvocationAdapter) adapter);
    }
  }
}
