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

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import javax.annotation.Nullable;

/**
 * A BoundViewAdapter is a type of adapter binding Models to Views. It supports recycling the Views
 * on unbind.
 *
 * @param <M> The model. This is the model type which is bound to the view by the adapter.
 */
public interface BoundViewAdapter<M> {
  /**
   * Value returned by getComputed[Height/Width]Px when an explicit value for the dimension has not
   * been computed, and it should be set by the parent instead.
   */
  int DIMENSION_NOT_SET = -1;

  /**
   * Returns the View bound to the Adapter. This is not a valid call until the first Model has been
   * bound to the Adapter.
   */
  @Nullable
  View getView();

  /**
   * Sets up an adapter, but does not bind data. Once this is called, the {@link #getView()} and
   * {@link #getKey()} are legally called. This method must not be called if a Model is currently
   * bound to the Adapter, you must call {@link #unbindModel()}.
   */
  void createAdapter(M model, FrameContext frameContext);

  /** Binds data from a Model to the Adapter, without changing child views or styles. */
  void bindModel(M model, FrameContext frameContext);

  /**
   * Unbinds the current Model from the Adapter. The Adapter will unbind all contained Adapters
   * recursively.
   */
  void unbindModel();

  /** Unbinds the current Model, and releases all child adapters to recycler pools. */
  void releaseAdapter();

  /** Returns a Key which represents an instance of a Model based upon a type. */
  RecyclerKey getKey();

  /** Sets the layout parameters on this adapter's view. */
  void setLayoutParams(LayoutParams layoutParams);

  /**
   * Returns the desired width of the adapter, computed from the model at bind time, or {@link
   * #DIMENSION_NOT_SET} which implies that the parent can choose the width.
   */
  int getComputedWidthPx();

  /**
   * Returns the desired height of the adapter, computed from the model at bind time, or {@link
   * #DIMENSION_NOT_SET} which implies that the parent can choose the height.
   */
  int getComputedHeightPx();
}
