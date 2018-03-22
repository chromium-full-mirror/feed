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

package com.google.android.libraries.feed.feedmodelprovider;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;

/**
 * Factory for creating instances of {@link ModelProviderFactory} using the {@link
 * FeedModelProvider}.
 */
public class FeedModelProviderFactory implements ModelProviderFactory {
  private final SessionManager sessionManager;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final UiRunnableHandler uiRunnableHandler;

  public FeedModelProviderFactory(
      SessionManager sessionManager,
      ThreadUtils threadUtils,
      TimingUtils timingUtils,
      UiRunnableHandler uiRunnableHandler) {
    this.sessionManager = sessionManager;
    this.threadUtils = threadUtils;
    this.timingUtils = timingUtils;
    this.uiRunnableHandler = uiRunnableHandler;
  }

  @Override
  public ModelProvider create(StreamSession sessionId) {
    FeedModelProvider modelProvider =
        new FeedModelProvider(sessionManager, threadUtils, timingUtils, uiRunnableHandler);
    sessionManager.getExistingSession(sessionId, modelProvider);
    return modelProvider;
  }

  @Override
  public ModelProvider createNew() {
    FeedModelProvider modelProvider =
        new FeedModelProvider(sessionManager, threadUtils, timingUtils, uiRunnableHandler);
    sessionManager.getNewSession(modelProvider);
    return modelProvider;
  }
}
