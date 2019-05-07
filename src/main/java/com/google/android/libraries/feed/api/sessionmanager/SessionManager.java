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

package com.google.android.libraries.feed.api.sessionmanager;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import java.util.List;

/**
 * The SessionManager is responsible for creating and updating. A session has a one-to-one
 * relationship with a ModelProvider. The SessionManager implements the session functionality and
 * management.
 *
 * <p>TODO: this will not be needed long-term as hosts will use RequestManager without
 * this
 */
public interface SessionManager {

  /**
   * Return a {@link Consumer} which is able to handle an update to the SessionManager. An update
   * consists of a List of {@link StreamDataOperation} objects. The {@link MutationContext} captures
   * the context which is initiating the update operation.
   */
  Consumer<Result<List<StreamDataOperation>>> getUpdateConsumer(MutationContext mutationContext);
}
