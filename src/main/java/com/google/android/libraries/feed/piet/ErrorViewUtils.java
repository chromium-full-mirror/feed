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
import android.support.v4.widget.TextViewCompat;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Utility class for creating an error view. */
class ErrorViewUtils {
  private static final int ERROR_PADDING = 4;
  private static final int ERROR_SIDE_PADDING = 16;
  private static final int ERROR_BACKGROUND_COLOR = 0xFFEF9A9A;
  private static final int ERROR_DIVIDER_COLOR = 0x65000000;

  @VisibleForTesting static final int ERROR_DIVIDER_WIDTH_DP = 1;

  private final Context context;

  ErrorViewUtils(Context context) {
    this.context = context;
  }

  /** Create a {@code View} containing all the errors. */
  View getErrorView(List<String> errors) {
    LinearLayout view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);
    LayoutParams layoutParams =
        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);
    view.setBackgroundColor(ERROR_BACKGROUND_COLOR);
    view.addView(getDivider());
    for (String error : errors) {
      view.addView(getErrorTextView(error));
    }
    return view;
  }

  private View getDivider() {
    View v = new View(context);
    LayoutParams layoutParams =
        new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, (int) ViewUtils.dpToPx(ERROR_DIVIDER_WIDTH_DP, context));
    v.setLayoutParams(layoutParams);
    v.setBackgroundColor(ERROR_DIVIDER_COLOR);
    return v;
  }

  private TextView getErrorTextView(String error) {
    TextView textView = new TextView(context);
    TextViewCompat.setTextAppearance(textView, R.style.gm_font_weight_regular);
    textView.setPadding(
        (int) ViewUtils.dpToPx(ERROR_SIDE_PADDING, context),
        (int) ViewUtils.dpToPx(ERROR_PADDING, context),
        (int) ViewUtils.dpToPx(ERROR_SIDE_PADDING, context),
        (int) ViewUtils.dpToPx(ERROR_PADDING, context));
    textView.setText(error);
    return textView;
  }
}
