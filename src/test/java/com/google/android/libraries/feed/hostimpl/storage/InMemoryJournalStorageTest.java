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
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalStorage;
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
    journalStorage.read(
        JOURNAL_NAME,
        new Consumer<List<byte[]>>() {
          @Override
          public void accept(List<byte[]> input) {

            // The result should be an empty List.
            assertThat(input).isNotNull();
            assertThat(input).isEmpty();
          }
        });
  }

  @Test
  public void appendToJournal() throws Exception {
    // Write some data and put the result from the callback into commitResult.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });
    // Read the data back into blobs.
    journalStorage.read(
        JOURNAL_NAME,
        new Consumer<List<byte[]>>() {
          @Override
          public void accept(List<byte[]> input) {
            // We should get back one byte array, containing the bytes of DATA_0 and DATA_1.
            assertThat(input).isNotNull();
            assertThat(input).hasSize(2);
            assertThat(Arrays.equals(input.get(0), DATA_0)).isTrue();
            assertThat(Arrays.equals(input.get(1), DATA_1)).isTrue();
          }
        });
  }

  @Test
  public void copyJournal() throws Exception {
    // Write some data.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });
    // Copy the data into a new journal and put the result from the callback into commitResult.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).copy(JOURNAL_COPY_NAME).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });

    // Read the data back into blobs.
    journalStorage.read(
        JOURNAL_COPY_NAME,
        new Consumer<List<byte[]>>() {
          @Override
          public void accept(List<byte[]> input) {
            // We should get back one byte array, containing the bytes of DATA_0.
            assertThat(input).isNotNull();
            assertThat(input).hasSize(2);
            assertThat(Arrays.equals(input.get(0), DATA_0)).isTrue();
            assertThat(Arrays.equals(input.get(1), DATA_1)).isTrue();
          }
        });
  }

  @Test
  public void deleteJournal() throws Exception {
    // Write some data.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });
    // Delete the journal and put the result from the callback into commitResult.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).delete().build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });
    // Try to read the deleted journal.
    journalStorage.read(
        JOURNAL_NAME,
        new Consumer<List<byte[]>>() {
          @Override
          public void accept(List<byte[]> input) {

            // The result should be an empty List.
            assertThat(input).isNotNull();
            assertThat(input).isEmpty();
          }
        });
  }

  @Test
  public void exists() throws Exception {
    // Write some data.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });
    journalStorage.exists(
        JOURNAL_NAME,
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean input) {
            assertThat(input).isTrue();
          }
        });
  }

  @Test
  public void exists_doesNotExist() throws Exception {
    journalStorage.exists(
        JOURNAL_NAME,
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean input) {
            assertThat(input).isFalse();
          }
        });
  }

  @Test
  public void getAllJournals_singleJournal() throws Exception {
    // Write some data.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME).append(DATA_0).append(DATA_1).build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });

    journalStorage.getAllJournals(
        new Consumer<List<String>>() {
          @Override
          public void accept(List<String> input) {
            assertThat(input).hasSize(1);
            assertThat(input).contains(JOURNAL_NAME);
          }
        });
  }

  @Test
  public void getAllJournals_multipleJournals() throws Exception {
    // Write some data.
    journalStorage.commit(
        new JournalMutation.Builder(JOURNAL_NAME)
            .append(DATA_0)
            .append(DATA_1)
            .copy(JOURNAL_COPY_NAME)
            .build(),
        new Consumer<CommitResult>() {
          @Override
          public void accept(CommitResult input) {
            assertThat(input).isEqualTo(CommitResult.SUCCESS);
          }
        });

    journalStorage.getAllJournals(
        new Consumer<List<String>>() {
          @Override
          public void accept(List<String> input) {
            assertThat(input).hasSize(2);
            assertThat(input).contains(JOURNAL_NAME);
            assertThat(input).contains(JOURNAL_COPY_NAME);
          }
        });
  }
}
