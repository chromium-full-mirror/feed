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
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentOperation;
import com.google.android.libraries.feed.host.storage.ContentOperation.Upsert;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** A {@link ContentStorage} that holds data in memory. */
public class InMemoryContentStorage implements ContentStorage, Dumpable {

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
  public Future<Map<String, byte[]>> getAll(String prefix) {
    getAllCount++;
    Map<String, byte[]> results = new HashMap<>();
    for (Entry<String, byte[]> entry : store.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        results.put(entry.getKey(), entry.getValue());
      }
    }
    return executor.submit(() -> results);
  }

  @Override
  public Future<Boolean> commit(ContentMutation mutation) {
    for (ContentOperation operation : mutation.getOperations()) {
      if (operation instanceof Upsert) {
        Upsert upsert = (Upsert) operation;
        String key = upsert.getKey();
        if (key == null) {
          Logger.e(TAG, "Invalid ContentMutation: null key");
          return executor.submit(() -> false);
        }
        ByteBuffer valueBuffer = upsert.getValue();
        if (valueBuffer == null) {
          Logger.e(TAG, "Invalid ContentMutation: null value");
          return executor.submit(() -> false);
        }
        byte[] value = valueBuffer.array();
        if (value.length == 0) {
          Logger.e(TAG, "Invalid ContentMutation: empty value");
          return executor.submit(() -> false);
        }
        byte[] currentValue = store.put(key, value);
        if (currentValue == null) {
          insertCount++;
        } else {
          updateCount++;
        }
      } else {
        Logger.e(TAG, "Invalid ContentMutation: unexpected operation: %s", operation.getType());
        return executor.submit(() -> false);
      }
    }
    return executor.submit(() -> true);
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
