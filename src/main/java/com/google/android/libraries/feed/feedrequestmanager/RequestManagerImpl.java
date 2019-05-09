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

package com.google.android.libraries.feed.feedrequestmanager;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.host.logging.RequestReason;
import com.google.android.libraries.feed.api.internal.requestmanager.FeedRequestManager;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;

/** Implementation of Client-visible RequestManagerApi. */
public final class RequestManagerImpl implements RequestManager {
  private static final String TAG = "RequestManagerImpl";

  private final FeedRequestManager feedRequestManager;
  private final SessionManager sessionManager;

  public RequestManagerImpl(FeedRequestManager feedRequestManager, SessionManager sessionManager) {
    this.feedRequestManager = feedRequestManager;
    this.sessionManager = sessionManager;
  }

  @Override
  public void triggerScheduledRefresh() {
    feedRequestManager.triggerRefresh(
        RequestReason.HOST_REQUESTED,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
  }
}
