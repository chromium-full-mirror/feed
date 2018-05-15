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

package com.google.android.libraries.feed.basicstream.internal.contentmodel;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContinuationContentModel;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContinuationContentModel}. */
@RunWith(RobolectricTestRunner.class)
public class ContinuationContentModelTest {

  @Mock private ContinuationViewHolder viewHolder;
  @Mock private OnClickListener clickListener;

  private ContinuationContentModel continuationContentModel;

  @Before
  public void setUp() {
    initMocks(this);
    continuationContentModel =
        new ContinuationContentModel(clickListener, /* paginateImmediately= */ false);
  }

  @Test
  public void testBind_spinnerOn() {
    continuationContentModel.setShowSpinner(true);

    continuationContentModel.bind(viewHolder);

    verify(viewHolder).bind(clickListener, /* showSpinner= */ true);
  }

  @Test
  public void testBind_spinnerOff() {
    continuationContentModel.setShowSpinner(false);

    continuationContentModel.bind(viewHolder);

    verify(viewHolder).bind(clickListener, /* showSpinner= */ false);
  }

  @Test
  public void testBind_paginateImmediately() {
    continuationContentModel =
        new ContinuationContentModel(clickListener, /* paginateImmediately= */ true);

    continuationContentModel.bind(viewHolder);

    verify(clickListener).onClick(any(View.class));

    verify(viewHolder).bind(clickListener, /* showSpinner= */ true);
  }

  @Test
  public void testBind_paginateImmediately_turnSpinnerOff() {
    continuationContentModel =
        new ContinuationContentModel(clickListener, /* paginateImmediately= */ true);

    continuationContentModel.setShowSpinner(false);

    continuationContentModel.bind(viewHolder);

    verifyZeroInteractions(clickListener);
    verify(viewHolder).bind(clickListener, /* showSpinner= */ false);
  }

  @Test
  public void testBind_paginateImmediately_onlyTriggersClickListenerOnce() {
    continuationContentModel =
        new ContinuationContentModel(clickListener, /* paginateImmediately= */ true);

    continuationContentModel.bind(viewHolder);
    continuationContentModel.unbind(viewHolder);
    continuationContentModel.bind(viewHolder);

    verify(clickListener).onClick(any(View.class));
    verify(viewHolder, times(2)).bind(clickListener, /* showSpinner= */ true);
  }

  @Test
  public void testUnbind() {
    continuationContentModel.bind(viewHolder);
    continuationContentModel.unbind(viewHolder);
    verify(viewHolder).unbind();
  }

  @Test
  public void testShowSpinner_postBind() {
    continuationContentModel.bind(viewHolder);

    continuationContentModel.setShowSpinner(/* showSpinner= */ true);

    verify(viewHolder).setShowSpinner(/* showSpinner= */ true);
  }
}
