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

package com.google.android.libraries.feed.common;

import com.google.android.libraries.feed.common.functional.Consumer;
import java.util.concurrent.CountDownLatch;

/**
 * This is a consumer interface to allow for synchronous use of an asynchronous method call, by
 * passing the latch in and then calling await. <b>Note that it is imperative that {@link #await()}
 * and {@link #accept(T)} are called on separate threads to avoid deadlock.</b>
 *
 * <p>Example usage:
 *
 * <pre>
 *    SynchronousConsumerLatch latch = new SynchronousConsumerLatch();
 *    asynchMethod(param1, latch); // second parameter is the Consumer
 *    latch.await();
 * </pre>
 */
public class SynchronousConsumerLatch<T> implements Consumer<T> {

  private final CountDownLatch latch = new CountDownLatch(1);

  @Override
  public void accept(T input) {
    latch.countDown();
  }

  public void await() throws InterruptedException {
    latch.await();
  }
}
