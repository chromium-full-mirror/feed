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

package com.google.android.libraries.feed.piet.util;

import javax.annotation.Nullable;

/** Utility methods for working with the Producer class. */
public class Producers {

  /**
   * Instead of {@link of()} returning a lambda, use this inner class to avoid making new classes
   * for each lambda.
   */
  private static class InnerProducer<T> implements Producer<T> {
    private final T instance;

    InnerProducer(T instance) {
      this.instance = instance;
    }

    @Override
    @Nullable
    public T get() {
      return instance;
    }
  }

  /** Return a Producer that always returns the provided instance. Mainly for use in tests. */
  public static <T> Producer<T> of(T instance) {
    return new InnerProducer<T>(instance);
  }

  /** Prevent instantiation */
  private Producers() {}
}
