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

import com.google.android.libraries.feed.host.common.Callback;
import com.google.android.libraries.feed.host.storage.StorageOperation.Delete;
import com.google.android.libraries.feed.host.storage.StorageOperation.DeleteByPrefix;
import com.google.android.libraries.feed.host.storage.StorageOperation.Upsert;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sequence of {@link StorageOperation} instances. This class is used to commit changes to {@link
 * KeyValuePairStorage}.
 */
public class StorageMutation {
  private final List<StorageOperation> operations;

  private StorageMutation(List<StorageOperation> operations) {
    this.operations = Collections.unmodifiableList(operations);
  }

  /** An unmodifiable list of operations to be committed by {@link KeyValuePairStorage}. */
  public List<StorageOperation> getOperations() {
    return operations;
  }

  /**
   * Creates a {@link StorageMutation}, which can be used to apply mutations to the underlying
   * {@link KeyValuePairStorage}.
   */
  public static class Builder {
    private final ArrayList<StorageOperation> operations = new ArrayList<>();
    private final StorageOperationListSimplifier simplifier = new StorageOperationListSimplifier();

    /**
     * Sets the key/value pair in {@link KeyValuePairStorage}, or inserts it if it doesn't exist.
     *
     * <p>This method can be called repeatedly to assign multiple key/values. If the same key is
     * assigned multiple times, only the last value will be persisted.
     */
    public Builder upsert(String key, byte[] value) {
      operations.add(new Upsert(key, ByteBuffer.wrap(value)));
      return this;
    }

    /**
     * Deletes the value from {@link KeyValuePairStorage} with a matching key, if it exists.
     *
     * <p>{@link KeyValuePairStorage#commit(StorageMutation, Callback)} will fulfill with {@link
     * CommitResult#SUCCESS}, even if {@code key} is not found in {@link KeyValuePairStorage}.
     *
     * <p>If {@link Delete} and {@link Upsert} are committed for the same key, only the last
     * operation will take effect.
     */
    public Builder delete(String key) {
      operations.add(new Delete(key));
      return this;
    }

    /**
     * Deletes all values from {@link KeyValuePairStorage} with matching key prefixes.
     *
     * <p>{@link KeyValuePairStorage#commit(StorageMutation, Callback)} will fulfill with {@link
     * CommitResult#SUCCESS}, even if no keys have a matching prefix.
     *
     * <p>If {@link DeleteByPrefix} and {@link Upsert} are committed for the same matching key, only
     * the last operation will take effect.
     */
    public Builder deleteByPrefix(String prefix) {
      operations.add(new DeleteByPrefix(prefix));
      return this;
    }

    /**
     * Simplifies the sequence of {@link StorageOperation} instances, and returns a new {@link
     * StorageMutation} the simplified list.
     */
    public StorageMutation build() {
      return new StorageMutation(simplifier.simplify(operations));
    }
  }
}
