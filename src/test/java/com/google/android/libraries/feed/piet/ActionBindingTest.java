// Copyright 2019 The Feed Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowView;

/** Tests of {@link ActionBinding}. */
@RunWith(RobolectricTestRunner.class)
public final class ActionBindingTest {
  private final Context context = Robolectric.buildActivity(Activity.class).get();
  private final View view = new View(context);
  @Mock private Drawable.Callback callback;

  @Mock private OnClickListener mockClickListener;
  @Mock private OnLongClickListener mockLongClickListener;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testUnbind() {
    view.setForeground(new ColorDrawable(12345));
    view.setOnClickListener(mockClickListener);
    view.setOnLongClickListener(mockLongClickListener);
    ActionBinding actionBinding = new ActionBinding(view, callback);

    actionBinding.unbind();

    ShadowView shadowView = Shadow.extract(view);
    assertThat(shadowView.getOnClickListener()).isNull();
    assertThat(shadowView.getOnLongClickListener()).isNull();
    assertThat(view.getForeground()).isNull();
    assertThat(view.isClickable()).isFalse();
    assertThat(view.isLongClickable()).isFalse();
  }
}
