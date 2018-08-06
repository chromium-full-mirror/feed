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

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.testing.conformance.storage.ContentStorageConformanceTest;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link InMemoryContentStorage}. */
@RunWith(RobolectricTestRunner.class)
public class InMemoryContentStorageTest extends ContentStorageConformanceTest {

  @Before
  public void setUp() {
    storage = new FakeContentStorage();
  }

  private static class FakeContentStorage extends InMemoryContentStorage implements ContentStorage {

    @Override
    public void get(List<String> keys, Consumer<Result<Map<String, byte[]>>> consumer) {
      consumer.accept(get(keys));
    }

    @Override
    public void getAll(String prefix, Consumer<Result<Map<String, byte[]>>> consumer) {
      consumer.accept(getAll(prefix));
    }

    @Override
    public void commit(ContentMutation mutation, Consumer<CommitResult> consumer) {
      consumer.accept(commit(mutation));
    }

    @Override
    public void getAllKeys(Consumer<Result<List<String>>> consumer) {
      consumer.accept(getAllKeys());
    }
  }
}
