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

/**
 * A settable version of {@link Supplier} allowing the value to be set or changed after the Supplier
 * has been created.
 *
 * @param <T> The type of the producer.
 */
public class SettableSupplier<T> implements Supplier<T> {
  private T value;

  @Override
  public T get() {
    return Validators.checkNotNull(value);
  }

  /**
   * Set the value, overriding the previous value. This must be called before {@link #get()} is
   * called. {@link #get()} will throw and {@link NullPointerException} if the value is not set.
   */
  public void set(T value) {
    this.value = value;
  }
}
