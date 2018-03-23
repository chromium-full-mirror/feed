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

package com.google.android.libraries.feed.host.config;

import android.support.annotation.StringDef;
import java.util.HashMap;
import java.util.List;

/**
 * Allows the host to provide config values to Jardin libraries. These config values can come from
 * experiment values, host specific customizations, or debug overrides.
 */
// TODO: Configuration needs plenty of adjustment.
public class ConfigManager {
  @StringDef({
    ConfigValues.FEED_SERVER_HOST,
    ConfigValues.FEED_SERVER_PATH_AND_PARAMS,
    ConfigValues.OPTIMISTIC_SESSION_WRITES
  })
  public @interface ConfigValues {
    String FEED_SERVER_HOST = "feed_server_host";
    String FEED_SERVER_PATH_AND_PARAMS = "feed_server_path_and_params";
    String OPTIMISTIC_SESSION_WRITES = "optimistic_session_writes";
  }

  private final HashMap<String, ConfigFlag<?>> values = new HashMap<>();

  public ConfigManager(List<ConfigFlag<?>> overrides) {
    values.put(
        ConfigValues.OPTIMISTIC_SESSION_WRITES,
        ConfigFlag.forBoolean(ConfigValues.OPTIMISTIC_SESSION_WRITES, false));
    for (ConfigFlag<?> override : overrides) {
      values.put(override.getKey(), override);
    }
  }

  public <T> T getValue(String key, T defaultValue) {
    ConfigFlag<?> value = values.get(key);
    return (value != null) ? (T) value.getValue() : defaultValue;
  }

  /**
   * A key-value pair with an expected type to serialize from and deserialize to.
   *
   * @param <T> the expected type of the config value for this flag. Currently only Integer and
   *     String are supported.
   */
  public static class ConfigFlag<T> {
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

    public static ConfigFlag<Boolean> forBoolean(String key, boolean value) {
      return new ConfigFlag<>(Boolean.class, key, value);
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
