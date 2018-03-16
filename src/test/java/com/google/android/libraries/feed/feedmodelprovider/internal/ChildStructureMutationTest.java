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
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link ChildStructureMutation} class. */
@RunWith(RobolectricTestRunner.class)
public class ChildStructureMutationTest {
  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  public void testAdd() {
    UpdatableModelChild child = mock(UpdatableModelChild.class);
    ChildStructureMutation childStructureMutation =
        new ChildStructureMutation(Operation.UPDATE_OR_APPEND, child, null);
    assertThat(childStructureMutation.operation).isEqualTo(Operation.UPDATE_OR_APPEND);
    assertThat(childStructureMutation.addChild).isEqualTo(child);
    assertThat(childStructureMutation.removeChild).isNull();
  }

  @Test
  public void testRemove() {
    StreamStructure child = StreamStructure.getDefaultInstance();
    ChildStructureMutation childStructureMutation =
        new ChildStructureMutation(Operation.REMOVE, null, child);
    assertThat(childStructureMutation.operation).isEqualTo(Operation.REMOVE);
    assertThat(childStructureMutation.removeChild).isEqualTo(child);
    assertThat(childStructureMutation.addChild).isNull();
  }
}
