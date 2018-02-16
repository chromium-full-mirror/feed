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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link ErrorViewUtils}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class ErrorViewUtilsTest {
  private static final String ERROR_TEXT_1 = "Interdimensional rift formation.";
  private static final String ERROR_TEXT_2 = "Exotic particle containment breach.";

  @Mock private FrameContext frameContext;

  private Context context;

  private ErrorViewUtils errorViewUtils;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    errorViewUtils = new ErrorViewUtils(context);
  }

  @Test
  public void testGetErrorView_singleError() {
    List<String> errors = new ArrayList<>();
    errors.add(ERROR_TEXT_1);

    View errorView = errorViewUtils.getErrorView(errors);

    assertThat(errorView).isInstanceOf(LinearLayout.class);
    LinearLayout listErrorView = (LinearLayout) errorView;
    assertThat(listErrorView.getOrientation()).isEqualTo(LinearLayout.VERTICAL);
    assertThat(listErrorView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(listErrorView.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);

    assertThat(listErrorView.getChildCount()).isEqualTo(2);

    // Check the divider
    assertThat(listErrorView.getChildAt(0).getLayoutParams().width)
        .isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(listErrorView.getChildAt(0).getLayoutParams().height)
        .isEqualTo((int) ViewUtils.dpToPx(ErrorViewUtils.ERROR_DIVIDER_WIDTH_DP, context));

    // Check the error box
    assertThat(listErrorView.getChildAt(1)).isInstanceOf(TextView.class);
    TextView textErrorView = (TextView) listErrorView.getChildAt(1);
    assertThat(textErrorView.getText()).isEqualTo(ERROR_TEXT_1);

    // Check that padding has been set (but don't check specific values)
    assertThat(textErrorView.getPaddingBottom()).isNotEqualTo(0);
    assertThat(textErrorView.getPaddingTop()).isNotEqualTo(0);
    assertThat(textErrorView.getPaddingStart()).isNotEqualTo(0);
    assertThat(textErrorView.getPaddingEnd()).isNotEqualTo(0);
  }

  @Test
  public void testGetErrorView_multipleErrors() {
    List<String> errors = new ArrayList<>();
    errors.add(ERROR_TEXT_1);
    errors.add(ERROR_TEXT_2);

    LinearLayout errorView = (LinearLayout) errorViewUtils.getErrorView(errors);

    assertThat(errorView.getChildCount()).isEqualTo(3);

    assertThat(((TextView) errorView.getChildAt(1)).getText()).isEqualTo(ERROR_TEXT_1);
    assertThat(((TextView) errorView.getChildAt(2)).getText()).isEqualTo(ERROR_TEXT_2);
  }

  @Test
  public void testGetErrorView_zeroErrors() {
    List<String> errors = new ArrayList<>();

    LinearLayout errorView = (LinearLayout) errorViewUtils.getErrorView(errors);

    // Only a divider
    assertThat(errorView.getChildCount()).isEqualTo(1);
  }
}
