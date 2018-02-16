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

import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.host.storage.StorageOperation.Upsert;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** A {@link KeyValuePairStorage} that holds data in memory. */
public class InMemoryKeyValuePairStorage implements KeyValuePairStorage, Dumpable {

  private static final String TAG = "InMemoryKVPStorage";

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<String, byte[]> store = new HashMap<>();
  private int getCount = 0;
  private int getAllCount = 0;
  private int insertCount = 0;
  private int updateCount = 0;

  @Override
  public Future<byte[]> get(String key) {
    // TODO: once Callback allows it, populate a failure reason here.
    getCount++;
    return executor.submit(() -> store.get(key));
  }

  @Override
  public Future<List<KeyValuePair>> getAll(String prefix) {
    getAllCount++;
    List<KeyValuePair> results = new ArrayList<>();
    for (Entry<String, byte[]> entry : store.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        results.add(new KeyValuePair(entry.getKey(), entry.getValue()));
      }
    }
    return executor.submit(() -> results);
  }

  @Override
  public Future<CommitResult> commit(StorageMutation mutation) {
    for (StorageOperation operation : mutation.getOperations()) {
      if (operation instanceof Upsert) {
        Upsert upsert = (Upsert) operation;
        String key = upsert.getKey();
        if (key == null) {
          Logger.e(TAG, "Invalid StorageMutation: null key");
          return executor.submit(() -> CommitResult.FAILURE);
        }
        ByteBuffer valueBuffer = upsert.getValue();
        if (valueBuffer == null) {
          Logger.e(TAG, "Invalid StorageMutation: null value");
          return executor.submit(() -> CommitResult.FAILURE);
        }
        byte[] value = valueBuffer.array();
        if (value.length == 0) {
          Logger.e(TAG, "Invalid StorageMutation: empty value");
          return executor.submit(() -> CommitResult.FAILURE);
        }
        byte[] currentValue = store.put(key, value);
        if (currentValue == null) {
          insertCount++;
        } else {
          updateCount++;
        }
      } else {
        Logger.e(
            TAG, "Invalid StorageMutation: unexpected operation: %s", operation.getType().name());
        return executor.submit(() -> CommitResult.FAILURE);
      }
    }
    return executor.submit(() -> CommitResult.SUCCESS);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("contentItems").value(store.size());
    dumper.forKey("getCount").value(getCount);
    dumper.forKey("getAllCount").value(getAllCount).compactPrevious();
    dumper.forKey("insertCount").value(insertCount).compactPrevious();
    dumper.forKey("updateCount").value(updateCount).compactPrevious();
  }
}
