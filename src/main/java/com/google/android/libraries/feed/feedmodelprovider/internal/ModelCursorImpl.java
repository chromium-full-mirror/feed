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

package com.google.android.libraries.feed.feedmodelprovider.internal;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Validators;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Implementation of {@link ModelCursor}. */
public class ModelCursorImpl implements ModelCursor, Dumpable {
  private static final String TAG = "ModelCursorImpl";

  private final Object lock = new Object();

  private final ThreadUtils threadUtils;

  @Nullable
  @GuardedBy("lock")
  private Iterator<UpdatableModelChild> iterator;

  @GuardedBy("lock")
  private boolean isInvalid = false;

  public ModelCursorImpl(Iterator<UpdatableModelChild> iterator, ThreadUtils threadUtils) {
    Validators.checkNotNull(iterator);
    this.iterator = iterator;
    this.threadUtils = threadUtils;
  }

  @Override
  public boolean advanceCursor(ProcessNextChild processor) {
    // This should only be called on the UI thread.
    threadUtils.checkMainThread();

    ModelChild nextChild;
    synchronized (lock) {
      if (isInvalid || iterator == null || !iterator.hasNext()) {
        return false;
      }
      nextChild = iterator.next();
    }
    processor.processNextChild(nextChild);
    return true;
  }

  @Override
  public void release() {
    // This could be called on a background thread.
    synchronized (lock) {
      isInvalid = true;
      iterator = null;
    }
  }

  @Override
  public boolean isInvalid() {
    synchronized (lock) {
      return isInvalid;
    }
  }

  @Override
  public boolean isAtEnd() {
    synchronized (lock) {
      return iterator == null || !this.iterator.hasNext();
    }
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("atEnd").value(isAtEnd());
    dumper.forKey("isInvalid").value(isInvalid()).compactPrevious();
  }
}
