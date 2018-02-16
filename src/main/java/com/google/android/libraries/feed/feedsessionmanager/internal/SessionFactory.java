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

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.TimingUtils;

/**
 * Factory for creating a {@link FeedSession} instance used in the {@link
 * com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager}.
 */
public class SessionFactory {
  private final Store store;
  private final ModelProviderFactory modelProviderFactory;
  private final TimingUtils timingUtils;
  private final InternalProtocolUtils internalProtocolUtils;
  private final ThreadUtils threadUtils;

  public SessionFactory(
      Store store,
      ModelProviderFactory modelProviderFactory,
      TimingUtils timingUtils,
      InternalProtocolUtils internalProtocolUtils,
      ThreadUtils threadUtils) {
    this.store = store;
    this.modelProviderFactory = modelProviderFactory;
    this.timingUtils = timingUtils;
    this.internalProtocolUtils = internalProtocolUtils;
    this.threadUtils = threadUtils;
  }

  public FeedSession getSession() {
    return new SessionImpl(
        modelProviderFactory,
        store,
        timingUtils,
        internalProtocolUtils,
        threadUtils);
  }

  public ManagedSession getHeadSession() {
    return new HeadSessionImpl(store, timingUtils, internalProtocolUtils);
  }
}
