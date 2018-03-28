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

package com.google.android.libraries.feed.host.storage.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.testing.RequiredConsumer;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import java.nio.charset.Charset;
import java.util.Map;
import org.junit.Test;

/**
 * Conformance test for {@link ContentStorage}. Hosts who wish to test against this should extend
 * this class and set {@code storage} to the Host implementation.
 */
public abstract class ContentStorageConformanceTest {

  private static final String KEY = "key";
  private static final String KEY_0 = KEY + " 0";
  private static final String KEY_1 = KEY + " 1";
  private static final String OTHER_KEY = "other";
  private static final byte[] DATA_0 = "data 0".getBytes(Charset.forName("UTF-8"));
  private static final byte[] DATA_1 = "data 1".getBytes(Charset.forName("UTF-8"));
  private final Consumer<Map<String, byte[]>> hasData0AndData1 =
      new Consumer<Map<String, byte[]>>() {
        @Override
        public void accept(Map<String, byte[]> input) {
          assertThat(input.get(KEY_0)).isEqualTo(DATA_0);
          assertThat(input.get(KEY_1)).isEqualTo(DATA_1);
        }
      };
  private static final byte[] OTHER_DATA = "other data".getBytes(Charset.forName("UTF-8"));

  // Helper consumers to make tests cleaner
  private final Consumer<byte[]> isData0 =
      new Consumer<byte[]>() {
        @Override
        public void accept(byte[] input) {
          assertThat(input).isEqualTo(DATA_0);
        }
      };
  private final Consumer<byte[]> isData1 =
      new Consumer<byte[]>() {
        @Override
        public void accept(byte[] input) {
          assertThat(input).isEqualTo(DATA_1);
        }
      };
  private final Consumer<byte[]> isDataOther =
      new Consumer<byte[]>() {
        @Override
        public void accept(byte[] input) {
          assertThat(input).isEqualTo(OTHER_DATA);
        }
      };
  private final Consumer<CommitResult> isSuccess =
      new Consumer<CommitResult>() {
        @Override
        public void accept(CommitResult input) {
          assertThat(input).isEqualTo(CommitResult.SUCCESS);
        }
      };
  private final Consumer<byte[]> isEmpty =
      new Consumer<byte[]>() {
        @Override
        public void accept(byte[] input) {
          assertThat(input).isNotNull();
          assertThat(input).hasLength(0);
        }
      };

  protected ContentStorage storage;

