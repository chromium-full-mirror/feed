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

package com.google.android.libraries.feed.common.testing;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.host.storage.InMemoryKeyValuePairStorage;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.android.libraries.feed.host.storage.KeyValuePairStorage;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * This is a Scope type object which is used in the Infrastructure Integration tests. It sets the
 * Feed objects from ProtocolAdapter through the SessionManager.
 */
public class InfrastructureIntegrationScope {

  private final FeedSessionManager feedSessionManager;
  private final FeedProtocolAdapter feedProtocolAdapter;
  private final FeedModelProviderFactory modelProviderFactory;

  InfrastructureIntegrationScope(
      ThreadUtils threadUtils, RequestManager mockRequestManager, ExecutorService executorService) {
    TimingUtils timingUtils = new TimingUtils();
    UiRunnableHandler uiRunnableHandler = new UiRunnableHandlerImpl();

    FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(new ExtensionProvider());
    KeyValuePairStorage keyValuePairStorage = new InMemoryKeyValuePairStorage();
    JournalStorage journalStorage = new InMemoryJournalStorage();
    FeedStore store =
        new FeedStore(timingUtils, extensionRegistry, keyValuePairStorage, journalStorage);
    feedSessionManager = new FeedSessionManager(executorService, store, timingUtils, threadUtils);
    modelProviderFactory =
        new FeedModelProviderFactory(
            feedSessionManager, threadUtils, timingUtils, uiRunnableHandler);
    feedProtocolAdapter = new FeedProtocolAdapter(feedSessionManager, timingUtils);
    feedSessionManager.setProtocolAdapter(feedProtocolAdapter);
    feedSessionManager.setRequestManager(mockRequestManager);
  }

  public ProtocolAdapter getProtocolAdapter() {
    return feedProtocolAdapter;
  }

  public SessionManager getSessionManager() {
    return feedSessionManager;
  }

  public ModelProviderFactory getModelProviderFactory() {
    return modelProviderFactory;
  }

  private static class ExtensionProvider implements ProtoExtensionProvider {
    @Override
    public List<GeneratedExtension<?, ?>> getProtoExtensions() {
      return new ArrayList<>();
    }
  }

  /** Builder for creating the {@link InfrastructureIntegrationScope} */
  public static class Builder {
    private final ThreadUtils mockThreadUtils;
    private final RequestManager mockRequestManager;
    private final ExecutorService executorService;

    public Builder(
        ThreadUtils mockThreadUtils,
        RequestManager mockRequestManager,
        ExecutorService executorService) {
      this.mockThreadUtils = mockThreadUtils;
      this.mockRequestManager = mockRequestManager;
      this.executorService = executorService;
    }

    public InfrastructureIntegrationScope build() {
      return new InfrastructureIntegrationScope(
          mockThreadUtils, mockRequestManager, executorService);
    }
  }
}
