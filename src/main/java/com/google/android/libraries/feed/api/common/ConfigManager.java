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

package com.google.android.libraries.feed.api.common;

import javax.annotation.Nullable;

/**
 * Allows the host to provide config values to Jardin libraries. These config values can come from
 * experiment values, host specific customizations, or debug overrides.
 */
// TODO: Configuration needs plenty of adjustment.
public interface ConfigManager {
  @Nullable
  <T> T getValue(ConfigFlag<T> flag);

  /**
   * A key-value pair with an expected type to serialize from and deserialize to.
   *
   * @param <T> the expected type of the config value for this flag. Currently only Integer and
   *     String are supported.
   */
  class ConfigFlag<T> {
    private final Class<T> type;
    private final String key;
    private final T value;

    private ConfigFlag(Class<T> type, String key, T value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }

    public static ConfigFlag<String> forString(String key, String value) {
      return new ConfigFlag<>(String.class, key, value);
    }

    public static ConfigFlag<Integer> forInteger(String key, int value) {
      return new ConfigFlag<>(Integer.class, key, value);
    }

    public String getKey() {
      return key;
    }

    public Class<T> getType() {
      return type;
    }

    public T getValue() {
      return value;
    }
  }
}
