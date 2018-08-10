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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.android.libraries.feed.common.Validators.checkState;

import android.support.annotation.VisibleForTesting;
import android.view.View;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.HeaderViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;
import com.google.android.libraries.feed.common.logging.Logger;

/** {@link FeatureDriver} for headers. */
public class HeaderDriver extends LeafFeatureDriver {

  private static final String TAG = "HeaderDriver";

  private final View header;
  /*@Nullable*/ private HeaderViewHolder headerViewHolder;

  public HeaderDriver(View header) {
    this.header = header;
  }

  @Override
  public void bind(FeedViewHolder viewHolder) {
    if (isBound()) {
      Logger.wtf(TAG, "Rebinding.");
      return;
    }

    checkState(viewHolder instanceof HeaderViewHolder);
    headerViewHolder = (HeaderViewHolder) viewHolder;
    headerViewHolder.bind(header);
  }

  @Override
  public void unbind() {
    if (headerViewHolder == null) {
      return;
    }

    headerViewHolder.unbind();
    headerViewHolder = null;
  }

  @Override
  public int getItemViewType() {
    return ViewHolderType.TYPE_HEADER;
  }

  @Override
  public void onDestroy() {}

  @VisibleForTesting
  boolean isBound() {
    return headerViewHolder != null;
  }
}
