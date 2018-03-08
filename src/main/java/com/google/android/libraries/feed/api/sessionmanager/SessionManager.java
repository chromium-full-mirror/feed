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

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.host.common.Callback;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.List;
import javax.annotation.Nullable;

/** The SessionManager is responsible for creating and updating sessions within Jardin. */
public interface SessionManager {

  /** The {@link ContentId} of the global Piet Shared State. */
  // TODO: This is here until we support multiple Shared States and having them
  // available through the ContentId.
  ContentId GLOBAL_SHARED_STATE =
      ContentId.newBuilder()
          .setContentDomain("piet-shared-state")
          .setId(1)
          .setTable("piet-shared-state")
          .build();

  /**
   * This is called by the UI to get a new session. The life of the session is controlled by the
   * Session manager. The SessionManager maintains HEAD$ which represents the most current state of
   * the stream. It will also decide which changes should be made to existing sessions and the life
   * time of existing sessions.
   */
  Session getNewSession();

  /**
   * Method which causes a request to be created for the next page of content based upon the
   * continuation token. This could be called from multiple Model Providers.
   */
  void handleToken(StreamToken streamToken);

  /**
   * Returns a List of {@link StreamPayload} for each of the keys. This operation may perform disk
   * reads, therefore the payloads are returned through a callback. The returned values will contain
   * both the payload and the content id of the payload.
   */
  void getStreamFeatures(List<StreamContentId> contentIds, Callback<List<PayloadWithId>> callback);

  /**
   * Return the shared state. This operation will be fast, so it can be called on the UI Thread.
   * This method will return {@code null} if the shared state is not found.
   */
  @Nullable
  StreamSharedState getSharedState(ContentId contentId);

  /**
   * Begin and edit against HEAD$. The SessionManager will update HEAD$ and determine which sessions
   * should be updated.
   */
  SessionMutation edit();
}
