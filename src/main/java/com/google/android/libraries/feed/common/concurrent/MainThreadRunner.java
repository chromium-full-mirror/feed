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

package com.google.android.libraries.feed.common.concurrent;

import android.os.Handler;
import android.os.Looper;
import com.google.android.libraries.feed.common.logging.Logger;

/** A {@link AbstractRunner} that executes on the main thread (UI thread). */
public class MainThreadRunner extends AbstractRunner {
  private static final String TAG = "MainThreadRunner";
  private static final Handler handler = new Handler(Looper.getMainLooper());

  public MainThreadRunner() {
    super(handler::post, "UI thread");
  }

  /** Executes the {@code runnable} on the with a delay. */
  public void executeWithDelay(String name, Runnable runnable, int delayMs) {
    Logger.d(TAG, "Running task [%s] on [%s]", name, "UI thread");
    handler.postDelayed(runnable, delayMs);
  }
}
