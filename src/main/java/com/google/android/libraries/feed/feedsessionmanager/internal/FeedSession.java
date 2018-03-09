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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.android.libraries.feed.api.sessionmanager.Session;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.List;

/**
 * This defines a fully defined {@link Session} for the {@link
 * com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager}. This supports initially
 * populating the Session.
 */
public interface FeedSession extends Session, ManagedSession {

  /** Called to initialize the session from $HEAD */
  void populateModelProvider(List<StreamStructure> session);

  /**
   * Sets the StreamSession on the Session. This is created by the Store after the initial Session
   * is created. It must be set for the SessionImpl implementation to work correctly.
   */
  void setStreamSession(StreamSession streamSession);
}
