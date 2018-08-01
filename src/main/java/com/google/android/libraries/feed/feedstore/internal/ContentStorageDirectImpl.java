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

package com.google.android.libraries.feed.feedstore.internal;

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.SimpleSettableFuture;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * An implementation of {@link ContentStorageDirect} which converts {@link ContentStorage} to a
 * synchronized implementation. This acts as a wrapper class over ContentStorage. It will provide a
 * consumer calling on the main thread and waiting on a Future to complete to return the consumer
 * results.
 */
public class ContentStorageDirectImpl implements ContentStorageDirect {
  private static final String TAG = "ContentStorageDirectImpl";
  private final MainThreadRunner mainThreadRunner;
  private final ContentStorage contentStorage;

  public ContentStorageDirectImpl(
      ContentStorage contentStorage, MainThreadRunner mainThreadRunner) {
    this.contentStorage = contentStorage;
    this.mainThreadRunner = mainThreadRunner;
  }

  /** Execute a task with a {@link Consumer}. */
  interface ConsumerTask<T> {
    void execute(Consumer<T> consumer);
  }

  /** Run a {@link ConsumerTask} on the main thread, providing a Consumer to receive the results. */
  private <T> T mainThreadCaller(ConsumerTask<T> task, T failure) {
    SimpleSettableFuture<T> sharedStatesFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(TAG, () -> task.execute(sharedStatesFuture::put));
    try {
      return sharedStatesFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, null);
      return failure;
    }
  }

  @Override
  public Result<Map<String, byte[]>> get(List<String> keys) {
    Logger.i(TAG, "DirectCall - get");
    return mainThreadCaller((consumer) -> contentStorage.get(keys, consumer), Result.failure());
  }

  @Override
  public Result<Map<String, byte[]>> getAll(String prefix) {
    Logger.i(TAG, "DirectCall - getAll");
    return mainThreadCaller(
        (consumer) -> contentStorage.getAll(prefix, consumer), Result.failure());
  }

  @Override
  public CommitResult commit(ContentMutation mutation) {
    Logger.i(TAG, "DirectCall - commit");
    return mainThreadCaller(
        (consumer) -> contentStorage.commit(mutation, consumer), CommitResult.FAILURE);
  }

  @Override
  public Result<List<String>> getAllKeys() {
    Logger.i(TAG, "DirectCall - getAllKeys");
    return mainThreadCaller(contentStorage::getAllKeys, Result.failure());
  }
}
