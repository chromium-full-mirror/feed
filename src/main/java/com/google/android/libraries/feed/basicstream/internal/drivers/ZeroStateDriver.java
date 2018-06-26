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
import android.view.View.OnClickListener;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ZeroStateViewHolder;
import com.google.android.libraries.feed.common.logging.Logger;
import java.util.Calendar;

/** {@link FeatureDriver} for the zero state. */
public class ZeroStateDriver extends LeafFeatureDriver implements OnClickListener {
  private static final String TAG = "ZeroStateDriver";
  private final ModelProvider modelProvider;
  private final ContentChangedListener contentChangedListener;
  private boolean spinnerShown;
  /*@Nullable*/ private ZeroStateViewHolder zeroStateViewHolder;

  ZeroStateDriver(
      ModelProvider modelProvider,
      ContentChangedListener contentChangedListener,
      boolean spinnerShown) {
    this.modelProvider = modelProvider;
    this.contentChangedListener = contentChangedListener;
    this.spinnerShown = spinnerShown;
  }

  @Override
  public void bind(FeedViewHolder viewHolder) {
    if (isBound()) {
      Logger.wtf(TAG, "Rebinding.");
    }
    checkState(viewHolder instanceof ZeroStateViewHolder);

    zeroStateViewHolder = (ZeroStateViewHolder) viewHolder;
    zeroStateViewHolder.bind(this, getHourOfDay(), spinnerShown);
  }

  @Override
  public int getItemViewType() {
    return ViewHolderType.TYPE_ZERO_STATE;
  }

  @Override
  public void unbind() {
    if (zeroStateViewHolder == null) {
      return;
    }

    zeroStateViewHolder.unbind();
    zeroStateViewHolder = null;
  }

  @Override
  public void onClick(View v) {
    if (zeroStateViewHolder == null) {
      Logger.wtf(TAG, "Calling onClick before binding.");
      return;
    }

    spinnerShown = true;
    zeroStateViewHolder.showSpinner(spinnerShown);
    contentChangedListener.onContentChanged();
    modelProvider.triggerRefresh();
  }

  @VisibleForTesting
  boolean isBound() {
    return zeroStateViewHolder != null;
  }

  @VisibleForTesting
  int getHourOfDay() {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  }

  @Override
  public void onDestroy() {}
}
