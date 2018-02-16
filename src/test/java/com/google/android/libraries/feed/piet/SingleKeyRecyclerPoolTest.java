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

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.TextElementAdapter.Key;
import com.google.search.now.ui.piet.StylesProto.Font;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

/** Tests of the {@link SingleKeyRecyclerPool} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SingleKeyRecyclerPoolTest {

  public static final Key DEFAULT_KEY = new TextElementAdapter.Key(Font.getDefaultInstance());
  public static final Key INVALID_KEY =
      new TextElementAdapter.Key(Font.newBuilder().setSize(999).build());
  public static final int DEFAULT_CAPACITY = 2;

  @Mock TextElementAdapter adapter;
  @Mock TextElementAdapter adapter2;
  @Mock TextElementAdapter adapter3;

  SingleKeyRecyclerPool<Key, TextElementAdapter> pool;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    pool = new SingleKeyRecyclerPool<>(DEFAULT_KEY, DEFAULT_CAPACITY);
  }

  @Test
  public void testPutAndGetOneElement() {
    pool.put(DEFAULT_KEY, adapter);
    assertThat(pool.get(DEFAULT_KEY)).isEqualTo(adapter);
  }

  @Test
  public void testGetOnEmptyPoolReturnsNull() {
    assertThat(pool.get(DEFAULT_KEY)).isNull();
  }

  @Test
  public void testGetWithDifferentKeyFails() {
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                pool.get(INVALID_KEY);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("does not match singleton key");
  }

  @Test
  public void testPutWithDifferentKeyFails() {
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                pool.put(INVALID_KEY, adapter);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("does not match singleton key");
  }

  @Test
  public void testGetWithNullKeyFails() {
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                pool.get(null);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("does not match singleton key");
  }

  @Test
  public void testPutWithNullKeyFails() {
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                pool.put(null, adapter);
              }
            })
        .throwsAnExceptionOfType(NullPointerException.class)
        .that()
        .hasMessageThat()
        .contains("null key for adapter");
  }

  @Test
  public void testPutSameElementTwiceFails() {
    pool.put(DEFAULT_KEY, adapter);
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                pool.put(DEFAULT_KEY, adapter);
              }
            })
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Already in the pool!");
  }

  @Test
  public void testPutAndGetTwoElements() {
    pool.put(DEFAULT_KEY, adapter);
    pool.put(DEFAULT_KEY, adapter2);
    assertThat(pool.get(DEFAULT_KEY)).isNotNull();
    assertThat(pool.get(DEFAULT_KEY)).isNotNull();
    assertThat(pool.get(DEFAULT_KEY)).isNull();
  }

  @Test
  public void testPoolOverflowIgnoresLastElement() {
    pool.put(DEFAULT_KEY, adapter);
    pool.put(DEFAULT_KEY, adapter2);
    pool.put(DEFAULT_KEY, adapter3);
    assertThat(pool.get(DEFAULT_KEY)).isNotNull();
    assertThat(pool.get(DEFAULT_KEY)).isNotNull();
    assertThat(pool.get(DEFAULT_KEY)).isNull();
  }
}
