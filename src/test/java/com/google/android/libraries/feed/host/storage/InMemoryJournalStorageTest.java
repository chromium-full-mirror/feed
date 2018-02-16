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
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link InMemoryJournalStorage}. */
@RunWith(RobolectricTestRunner.class)
public class InMemoryJournalStorageTest {
  // TODO: set up threading to allow testing of real asynchronous JournalStorages.
  // TODO: daggerify to test any JournalStorage with this test class.
  private static final String JOURNAL_NAME = "journal name";
  private static final byte[] DATA_0 = "data 0".getBytes(Charset.forName("UTF-8"));
  private static final byte[] DATA_1 = "data 1".getBytes(Charset.forName("UTF-8"));
  private static final String JOURNAL_COPY_NAME = "journal copy name";

  private JournalStorage journalStorage;

  @Before
  public void setUp() {
    journalStorage = new InMemoryJournalStorage();
  }

  @Test
  public void readOfEmptyJournalReturnsEmptyData() throws Exception {
    // Try to read some blobs from an empty journal store.
    List<byte[]> blobs = journalStorage.read(JOURNAL_NAME).get();

    // The result should be an empty List.
    assertThat(blobs).isNotNull();
    assertThat(blobs).isEmpty();
  }

  @Test
  public void appendToJournal() throws Exception {
    // Write some data and put the result from the callback into commitResult.
    CommitResult commitResult =
        journalStorage
            .commit(new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build())
            .get();
    // Read the data back into blobs.
    List<byte[]> blobs = journalStorage.read(JOURNAL_NAME).get();

    // We should get back one byte array, containing the bytes of DATA_0 and DATA_1.
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);
    assertThat(blobs).isNotNull();
    assertThat(blobs).hasSize(2);
    assertThat(Arrays.equals(blobs.get(0), DATA_0)).isTrue();
    assertThat(Arrays.equals(blobs.get(1), DATA_1)).isTrue();
  }

  @Test
  public void copyJournal() throws Exception {
    // Write some data.
    journalStorage
        .commit(new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build())
        .get();
    // Copy the data into a new journal and put the result from the callback into commitResult.
    CommitResult commitResult =
        journalStorage
            .commit(new JournalMutation.Builder(JOURNAL_NAME).copy(JOURNAL_COPY_NAME).build())
            .get();
    // Read the data back into blobs.
    List<byte[]> blobs = journalStorage.read(JOURNAL_COPY_NAME).get();

    // We should get back one byte array, containing the bytes of DATA_0.
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);
    assertThat(blobs).isNotNull();
    assertThat(blobs).hasSize(2);
    assertThat(Arrays.equals(blobs.get(0), DATA_0)).isTrue();
    assertThat(Arrays.equals(blobs.get(1), DATA_1)).isTrue();
  }

  @Test
  public void deleteJournal() throws Exception {
    // Write some data.
    journalStorage
        .commit(new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build())
        .get();
    // Delete the journal and put the result from the callback into commitResult.
    CommitResult commitResult =
        journalStorage.commit(new JournalMutation.Builder(JOURNAL_NAME).delete().build()).get();
    // Try to read the deleted journal.
    List<byte[]> blobs = journalStorage.read(JOURNAL_NAME).get();

    // The result should be an empty List.
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);
    assertThat(blobs).isNotNull();
    assertThat(blobs).isEmpty();
  }
}
