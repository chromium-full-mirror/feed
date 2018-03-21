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

import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalOperation;
import com.google.android.libraries.feed.host.storage.JournalOperation.Append;
import com.google.android.libraries.feed.host.storage.JournalOperation.Copy;
import com.google.android.libraries.feed.host.storage.JournalOperation.Delete;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** A {@link JournalStorage} implementation that holds the data in memory. */
public class InMemoryJournalStorage implements JournalStorage, Dumpable {
  private static final String TAG = "InMemoryJournalStorage";

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<String, List<byte[]>> journals;
  private int readCount = 0;
  private int appendCount = 0;
  private int copyCount = 0;
  private int deleteCount = 0;

  public InMemoryJournalStorage() {
    journals = new HashMap<>();
  }

  @Override
  public Future<List<byte[]>> read(String journalName) {
    return executor.submit(
        () -> {
          readCount++;
          List<byte[]> journal = journals.get(journalName);
          if (journal == null) {
            journal = new ArrayList<>();
          }
          return journal;
        });
  }

  @Override
  public Future<Boolean> commit(JournalMutation mutation) {
    return executor.submit(
        () -> {
          String journalName = mutation.getJournalName();

          for (JournalOperation operation : mutation.getOperations()) {
            // TODO: instanceof is ugly here, and it makes it hard to report what the
            // bad
            // value was if we get an unexpected operation. Also include an enum in
            // JournalOperation?
            if (operation instanceof Append) {
              if (!append(((Append) operation).getValue(), journalName)) {
                return false;
              }
            } else if (operation instanceof Copy) {
              if (!copy(journalName, ((Copy) operation).getToJournalName())) {
                return false;
              }
            } else if (operation instanceof Delete) {
              if (!delete(journalName)) {
                return false;
              }
            } else {
              Logger.w(TAG, "Unexpected JournalOperation type: %s", operation.getType());
              return false;
            }
          }
          return true;
        });
  }

  private boolean append(ByteBuffer value, String journalName) {
    List<byte[]> journal = journals.get(journalName);
    if (value == null) {
      Logger.e(TAG, "Journal not found: %s", journalName);
      return false;
    }
    if (journal == null) {
      journal = new ArrayList<>();
      journals.put(journalName, journal);
    }
    appendCount++;
    journal.add(value.array());
    return true;
  }

  private boolean copy(String fromJournalName, String toJournalName) {
    copyCount++;
    List<byte[]> toJournal = journals.get(toJournalName);
    if (toJournal != null) {
      Logger.e(TAG, "Copy destination journal already present: %s", toJournalName);
      return false;
    }
    List<byte[]> journal = journals.get(fromJournalName);
    if (journal != null) {
      // TODO: Compact before copying?
      journals.put(toJournalName, new ArrayList<>(journal));
    }
    return true;
  }

  private boolean delete(String journalName) {
    deleteCount++;
    journals.remove(journalName);
    return true;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("readCount").value(readCount);
    dumper.forKey("appendCount").value(appendCount).compactPrevious();
    dumper.forKey("copyCount").value(copyCount).compactPrevious();
    dumper.forKey("deleteCount").value(deleteCount).compactPrevious();
    dumper.forKey("sessions").value(journals.size());
    for (Entry<String, List<byte[]>> entry : journals.entrySet()) {
      Dumper childDumper = dumper.getChildDumper();
      childDumper.title("Session");
      childDumper.forKey("name").value(entry.getKey());
      childDumper.forKey("operations").value(entry.getValue().size()).compactPrevious();
    }
  }
}
