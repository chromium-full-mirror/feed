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
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for adapters that act as containers for other adapters, such as ElementList and
 * GridRow. Ensures that lifecycle methods are called on child adapters when the parent class binds,
 * unbinds, or releases.
 */
abstract class ElementContainerAdapter<
        V extends ViewGroup, ChildAdapterT extends ElementAdapter<? extends View, ?>, M>
    extends ElementAdapter<V, M> {

  @VisibleForTesting final List<ChildAdapterT> childAdapters;

  ElementContainerAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
    childAdapters = new ArrayList<>();
  }

  void initialCreate() {
    if (getBaseView() != null) {
      return;
    }
    initializeAdapter(createViewGroup(), createKey());
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
    if (getBaseView() != null) {
      getBaseView().removeAllViews();
    }
    for (ElementAdapter<?, ?> childAdapter : childAdapters) {
      getParameters().elementAdapterFactory.releaseAdapter(childAdapter);
    }
    childAdapters.clear();
  }

  void addChildAdapter(ChildAdapterT adapter) {
    childAdapters.add(adapter);
  }

  /** Creates the base ViewGroup for this adapter. */
  abstract V createViewGroup();

  /**
   * Creates a {@link RecyclerKey} for this adapter based on the model (or returns SINGLETON_KEY if
   * all adapters of this type are compatible for recycling).
   */
  RecyclerKey createKey() {
    return SingletonKeySupplier.SINGLETON_KEY;
  }
}