  @Test
  public void missingKey() throws Exception {
    RequiredConsumer<byte[]> consumer = new RequiredConsumer<>(isEmpty);
    storage.get("missing key", consumer);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void storeAndRetrieve() throws Exception {
    RequiredConsumer<CommitResult> consumer =
        new RequiredConsumer<>(
            new Consumer<CommitResult>() {
              @Override
              public void accept(CommitResult input) {
                assertThat(input).isEqualTo(CommitResult.SUCCESS);

                RequiredConsumer<byte[]> byteConsumer = new RequiredConsumer<>(isData0);
                storage.get(KEY_0, byteConsumer);
                assertThat(byteConsumer.isCalled()).isTrue();
              }
            });
    storage.commit(new ContentMutation.Builder().upsert(KEY_0, DATA_0).build(), consumer);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void storeAndDelete() throws Exception {
    RequiredConsumer<CommitResult> consumer =
        new RequiredConsumer<>(
            new Consumer<CommitResult>() {
              @Override
              public void accept(CommitResult input) {
                assertThat(input).isEqualTo(CommitResult.SUCCESS);

                // Confirm Key 0 and 1 are present
                RequiredConsumer<byte[]> byteConsumer1 = new RequiredConsumer<>(isData0);
                storage.get(KEY_0, byteConsumer1);
                assertThat(byteConsumer1.isCalled()).isTrue();

                RequiredConsumer<byte[]> byteConsumer2 = new RequiredConsumer<>(isData1);
                storage.get(KEY_1, byteConsumer2);
                assertThat(byteConsumer2.isCalled()).isTrue();

                // Delete Key 0
                RequiredConsumer<CommitResult> deleteConsumer = new RequiredConsumer<>(isSuccess);
                storage.commit(new ContentMutation.Builder().delete(KEY_0).build(), deleteConsumer);
                assertThat(deleteConsumer.isCalled()).isTrue();

                // Confirm that Key 0 is deleted and Key 1 is present
                byteConsumer1 = new RequiredConsumer<>(isEmpty);
                storage.get(KEY_0, byteConsumer1);
                assertThat(byteConsumer1.isCalled()).isTrue();

                byteConsumer2 = new RequiredConsumer<>(isData1);
                storage.get(KEY_1, byteConsumer2);
                assertThat(byteConsumer2.isCalled()).isTrue();
              }
            });
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build(),
        consumer);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void storeAndDeleteByPrefix() throws Exception {
    RequiredConsumer<CommitResult> consumer =
        new RequiredConsumer<>(
            new Consumer<CommitResult>() {
              @Override
              public void accept(CommitResult input) {
                assertThat(input).isEqualTo(CommitResult.SUCCESS);

                // Confirm Key 0, Key 1, and Other are present
                RequiredConsumer<byte[]> byteConsumer1 = new RequiredConsumer<>(isData0);
                storage.get(KEY_0, byteConsumer1);
                assertThat(byteConsumer1.isCalled()).isTrue();

                RequiredConsumer<byte[]> byteConsumer2 = new RequiredConsumer<>(isData1);
                storage.get(KEY_1, byteConsumer2);
                assertThat(byteConsumer2.isCalled()).isTrue();

                RequiredConsumer<byte[]> byteConsumer3 = new RequiredConsumer<>(isDataOther);
                storage.get(OTHER_KEY, byteConsumer3);
                assertThat(byteConsumer3.isCalled()).isTrue();

                // Delete by prefix Key
                RequiredConsumer<CommitResult> deleteConsumer = new RequiredConsumer<>(isSuccess);
                storage.commit(
                    new ContentMutation.Builder().deleteByPrefix(KEY).build(), deleteConsumer);

                // Confirm Key 0 and Key 1 are deleted, and Other is present
                byteConsumer1 = new RequiredConsumer<>(isEmpty);
                storage.get(KEY_0, byteConsumer1);
                assertThat(byteConsumer1.isCalled()).isTrue();

                byteConsumer2 = new RequiredConsumer<>(isEmpty);
                storage.get(KEY_1, byteConsumer2);
                assertThat(byteConsumer2.isCalled()).isTrue();

                byteConsumer3 = new RequiredConsumer<>(isDataOther);
                storage.get(OTHER_KEY, byteConsumer3);
                assertThat(byteConsumer3.isCalled()).isTrue();
              }
            });
    storage.commit(
        new ContentMutation.Builder()
            .upsert(KEY_0, DATA_0)
            .upsert(KEY_1, DATA_1)
            .upsert(OTHER_KEY, OTHER_DATA)
            .build(),
        consumer);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void multipleValues_get() throws Exception {
    RequiredConsumer<CommitResult> commitResultRequiredConsumer =
        new RequiredConsumer<>(
            new Consumer<CommitResult>() {
              @Override
              public void accept(CommitResult input) {
                assertThat(input).isEqualTo(CommitResult.SUCCESS);

                RequiredConsumer<byte[]> byteConsumer1 = new RequiredConsumer<>(isData0);
                storage.get(KEY_0, byteConsumer1);
                assertThat(byteConsumer1.isCalled()).isTrue();

                RequiredConsumer<byte[]> byteConsumer2 = new RequiredConsumer<>(isData1);
                storage.get(KEY_1, byteConsumer2);
                assertThat(byteConsumer2.isCalled()).isTrue();
              }
            });
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build(),
        commitResultRequiredConsumer);
    assertThat(commitResultRequiredConsumer.isCalled()).isTrue();
  }

  @Test
  public void multipleValues_getAll() throws Exception {
    RequiredConsumer<CommitResult> commitResultConsumer =
        new RequiredConsumer<>(
            new Consumer<CommitResult>() {
              @Override
              public void accept(CommitResult input) {
                assertThat(input).isEqualTo(CommitResult.SUCCESS);

                RequiredConsumer<Map<String, byte[]>> mapConsumer =
                    new RequiredConsumer<>(hasData0AndData1);
                storage.getAll(KEY, mapConsumer);
                assertThat(mapConsumer.isCalled()).isTrue();
              }
            });
    storage.commit(
        new ContentMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build(),
        commitResultConsumer);
    assertThat(commitResultConsumer.isCalled()).isTrue();
  }
}
