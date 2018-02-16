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

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link com.google.android.libraries.feed.api.modelprovider.StreamFeatureChange}. */
@RunWith(RobolectricTestRunner.class)
public class StreamFeatureChangeTest {

  @Test
  public void testStreamFeatureChange() {
    StreamContentId contentId = StreamContentId.getDefaultInstance();
    StreamFeature streamFeature = StreamFeature.getDefaultInstance();
    List<ModelChild> appendChildren = new ArrayList<>();
    List<ModelChild> removeChildren = new ArrayList<>();
    StreamFeatureChange.ChildChange childChange =
        new StreamFeatureChange.ChildChange(appendChildren, removeChildren);
    StreamFeatureChange streamFeatureChange =
        new StreamFeatureChange(contentId, streamFeature, childChange);

    assertThat(streamFeatureChange.getContentId()).isEqualTo(contentId);
    assertThat(streamFeatureChange.getUpdatedStreamFeature()).isEqualTo(streamFeature);
    assertThat(streamFeatureChange.getNewChildren()).isEqualTo(appendChildren);
    assertThat(streamFeatureChange.getRemovedChildren()).isEqualTo(removeChildren);
  }

  @Test
  public void testNullLists() {
    StreamContentId contentId = StreamContentId.getDefaultInstance();
    StreamFeature streamFeature = StreamFeature.getDefaultInstance();
    StreamFeatureChange.ChildChange childChange = new StreamFeatureChange.ChildChange(null, null);
    StreamFeatureChange streamFeatureChange =
        new StreamFeatureChange(contentId, streamFeature, childChange);

    assertThat(streamFeatureChange.getNewChildren()).isEmpty();
    assertThat(streamFeatureChange.getRemovedChildren()).isEmpty();
  }
}
