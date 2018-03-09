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
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A {@code ManagedSession} is a Session which support management features so the SessionManager can
 * manage the session.
 */
public interface ManagedSession {

  /** Called to update the Session, this is responsible for updating the Stored session instance. */
  void updateSession(
      List<StreamStructure> streamStructures, @Nullable StreamToken mutationSourceToken);

  /** The unique identifier created by the storage layer for the session. */
  StreamSession getStreamSession();
}
