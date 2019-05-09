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

package com.google.android.libraries.feed.api.internal.common;

import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import java.util.Arrays;

/**
 * Structure style class which binds a String contentId with the action properties data for that
 * contentId. The class is immutable and provides access to the fields directly.
 */
public final class ActionPropertiesWithId {

  public final String contentId;
  public final OpaqueActionData actionData;

  public ActionPropertiesWithId(String contentId, OpaqueActionData actionData) {
    this.contentId = contentId;
    this.actionData = actionData;
  }

  @Override
  public boolean equals(/*@Nullable*/ Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ActionPropertiesWithId that = (ActionPropertiesWithId) o;

    return contentId.equals(that.contentId)
        && Arrays.equals(actionData.toByteArray(), that.actionData.toByteArray());
  }

  @Override
  public int hashCode() {
    int result = contentId.hashCode();
    result = 31 * result + actionData.hashCode();
    return result;
  }
}
