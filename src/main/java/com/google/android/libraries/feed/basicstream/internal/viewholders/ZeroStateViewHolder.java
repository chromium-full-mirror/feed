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

package com.google.android.libraries.feed.basicstream.internal.viewholders;

import android.content.Context;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

/** {@link ViewHolder} for zero state. */
public class ZeroStateViewHolder extends FeedViewHolder {
  private final View view;
  private final View actionButton;

  public ZeroStateViewHolder(Context context, FrameLayout frameLayout) {
    super(frameLayout);
    view = LayoutInflater.from(context).inflate(R.layout.zero_state, frameLayout);
    actionButton = view.findViewById(R.id.action_button);
  }

  public void bind(OnClickListener onClickListener) {
    actionButton.setOnClickListener(onClickListener);
  }

  @Override
  public void unbind() {
    // Clear OnClickListener to null to allow for GC.
    actionButton.setOnClickListener(null);

    // Set clickable to false as setting OnClickListener to null sets clickable to true.
    actionButton.setClickable(false);
  }
}
