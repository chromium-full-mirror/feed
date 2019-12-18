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

package com.google.android.libraries.feed.common.ui;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link LayoutUtils}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.JELLY_BEAN_MR1)
public class LayoutUtilsTest {
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void testDpToPx() {
    // TODO: Switch this to using @Config to set density to something different than 1.
    assertThat(LayoutUtils.dpToPx(1000.0f, context)).isWithin(1.0e-04f).of(1000.0f);
  }

  @Test
  public void testPxToDp() {
    // TODO: Switch this to using @Config to set density to something different than 1.
    assertThat(LayoutUtils.pxToDp(1000.0f, context)).isWithin(1.0e-04f).of(1000.0f);
  }

  @Test
  public void testSpToPx() {
    context.getResources().getDisplayMetrics().scaledDensity = 3.0f;
    assertThat(LayoutUtils.spToPx(1000.0f, context)).isWithin(1.0e-03f).of(3000.0f);
  }

  @Test
  public void testPxToSp() {
    context.getResources().getDisplayMetrics().scaledDensity = 3.0f;
    assertThat(LayoutUtils.pxToSp(3000.0f, context)).isWithin(1.0e-04f).of(1000.0f);
  }
}
