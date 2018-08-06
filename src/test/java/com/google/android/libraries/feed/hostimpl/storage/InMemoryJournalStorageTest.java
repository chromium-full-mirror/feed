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

import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.android.libraries.feed.testing.conformance.storage.JournalStorageConformanceTest;
import java.util.List;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link InMemoryJournalStorage}. */
@RunWith(RobolectricTestRunner.class)
public class InMemoryJournalStorageTest extends JournalStorageConformanceTest {

  @Before
  public void setUp() {
    initMocks(this);
    journalStorage = new FakeJournalStorage();
  }

  private static class FakeJournalStorage extends InMemoryJournalStorage implements JournalStorage {

    @Override
    public void read(String journalName, Consumer<Result<List<byte[]>>> consumer) {
      consumer.accept(read(journalName));
    }

    @Override
    public void commit(JournalMutation mutation, Consumer<CommitResult> consumer) {
      consumer.accept(commit(mutation));
    }

    @Override
    public void exists(String journalName, Consumer<Result<Boolean>> consumer) {
      consumer.accept(exists(journalName));
    }

    @Override
    public void getAllJournals(Consumer<Result<List<String>>> consumer) {
      consumer.accept(getAllJournals());
    }

    @Override
    public void deleteAll(Consumer<CommitResult> consumer) {
      consumer.accept(deleteAll());
    }
  }
}
