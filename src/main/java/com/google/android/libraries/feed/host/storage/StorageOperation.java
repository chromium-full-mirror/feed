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

import android.support.annotation.IntDef;
import java.nio.ByteBuffer;

/** A mutation to the underlying {@link ContentStorage}. */
public abstract class StorageOperation {
  /** The types of operations. */
  @IntDef({Type.DELETE, Type.DELETE_BY_PREFIX, Type.UPSERT})
  @interface Type {
    int DELETE = 0;
    int DELETE_BY_PREFIX = 1;
    int UPSERT = 3;
  }

  public @Type int getType() {
    return type;
  }

  private final @Type int type;

  // Only the following classes may extend StorageOperation
  private StorageOperation(@Type int type) {
    this.type = type;
  }

  /**
   * A {@link StorageOperation} created by calling {@link StorageMutation.Builder#upsert(String,
   * byte[])}.
   */
  public static class Upsert extends StorageOperation {
    private final String key;
    private final ByteBuffer value;

    Upsert(String key, ByteBuffer value) {
      super(Type.UPSERT);
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public ByteBuffer getValue() {
      return value;
    }
  }

  /**
   * A {@link StorageOperation} created by calling {@link StorageMutation.Builder#delete(String)}.
   */
  public static class Delete extends StorageOperation {
    private final String key;

    Delete(String key) {
      super(Type.DELETE);
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }

  /**
   * A {@link StorageOperation} created by calling {@link
   * StorageMutation.Builder#deleteByPrefix(String)}.
   */
  public static class DeleteByPrefix extends StorageOperation {
    private final String prefix;

    DeleteByPrefix(String prefix) {
      super(Type.DELETE_BY_PREFIX);
      this.prefix = prefix;
    }

    public String getPrefix() {
      return prefix;
    }
  }
}
