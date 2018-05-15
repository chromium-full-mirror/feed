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
import static org.mockito.MockitoAnnotations.initMocks;

import android.view.View.OnClickListener;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContinuationContentModel;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContinuationDriver}.j */
@RunWith(RobolectricTestRunner.class)
public class ContinuationDriverTest {

  @Mock private ModelToken modelToken;
  @Mock private ModelProvider modelProvider;
  @Mock private ContinuationContentModel continuationContentModel;

  private ContinuationDriverForTest continuationDriver;
  private Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setup() {
    initMocks(this);

    continuationDriver =
        new ContinuationDriverForTest(
            modelProvider, modelToken, continuationContentModel, configuration);
  }

  @Test
  public void testGetContentModel_immediatePaginationOn() {
    continuationDriver =
        new ContinuationDriverForTest(
            modelProvider,
            modelToken,
            continuationContentModel,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build());

    ContentModel continuationContentModel = continuationDriver.getContentModel();

    assertThat(continuationContentModel).isEqualTo(this.continuationContentModel);
    assertThat(continuationDriver.paginationImmediatelyArgument).isTrue();
  }

  @Test
  public void testGetContentModel_immediatePaginationOff() {
    continuationDriver =
        new ContinuationDriverForTest(
            modelProvider,
            modelToken,
            continuationContentModel,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, false).build());

    ContentModel continuationContentModel = continuationDriver.getContentModel();

    assertThat(continuationContentModel).isEqualTo(this.continuationContentModel);
    assertThat(continuationDriver.paginationImmediatelyArgument).isFalse();
  }

  @Test
  public void testGetContentModel_reusesPreviousContentModel() {
    ContentModel continuationContentModel = continuationDriver.getContentModel();

    assertThat(continuationDriver.getContentModel()).isSameAs(continuationContentModel);
  }

  @Test
  public void testOnClick() {
    continuationDriver.getContentModel();
    continuationDriver.onClick(/* v= */ null);

    verify(continuationContentModel).setShowSpinner(true);
    verify(modelProvider).handleToken(modelToken);
  }

  private class ContinuationDriverForTest extends ContinuationDriver {

    private ContinuationContentModel continuationContentModel;
    private boolean paginationImmediatelyArgument;

    ContinuationDriverForTest(
        ModelProvider modelProvider,
        ModelToken modelToken,
        ContinuationContentModel continuationContentModel,
        Configuration configuration) {
      super(modelProvider, modelToken, configuration);
      this.continuationContentModel = continuationContentModel;
    }

    @Override
    ContinuationContentModel createContinuationContentModel(
        OnClickListener onClickListener, boolean paginateImmediately) {
      this.paginationImmediatelyArgument = paginateImmediately;
      return continuationContentModel;
    }
  }
}
