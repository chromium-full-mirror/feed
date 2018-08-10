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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.basicstream.internal.viewholders.HeaderViewHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link HeaderDriver}. */
@RunWith(RobolectricTestRunner.class)
public class HeaderDriverTest {

  @Mock private HeaderViewHolder headerViewHolder;

  private View header;
  private HeaderDriver headerDriver;

  @Before
  public void setup() {
    initMocks(this);
    Context context = Robolectric.setupActivity(Activity.class);
    header = new FrameLayout(context);
    headerDriver = new HeaderDriver(header);
  }

  @Test
  public void testBind() {
    assertThat(headerDriver.isBound()).isFalse();

    headerDriver.bind(headerViewHolder);

    assertThat(headerDriver.isBound()).isTrue();
    verify(headerViewHolder).bind(header);
  }

  @Test
  public void testUnbind() {
    headerDriver.bind(headerViewHolder);
    assertThat(headerDriver.isBound()).isTrue();

    headerDriver.unbind();

    assertThat(headerDriver.isBound()).isFalse();
    verify(headerViewHolder).unbind();
  }

  @Test
  public void testUnbind_doesNotCallUnbindIfNotBound() {
    assertThat(headerDriver.isBound()).isFalse();

    headerDriver.unbind();
    verifyNoMoreInteractions(headerViewHolder);
  }
}
