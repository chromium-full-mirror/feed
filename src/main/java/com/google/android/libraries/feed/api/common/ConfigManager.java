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

/**
 * Allows the host to provide config values to Jardin libraries. These config values can come from
 * experiment values, host specific customizations, or debug overrides.
 */
public interface ConfigManager {
  <T> T getValue(ConfigFlag<T> flag);

  /**
   * A config flag definition including key, default value and expected type.
   *
   * @param <T> the expected type of the config value for this flag. Currently only Integer and
   *     String are supported.
   */
  class ConfigFlag<T> {
    private final Class<T> tClass;
    private final String key;
    private final T defaultValue;

    private ConfigFlag(Class<T> tClass, String key, T defaultValue) {
      this.tClass = tClass;
      this.key = key;
      this.defaultValue = defaultValue;
    }

    public static ConfigFlag<String> forString(String key, String defaultValue) {
      return new ConfigFlag<>(String.class, key, defaultValue);
    }

    public static ConfigFlag<Integer> forInteger(String key, int defaultValue) {
      return new ConfigFlag<>(Integer.class, key, defaultValue);
    }

    public String getKey() {
      return key;
    }

    public Class<T> getType() {
      return tClass;
    }

    public T getDefaultValue() {
      return defaultValue;
    }
  }
}
