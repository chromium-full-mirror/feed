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

package com.google.android.libraries.feed.testing.actionmanager;

import com.google.android.libraries.feed.api.internal.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.internal.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.api.internal.common.DismissActionWithSemanticProperties;
import com.google.android.libraries.feed.common.Result;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fake implementation of {@link ActionReader}. */
public final class FakeActionReader implements ActionReader {
  private final ArrayList<DismissActionWithSemanticProperties> dismissActions = new ArrayList<>();
  private final Map<String, OpaqueActionData> actionProperties = new HashMap<>();

  @Override
  public Result<List<DismissActionWithSemanticProperties>>
      getDismissActionsWithSemanticProperties() {
    return Result.success(dismissActions);
  }

  @Override
  public Result<List<ActionPropertiesWithId>> getActionProperties(List<String> contentIds) {
    ArrayList<ActionPropertiesWithId> result = new ArrayList<>();
    for (String contentId : contentIds) {
      OpaqueActionData data = actionProperties.get(contentId);
      if (data != null) {
        result.add(new ActionPropertiesWithId(contentId, data));
      }
    }
    return Result.success(result);
  }

  /** Adds a dismiss action with semantic properties. */
  public FakeActionReader addDismissActionsWithSemanticProperties(
      DismissActionWithSemanticProperties... dismissActionsToAdd) {
    Collections.addAll(dismissActions, dismissActionsToAdd);
    return this;
  }

  /** Adds action properties for the {@code contentId}. */
  public FakeActionReader addActionProperties(String contentId, OpaqueActionData data) {
    actionProperties.put(contentId, data);
    return this;
  }
}
