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
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/** Container adapter that controls a ViewGroup of other elements. */
public abstract class ViewGroupBoundViewAdapter<V extends ViewGroup, M, K>
    extends ElementAdapter<V, M, K> {

  @VisibleForTesting final List<ElementAdapter<? extends View, ?, ?>> childAdapters;

  ViewGroupBoundViewAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
    childAdapters = new ArrayList<>();
  }

  protected void initialBind() {
    if (getBaseView() != null) {
      return;
    }
    initializeAdapter(createViewGroup(), createKey());
  }

  /** Unbind the model and release child adapters. Be sure to call this in any overrides. */
  @Override
  public void onUnbindModel() {
    if (getBaseView() != null) {
      getBaseView().removeAllViews();
    }
    releaseChildren();
    childAdapters.clear();
    super.onUnbindModel();
  }

  protected void releaseChildren() {
    for (ElementAdapter<?, ?, ?> childAdapter : childAdapters) {
      getParameters().adapterFactory.releaseAdapter(childAdapter);
    }
  }

  protected void addChildAdapter(ElementAdapter<? extends View, ?, ?> adapter) {
    childAdapters.add(adapter);
  }

  /** Creates the base ViewGroup for this adapter. */
  protected abstract V createViewGroup();

  /** Creates a recycler pool key for this adapter based on the model (or returns SINGLETON_KEY). */
  protected abstract K createKey();
}
