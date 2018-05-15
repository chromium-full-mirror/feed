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

package com.google.android.libraries.feed.basicstream.internal.contentmodels;

import static com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType.TYPE_CONTINUATION;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View.OnClickListener;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;

/** {@link ContentModel} to render a more button in a {@link RecyclerView}. */
public class ContinuationContentModel implements ContentModel {

  private final OnClickListener onClickListener;
  private boolean paginateImmediately;
  private boolean spinnerShowing;
  /*@Nullable*/ private ContinuationViewHolder viewHolder;

  public ContinuationContentModel(OnClickListener onClickListener, boolean paginateImmediately) {
    this.onClickListener = onClickListener;
    this.paginateImmediately = paginateImmediately;

    // Spinner should be showing immediately if paginateImmediately is true.
    this.spinnerShowing = paginateImmediately;
  }

  @Override
  public void bind(ViewHolder viewHolder) {
    this.viewHolder = (ContinuationViewHolder) viewHolder;

    if (paginateImmediately) {
      onClickListener.onClick(viewHolder.itemView);
      paginateImmediately = false;
    }

    ((ContinuationViewHolder) viewHolder).bind(onClickListener, spinnerShowing);
  }

  @Override
  public void unbind(ViewHolder viewHolder) {
    ((ContinuationViewHolder) viewHolder).unbind();
    this.viewHolder = null;
  }

  @Override
  public int itemId() {
    return hashCode();
  }

  @Override
  public int getItemViewType() {
    return TYPE_CONTINUATION;
  }

  public void setShowSpinner(boolean showSpinner) {
    spinnerShowing = showSpinner;

    // If paginateImmediately was initialized to true, but before binding showSpinner is set to
    // false, pagination should not start on bind.
    paginateImmediately &= showSpinner;

    if (viewHolder != null) {
      viewHolder.setShowSpinner(showSpinner);
    }
  }
}
