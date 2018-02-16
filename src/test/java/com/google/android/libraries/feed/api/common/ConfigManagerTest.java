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

import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.api.common.ConfigManager.ConfigFlag;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link ConfigManager}. */
@RunWith(RobolectricTestRunner.class)
public class ConfigManagerTest {

  @Test
  public void testConfigFlag_string() {
    String key = "key";
    String defaultValue = "value";
    ConfigFlag<String> configFlag = ConfigFlag.forString(key, defaultValue);
    assertThat(configFlag).isNotNull();
    assertThat(configFlag.getKey()).isEqualTo(key);
    assertThat(configFlag.getDefaultValue()).isEqualTo(defaultValue);
    assertThat(configFlag.getType()).isEqualTo(String.class);
  }

  @Test
  public void testConfigFlag_integer() {
    String key = "key";
    int defaultValue = 17;
    ConfigFlag<Integer> configFlag = ConfigFlag.forInteger(key, defaultValue);
    assertThat(configFlag).isNotNull();
    assertThat(configFlag.getKey()).isEqualTo(key);
    assertThat(configFlag.getDefaultValue()).isEqualTo(defaultValue);
    assertThat(configFlag.getType()).isEqualTo(Integer.class);
  }
}
