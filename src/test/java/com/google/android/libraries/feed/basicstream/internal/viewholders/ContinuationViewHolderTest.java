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
import com.google.android.libraries.feed.basicstream.internal.logging.LoggingListener;
import com.google.android.libraries.feed.basicstream.internal.logging.VisibilityMonitor;
import com.google.android.libraries.feed.host.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContinuationViewHolder}. */
@RunWith(RobolectricTestRunner.class)
public class ContinuationViewHolderTest {
  private static final Configuration CONFIGURATION = new Configuration.Builder().build();

  private ContinuationViewHolder continuationViewHolder;
  private Context context;
  private FrameLayout frameLayout;

  @Mock private OnClickListener onClickListener;
  @Mock private LoggingListener loggingListener;
  @Mock private VisibilityMonitor visibilityMonitor;

  @Before
  public void setup() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    frameLayout = new FrameLayout(context);
    continuationViewHolder =
        new ContinuationViewHolder(CONFIGURATION, context, frameLayout) {
          @Override
          VisibilityMonitor createVisibilityMonitor(View view, Configuration configuration) {
            return visibilityMonitor;
          }
        };
  }

  @Test
  public void testConstructorInflatesView() {
    assertThat((View) frameLayout.findViewById(R.id.more_button)).isNotNull();
  }

  @Test
  public void testClick() {
    continuationViewHolder.bind(onClickListener, loggingListener, /* showSpinner= */ false);

    View buttonView = frameLayout.findViewById(R.id.action_button);
    buttonView.performClick();

    verify(onClickListener).onClick(buttonView);
    verify(loggingListener).onContentClicked();
  }

  @Test
  public void testBind_spinnerOff() {
    continuationViewHolder.bind(onClickListener, loggingListener, /* showSpinner= */ false);
    View buttonView = frameLayout.findViewById(R.id.action_button);
    View spinnerView = frameLayout.findViewById(R.id.loading_spinner);

    assertThat(buttonView.isClickable()).isTrue();
    assertThat(buttonView.getVisibility()).isEqualTo(View.VISIBLE);

    assertThat(spinnerView.getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void testBind_spinnerOn() {
    continuationViewHolder.bind(onClickListener, loggingListener, /* showSpinner= */ true);
    View buttonView = frameLayout.findViewById(R.id.action_button);
    View spinnerView = frameLayout.findViewById(R.id.loading_spinner);

    assertThat(buttonView.getVisibility()).isEqualTo(View.GONE);
    assertThat(spinnerView.getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void testBind_setsListener() {
    continuationViewHolder.bind(onClickListener, loggingListener, /* showSpinner= */ false);

    verify(visibilityMonitor).setListener(loggingListener);
  }

  @Test
  public void testUnbind() {
    continuationViewHolder.bind(onClickListener, loggingListener, /* showSpinner= */ false);
    continuationViewHolder.unbind();

    View view = frameLayout.findViewById(R.id.action_button);
    assertThat(view.isClickable()).isFalse();
    verify(visibilityMonitor).setListener(null);
  }
}
