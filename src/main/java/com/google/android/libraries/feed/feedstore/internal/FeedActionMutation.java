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

package com.google.android.libraries.feed.feedstore.internal;

import com.google.android.libraries.feed.api.store.ActionMutation;
import com.google.android.libraries.feed.common.functional.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Implementation of {@link ActionMutation} */
public class FeedActionMutation implements ActionMutation {

  private final Map<Integer, List<String>> actions = new HashMap<>();
  private final Consumer<Map<Integer, List<String>>> consumer;

  public FeedActionMutation(Consumer<Map<Integer, List<String>>> consumer) {
    this.consumer = consumer;
  }

  @Override
  public ActionMutation add(int action, String contentId) {
    /*@Nullable*/ List<String> actionsForType = actions.get(action);
    if (actionsForType == null) {
      actionsForType = new ArrayList<>();
    }
    actionsForType.add(contentId);
    actions.put(action, actionsForType);
    return this;
  }

  @Override
  public void commit() {
    consumer.accept(actions);
  }
}
