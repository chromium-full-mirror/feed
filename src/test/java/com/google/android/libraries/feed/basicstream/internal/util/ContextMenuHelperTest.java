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

package com.google.android.libraries.feed.basicstream.internal.util;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.MeasureSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContextMenuHelper}. */
@RunWith(RobolectricTestRunner.class)
public class ContextMenuHelperTest {

  private Context context;

  @Before
  public void setup() {
    context = Robolectric.setupActivity(Activity.class);
  }

  @Test
  public void testGetHeight() {
    View view = new View(context);
    view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

    int top = 25;
    int bottom = 150;
    view.layout(0, top, 0, bottom);

    assertThat(new ContextMenuHelper(view).getStreamHeight()).isEqualTo(bottom - top);
  }
}
