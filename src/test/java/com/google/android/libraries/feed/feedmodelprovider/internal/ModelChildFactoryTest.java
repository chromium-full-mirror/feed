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

package com.google.android.libraries.feed.feedmodelprovider.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link ModelChildFactory}. */
@RunWith(RobolectricTestRunner.class)
public class ModelChildFactoryTest {
  private StreamContentId contentId = null;
  private StreamContentId parentId = null;

  @Before
  public void setup() {
    initMocks(this);
    contentId = StreamContentId.newBuilder().build();
    parentId = StreamContentId.newBuilder().build();
  }

  @Test
  public void testCreateChild() {
    ModelChildFactory modelChildFactory = new ModelChildFactory();
    ModelChild modelChild = modelChildFactory.createUnboundChild(contentId, parentId);
    assertThat(modelChild).isNotNull();
    assertThat(modelChild.getType()).isEqualTo(Type.UNBOUND);
    assertThat(modelChild.getContentId()).isEqualTo(contentId);
    assertThat(modelChild.hasParentId()).isTrue();
    assertThat(modelChild.getParentId()).isEqualTo(parentId);
  }
}
