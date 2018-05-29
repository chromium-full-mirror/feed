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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ZeroStateViewHolder}. */
@RunWith(RobolectricTestRunner.class)
public class ZeroStateViewHolderTest {

  private ZeroStateViewHolder zeroStateViewHolder;
  private FrameLayout frameLayout;
  @Mock private OnClickListener onClickListener;

  @Before
  public void setup() {
    initMocks(this);
    Context context = Robolectric.setupActivity(Activity.class);
    frameLayout = new FrameLayout(context);
    zeroStateViewHolder = new ZeroStateViewHolder(context, frameLayout);
  }

  @Test
  public void testZeroStateViewInflated() {
    View view = frameLayout.findViewById(R.id.zero_state);
    assertThat(view).isNotNull();
    assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void testBind_setsOnClickListener() {
    zeroStateViewHolder.bind(onClickListener);

    View actionButton = frameLayout.findViewById(R.id.action_button);
    actionButton.performClick();

    verify(onClickListener).onClick(actionButton);
  }

  @Test
  public void testUnbind() {
    zeroStateViewHolder.bind(onClickListener);

    View actionButton = frameLayout.findViewById(R.id.action_button);
    assertThat(actionButton.hasOnClickListeners()).isTrue();
    assertThat(actionButton.isClickable()).isTrue();

    zeroStateViewHolder.unbind();

    assertThat(actionButton.hasOnClickListeners()).isFalse();
    assertThat(actionButton.isClickable()).isFalse();
  }
}
