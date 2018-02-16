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

package com.google.android.libraries.feed.host.storage;

import static com.google.common.truth.Truth.assertThat;

import org.robolectric.RobolectricTestRunner;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link InMemoryKeyValuePairStorage}. */
@RunWith(RobolectricTestRunner.class)
public class InMemoryKeyValuePairStorageTest {

  private static final String KEY = "key";
  private static final String KEY_0 = KEY + " 0";
  private static final String KEY_1 = KEY + " 1";
  private static final byte[] DATA_0 = "data 0".getBytes(Charset.forName("UTF-8"));
  private static final byte[] DATA_1 = "data 1".getBytes(Charset.forName("UTF-8"));
  private KeyValuePairStorage storage;

  @Before
  public void setUp() {
    storage = new InMemoryKeyValuePairStorage();
  }

  @Test
  public void missingKey() throws Exception {
    byte[] value = storage.get("missing key").get();
    assertThat(value).isNull();
  }

  @Test
  public void storeAndRetrieve() throws Exception {
    storage.commit(new StorageMutation.Builder().upsert(KEY_0, DATA_0).build()).get();

    byte[] value = storage.get(KEY_0).get();

    assertThat(value).isEqualTo(DATA_0);
  }

  @Test
  public void multipleValues_get() throws Exception {
    storage
        .commit(new StorageMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build())
        .get();

    byte[] value0 = storage.get(KEY_0).get();
    byte[] value1 = storage.get(KEY_1).get();

    assertThat(value0).isEqualTo(DATA_0);
    assertThat(value1).isEqualTo(DATA_1);
  }

  @Test
  public void multipleValues_getAll() throws Exception {
    storage
        .commit(new StorageMutation.Builder().upsert(KEY_0, DATA_0).upsert(KEY_1, DATA_1).build())
        .get();

    List<KeyValuePair> values = storage.getAll(KEY).get();

    Map<String, byte[]> map = new HashMap<>();
    for (KeyValuePair value : values) {
      map.put(value.getKey(), value.getValue());
    }
    assertThat(map.get(KEY_0)).isEqualTo(DATA_0);
    assertThat(map.get(KEY_1)).isEqualTo(DATA_1);
  }
}
