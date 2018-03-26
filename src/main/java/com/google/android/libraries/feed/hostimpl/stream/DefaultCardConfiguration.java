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

package com.google.android.libraries.feed.hostimpl.stream;

import android.content.Context;
import com.google.android.libraries.feed.host.stream.CardConfiguration;

/** A default card configuration which will work for most hosts. */
public class DefaultCardConfiguration implements CardConfiguration {

  private final Context context;

  public DefaultCardConfiguration(Context context) {
    this.context = context;
  }

  @Override
  public int getDefaultCornerRadius() {
    return context.getResources().getDimensionPixelSize(R.dimen.rounded_corner_radius);
  }

  @Override
  public float getDefaultCardElevation() {
    return context.getResources().getDimension(R.dimen.card_elevation);
  }

  @Override
  public float getMaxDefaultCardElevation() {
    return context.getResources().getDimension(R.dimen.card_elevation_large);
  }

  @Override
  public float getCardBottomPadding() {
    return context.getResources().getDimension(R.dimen.card_bottom_margin);
  }
}
