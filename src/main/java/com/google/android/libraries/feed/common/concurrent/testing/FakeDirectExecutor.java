// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.common.concurrent.testing;

import java.util.concurrent.Executor;

/** Fake {@link Executor} that enforces a background thread. */
public final class FakeDirectExecutor implements Executor {
  private final FakeThreadUtils fakeThreadUtils;

  public FakeDirectExecutor(FakeThreadUtils fakeThreadUtils) {
    this.fakeThreadUtils = fakeThreadUtils;
  }

  @Override
  public void execute(Runnable command) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    try {
      command.run();
    } finally {
      fakeThreadUtils.enforceMainThread(policy);
    }
  }
}
