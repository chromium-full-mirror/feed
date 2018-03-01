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
import android.view.View;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.CustomElementData;
import com.google.search.now.ui.piet.ElementsProto.Element;

/** Adapter that manages a custom view created by the host. */
public class CustomElementAdapter extends ElementAdapter<View, CustomElement> {
  private static final String TAG = "CustomElementAdapter";

  public CustomElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected CustomElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasCustomElement()) {
      throw new IllegalArgumentException(
          String.format("Missing CustomElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getCustomElement();
  }

  @Override
  public void onBindModel(
      CustomElement customElement, Element baseElement, FrameContext frameContext) {
    CustomElementData customElementData;
    switch (customElement.getContentCase()) {
      case CUSTOM_ELEMENT_DATA:
        customElementData = customElement.getCustomElementData();
        break;
      case CUSTOM_BINDING:
        customElementData =
            frameContext.getCustomElementFromBinding(customElement.getCustomBinding());
        break;
      default:
        Logger.e(TAG, "Missing payload in CustomElement");
        return;
    }

    View v = frameContext.getCustomElementProvider().createCustomElement(customElementData);
    initializeAdapter(v, KeySupplier.SINGLETON_KEY);
  }

  @Override
  public void onUnbindModel() {
    if (getView() != null) {
      getFrameContext().getCustomElementProvider().releaseCustomView(getView());
    }
  }

  static final class Key {}

  static class KeySupplier extends SingletonKeySupplier<CustomElementAdapter, CustomElement> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public CustomElementAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new CustomElementAdapter(context, parameters);
    }
  }
}
