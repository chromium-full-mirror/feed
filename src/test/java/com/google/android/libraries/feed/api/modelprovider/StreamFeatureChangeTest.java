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

package com.google.android.libraries.feed.api.modelprovider;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link com.google.android.libraries.feed.api.modelprovider.StreamFeatureChange}. */
@RunWith(RobolectricTestRunner.class)
public class StreamFeatureChangeTest {

  @Mock private ModelFeature modelFeature;

  private StreamContentId modelContentId;
  private ContentIdGenerators idGenerators = new ContentIdGenerators();

  @Before
  public void setup() {
    initMocks(this);
    modelContentId = idGenerators.createFeatureStreamContentId(1);
    StreamFeature streamFeature = StreamFeature.newBuilder().setContentId(modelContentId).build();
    when(modelFeature.getStreamFeature()).thenReturn(streamFeature);
  }

  @Test
  public void testStreamFeatureChange() {
    StreamFeatureChange streamFeatureChange = new StreamFeatureChange(modelFeature);

    assertThat(streamFeatureChange.getModelFeature()).isEqualTo(modelFeature);
    assertThat(streamFeatureChange.getContentId()).isEqualTo(modelContentId);
    assertThat(streamFeatureChange.getChildChanges().getAppendedChildren()).isEmpty();
    assertThat(streamFeatureChange.isFeatureChanged()).isFalse();

    streamFeatureChange.setFeatureChange(true);
    assertThat(streamFeatureChange.isFeatureChanged()).isTrue();
  }

  @Test
  public void testAppendChild() {
    ModelChild modelChild = mock(ModelChild.class);
    StreamFeatureChange streamFeatureChange = new StreamFeatureChange(modelFeature);
    streamFeatureChange.getChildChanges().addAppendChild(modelChild);
    assertThat(streamFeatureChange.getChildChanges().getAppendedChildren()).hasSize(1);
    assertThat(streamFeatureChange.getChildChanges().getAppendedChildren()).contains(modelChild);
  }
}
