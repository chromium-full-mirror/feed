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

package com.google.android.libraries.feed.hostimpl.storage;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import org.robolectric.RobolectricTestRunner;
import java.nio.charset.Charset;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link InMemoryContentStorage}. */
@RunWith(RobolectricTestRunner.class)
public class InMemoryContentStorageTest {

  private static final String KEY = "key";
  private static final String KEY_0 = KEY + " 0";
  private static final String KEY_1 = KEY + " 1";
  private static final byte[] DATA_0 = "data 0".getBytes(Charset.forName("UTF-8"));
  private static final byte[] DATA_1 = "data 1".getBytes(Charset.forName("UTF-8"));
  private ContentStorage storage;

  @Before
  public void setUp() {
    storage = new InMemoryContentStorage();
  }

  @Test
  public void missingKey() throws Exception {
    byte[] value = storage.get("missing key").get();
    assertThat(value).isNull();
  }

  @Test
  public void missingKey_async() throws Exception {
    storage.get(
        "missing key",
        new Consumer<byte[]>() {
          @Override
          public void accept(byte[] input) {
            assertThat(input).isNull();
          }
        });
  }

  @Test
  public void storeAndRetrieve() throws Exception {
    storage.commit(new ContentMutation.Builder().upsert(KEY_0, DATA_0).build()).get();

    byte[] value = storage.get(KEY_0).get();

    assertThat(value).isEqualTo(DATA_0);
  }

  @Test
  public void storeAndRetrieve_async() throws Exception {
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
            storage.get(
                KEY_0,
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] input) {
                    assertThat(input).isEqualTo(DATA_0);
                  }
                });
          }
        });
  }

  @Test
  public void multipleValues_get() throws Exception {
    storage
        .commit(new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build())
        .get();

    byte[] value0 = storage.get(KEY_0).get();
    byte[] value1 = storage.get(KEY_1).get();

    assertThat(value0).isEqualTo(DATA_0);
    assertThat(value1).isEqualTo(DATA_1);
  }

  @Test
  public void multipleValues_get_async() throws Exception {
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);

            storage.get(
                KEY_0,
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] input) {
                    assertThat(input).isEqualTo(DATA_0);
                  }
                });

            storage.get(
                KEY_1,
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] input) {
                    assertThat(input).isEqualTo(DATA_1);
                  }
                });
          }
        });
  }

  @Test
  public void multipleValues_getAll() throws Exception {
    storage
        .commit(new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build())
        .get();

    Map<String, byte[]> values = storage.getAll(KEY).get();

    assertThat(values.get(KEY_0)).isEqualTo(DATA_0);
    assertThat(values.get(KEY_1)).isEqualTo(DATA_1);
  }

  @Test
  public void multipleValues_getAll_async() throws Exception {
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);

            storage.getAll(
                KEY,
                new Consumer<Map<String, byte[]>>() {
                  @Override
                  public void accept(Map<String, byte[]> input) {
                    assertThat(input.get(KEY_0)).isEqualTo(DATA_0);
                    assertThat(input.get(KEY_1)).isEqualTo(DATA_1);
                  }
                });
          }
        });
  }
}
