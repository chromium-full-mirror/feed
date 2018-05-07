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

package com.google.android.libraries.feed.basicstream.internal.contentmodels.testing;

import android.support.v7.widget.RecyclerView.ViewHolder;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;

/** Fake for {@link ContentModel}. */
public class FakeContentModel implements ContentModel {

  @ViewHolderType private final int viewHolderType;

  private FakeContentModel(@ViewHolderType int viewHolderType) {
    this.viewHolderType = viewHolderType;
  }

  @Override
  public void bind(ViewHolder viewHolder) {}

  @Override
  public void unbind(ViewHolder viewHolder) {}

  @Override
  public int itemId() {
    return hashCode();
  }

  @Override
  public int getItemViewType() {
    return viewHolderType;
  }

  public static class Builder {
    @ViewHolderType private int viewHolderType = ViewHolderType.TYPE_CARD;

    public Builder setItemViewType(@ViewHolderType int viewHolderType) {
      this.viewHolderType = viewHolderType;
      return this;
    }

    public FakeContentModel build() {
      return new FakeContentModel(viewHolderType);
    }
  }
}
