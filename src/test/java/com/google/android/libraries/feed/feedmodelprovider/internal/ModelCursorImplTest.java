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

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor.ProcessNextChild;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link ModelCursorImpl}. */
@RunWith(RobolectricTestRunner.class)
public class ModelCursorImplTest {

  @Mock ThreadUtils threadUtils;

  private List<UpdatableModelChild> modelChildren;
  private int childCount = 0;
  private StreamContentId parentContentId;

  @Before
  public void setup() {
    initMocks(this);
    modelChildren = new ArrayList<>();
    parentContentId = StreamContentId.getDefaultInstance();
  }

  @Test
  public void testEmptyCursor() {
    ModelCursorImpl cursor = new ModelCursorImpl(parentContentId, modelChildren, threadUtils);
    assertThat(cursor.isInvalid()).isFalse();
    assertThat(cursor.isAtEnd()).isTrue();
  }

  @Test
  public void testReleaseCursor() {
    modelChildren.add(mock(UpdatableModelChild.class));
    ModelCursorImpl cursor = new ModelCursorImpl(parentContentId, modelChildren, threadUtils);
    assertThat(cursor.isInvalid()).isFalse();
    assertThat(cursor.isAtEnd()).isFalse();

    cursor.release();
    assertThat(cursor.isInvalid()).isTrue();
    assertThat(cursor.isAtEnd()).isTrue();

    assertThat(
            cursor.advanceCursor(
                new ProcessNextChild() {
                  @Override
                  public void processNextChild(ModelChild child) {
                    // Do nothing
                  }
                }))
        .isFalse();
  }

  @Test
  public void testIteration() {
    int childrenToAdd = 3;
    for (int i = 0; i < childrenToAdd; i++) {
      modelChildren.add(mock(UpdatableModelChild.class));
    }
    ModelCursorImpl cursor = new ModelCursorImpl(parentContentId, modelChildren, threadUtils);
    childCount = 0;
    for (int i = 0; i < childrenToAdd; i++) {
      assertThat(cursor.advanceCursor(incrementChildCountProcessor())).isTrue();
    }
    assertThat(cursor.advanceCursor(incrementChildCountProcessor())).isFalse();
    assertThat(cursor.isAtEnd()).isTrue();
    assertThat(childCount).isEqualTo(childrenToAdd);
  }

  private ProcessNextChild incrementChildCountProcessor() {
    return new ProcessNextChild() {
      @Override
      public void processNextChild(ModelChild child) {
        childCount++;
      }
    };
  }
}
