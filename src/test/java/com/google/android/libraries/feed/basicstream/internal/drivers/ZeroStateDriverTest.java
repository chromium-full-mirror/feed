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
import android.widget.FrameLayout;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ZeroStateViewHolder;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ZeroStateDriver}. */
@RunWith(RobolectricTestRunner.class)
public class ZeroStateDriverTest {

  @Mock private ModelProvider modelProvider;
  @Mock private ZeroStateViewHolder zeroStateViewHolder;

  private static final int HOUR_OF_DAY = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  private ZeroStateDriver zeroStateDriver;
  private Context context;

  @Before
  public void setup() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);

    zeroStateDriver =
        new ZeroStateDriver(modelProvider) {
          @Override
          int getHourOfDay() {
            return HOUR_OF_DAY;
          }
        };
  }

  @Test
  public void testBind() {
    assertThat(zeroStateDriver.isBound()).isFalse();

    zeroStateDriver.bind(zeroStateViewHolder);

    assertThat(zeroStateDriver.isBound()).isTrue();
    verify(zeroStateViewHolder).bind(zeroStateDriver, HOUR_OF_DAY);
  }

  @Test
  public void testUnbind() {
    zeroStateDriver.bind(zeroStateViewHolder);
    assertThat(zeroStateDriver.isBound()).isTrue();

    zeroStateDriver.unbind();

    assertThat(zeroStateDriver.isBound()).isFalse();
    verify(zeroStateViewHolder).unbind();
  }

  @Test
  public void testUnbind_doesNotCallUnbindIfNotBound() {
    assertThat(zeroStateDriver.isBound()).isFalse();

    zeroStateDriver.unbind();
    verifyNoMoreInteractions(zeroStateViewHolder);
  }

  @Test
  public void testOnClick() {
    zeroStateDriver.bind(zeroStateViewHolder);

    zeroStateDriver.onClick(new FrameLayout(context));

    verify(modelProvider).triggerRefresh();
  }
}
