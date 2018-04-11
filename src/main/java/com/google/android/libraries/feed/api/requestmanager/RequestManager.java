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

package com.google.android.libraries.feed.api.requestmanager;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;

/** Creates and issues requests to the server. */
public interface RequestManager {

  /**
   * Issues a request for the next page of data.
   *
   * <p>Called when the user scrolls near the end of the feed to fetch the next page, or more of the
   * carousel.
   *
   * @param mutationContext This must contain the token and optionally the session making the
   *     request.
   */
  void loadMore(MutationContext mutationContext);

  /**
   * Issues a request to refresh the entire feed.
   *
   * <p>This allows the host app to manually control refresh behavior.
   *
   * @param reason the reason why the refresh is being requested.
   */
  void triggerRefresh(RequestReason reason);

  /** Handle a asynchronous response from the Network Client */
  void handlePushedResponseBytes(byte[] responseBytes);
}
