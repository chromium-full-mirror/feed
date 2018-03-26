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
import com.google.android.libraries.feed.host.stream.StreamConfiguration;

/** A default stream configuration which will work for most hosts. */
public class DefaultStreamConfiguration implements StreamConfiguration {

  private final Context context;

  public DefaultStreamConfiguration(Context context) {
    this.context = context;
  }

  @Override
  public int getPaddingStart() {
    return context.getResources().getDimensionPixelOffset(R.dimen.side_padding);
  }

  @Override
  public int getPaddingEnd() {
    return context.getResources().getDimensionPixelOffset(R.dimen.side_padding);
  }

  @Override
  public int getPaddingTop() {
    return context.getResources().getDimensionPixelOffset(R.dimen.vertical_padding);
  }

  @Override
  public int getPaddingBottom() {
    return context.getResources().getDimensionPixelOffset(R.dimen.vertical_padding);
  }
}
