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

import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;

/** Apply a set of DataOperations to the SessionManager. */
public interface SessionMutation {
  /** Add a set of DataOperations. These will update HEAD$ and possibly other sessions. */
  void addOperation(StreamDataOperation dataOperation);

  /** This indicates the SessionMutation was due to a response from a continuation token */
  void setMutationContext(StreamToken streamToken);

  /** Commit the Mutation. */
  void commit();
}
