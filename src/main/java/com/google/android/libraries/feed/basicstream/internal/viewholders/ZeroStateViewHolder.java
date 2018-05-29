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
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

/** {@link ViewHolder} for zero state. */
public class ZeroStateViewHolder extends FeedViewHolder {
  public static final int MORNING_HOUR_START = 0;
  public static final int AFTERNOON_HOUR_START = 12;
  public static final int EVENING_HOUR_START = 17;

  private final View view;
  private final View actionButton;
  private final TextView bodyText;

  public ZeroStateViewHolder(Context context, FrameLayout frameLayout) {
    super(frameLayout);
    view = LayoutInflater.from(context).inflate(R.layout.zero_state, frameLayout);
    actionButton = view.findViewById(R.id.action_button);
    bodyText = view.findViewById(R.id.body_text);
  }

  public void bind(OnClickListener onClickListener, int hour) {
    bodyText.setText(getBodyText(hour));
    actionButton.setOnClickListener(onClickListener);
  }

  @Override
  public void unbind() {
    // Clear OnClickListener to null to allow for GC.
    actionButton.setOnClickListener(null);

    // Set clickable to false as setting OnClickListener to null sets clickable to true.
    actionButton.setClickable(false);
  }

  @StringRes
  private int getBodyText(int hour) {
    if (hour >= MORNING_HOUR_START && hour < AFTERNOON_HOUR_START) {
      return R.string.ntp_all_dismissed_body_text_morning;
    } else if (hour >= AFTERNOON_HOUR_START && hour < EVENING_HOUR_START) {
      return R.string.ntp_all_dismissed_body_text_afternoon;
    } else {
      return R.string.ntp_all_dismissed_body_text_evening;
    }
  }
}
