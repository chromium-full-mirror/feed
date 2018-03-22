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

package com.google.android.libraries.feed.api.scope;

import com.google.android.libraries.feed.api.common.Clock;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.SystemClockImpl;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.feedrequestmanager.FeedRequestManager;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.config.ConfigManager;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

/**
 * Per-process instance of the feed library.
 *
 * <p>It's the host's responsibility to make sure there's only one instance of this per process, per
 * user.
 */
public class FeedProcessScope implements Dumpable {
  private static final String TAG = "FeedProcessScope";

  public FeedStreamScope.Builder createFeedStreamScopeBuilder(ActionApi actionApi) {
    return new FeedStreamScope.Builder(
        actionApi,
        imageLoaderApi,
        loggingApi,
        protocolAdapter,
        sessionManager,
        threadUtils,
        timingUtils,
        uiRunnableHandler,
        clock);
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.dump(protocolAdapter);
    dumper.dump(timingUtils);
    if (sessionManager instanceof Dumpable) {
      dumper.dump((Dumpable) sessionManager);
    }
    if (store instanceof Dumpable) {
      dumper.dump((Dumpable) store);
    }
  }

  public void onDestroy() {
    try {
      networkClient.close();
    } catch (Exception ignored) {
      // Ignore exception when closing.
    }
  }

  /** A builder that creates a {@link FeedProcessScope}. */
  public static class Builder {

    /** The APIs are all required to construct the scope. */
    public Builder(
        ConfigManager configManager,
        ImageLoaderApi imageLoaderApi,
        LoggingApi loggingApi,
        NetworkClient networkClient,
        SchedulerApi schedulerApi) {
      this.configManager = configManager;
      this.imageLoaderApi = imageLoaderApi;
      this.loggingApi = loggingApi;
      this.networkClient = networkClient;
      this.schedulerApi = schedulerApi;
    }

    public Builder setProtocolAdapter(FeedProtocolAdapter protocolAdapter) {
      this.protocolAdapter = protocolAdapter;
      return this;
    }

    public Builder setRequestManager(RequestManager requestManager) {
      this.requestManager = requestManager;
      return this;
    }

    public Builder setSessionManager(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
      return this;
    }

    public Builder setProtoExtensionProvider(ProtoExtensionProvider protoExtensionProvider) {
      this.protoExtensionProvider = protoExtensionProvider;
      return this;
    }

    public Builder setContentStorage(ContentStorage contentStorage) {
      this.contentStorage = contentStorage;
      return this;
    }

    public Builder setJournalStorage(JournalStorage journalStorage) {
      this.journalStorage = journalStorage;
      return this;
    }

    public FeedProcessScope build() {
      // Build default component instances if necessary.
      if (protoExtensionProvider == null) {
        // Return an empty list of extensions by default.
        protoExtensionProvider = () -> new ArrayList<>();
      }
      FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(protoExtensionProvider);
      if (contentStorage == null) {
        contentStorage = new InMemoryContentStorage();
      }
      if (journalStorage == null) {
        journalStorage = new InMemoryJournalStorage();
      }
      TimingUtils timingUtils = new TimingUtils();
      FeedStore store =
          new FeedStore(timingUtils, extensionRegistry, contentStorage, journalStorage);
      UiRunnableHandler uiRunnableHandler = new UiRunnableHandlerImpl();
      ThreadUtils threadUtils = new ThreadUtils();
      if (sessionManager == null) {
        sessionManager =
            new FeedSessionManager(
                Executors.newSingleThreadExecutor(),
                store,
                timingUtils,
                threadUtils,
                configManager);
      }
      if (protocolAdapter == null) {
        protocolAdapter = new FeedProtocolAdapter(sessionManager, timingUtils);
      }
      if (requestManager == null) {
        requestManager =
            new FeedRequestManager(
                configManager, networkClient, protocolAdapter, extensionRegistry);
      }
      if (sessionManager instanceof FeedSessionManager) {
        ((FeedSessionManager) sessionManager).setProtocolAdapter(protocolAdapter);
        ((FeedSessionManager) sessionManager).setRequestManager(requestManager);
      }

      AppLifecycleListener appLifecycleListener = new FeedAppLifecycleListener();

      return new FeedProcessScope(
          configManager,
          imageLoaderApi,
          loggingApi,
          networkClient,
          protocolAdapter,
          requestManager,
          schedulerApi,
          sessionManager,
          store,
          timingUtils,
          threadUtils,
          uiRunnableHandler,
          extensionRegistry,
          appLifecycleListener,
          new SystemClockImpl());
    }

    // Required fields.
    private final ConfigManager configManager;
    private final ImageLoaderApi imageLoaderApi;
    private final LoggingApi loggingApi;
    private final NetworkClient networkClient;
    private final SchedulerApi schedulerApi;

    // Optional fields - if they are not provided, we will use default implementations.
    @Nullable private RequestManager requestManager = null;
    @Nullable private SessionManager sessionManager = null;
    @Nullable private FeedProtocolAdapter protocolAdapter = null;
    @Nullable private ProtoExtensionProvider protoExtensionProvider = null;
    @Nullable private ContentStorage contentStorage = null;
    @Nullable private JournalStorage journalStorage = null;
  }

  private FeedProcessScope(
      ConfigManager configManager,
      ImageLoaderApi imageLoaderApi,
      LoggingApi loggingApi,
      NetworkClient networkClient,
      FeedProtocolAdapter protocolAdapter,
      RequestManager requestManager,
      SchedulerApi schedulerApi,
      SessionManager sessionManager,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      UiRunnableHandler uiRunnableHandler,
      FeedExtensionRegistry extensionRegistry,
      AppLifecycleListener appLifecycleListener,
      Clock clock) {
    this.configManager = configManager;
    this.imageLoaderApi = imageLoaderApi;
    this.loggingApi = loggingApi;
    this.networkClient = networkClient;
    this.protocolAdapter = protocolAdapter;
    this.requestManager = requestManager;
    this.schedulerApi = schedulerApi;
    this.sessionManager = sessionManager;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.uiRunnableHandler = uiRunnableHandler;
    this.extensionRegistry = extensionRegistry;
    this.appLifecycleListener = appLifecycleListener;
    this.clock = clock;
  }

  private final ConfigManager configManager;
  private final ImageLoaderApi imageLoaderApi;
  private final LoggingApi loggingApi;
  private final NetworkClient networkClient;
  private final FeedProtocolAdapter protocolAdapter;
  private final RequestManager requestManager;
  private final SchedulerApi schedulerApi;
  private final SessionManager sessionManager;
  private final Store store;
  private final TimingUtils timingUtils;
  private final ThreadUtils threadUtils;
  private final UiRunnableHandler uiRunnableHandler;
  private final FeedExtensionRegistry extensionRegistry;
  private final AppLifecycleListener appLifecycleListener;
  private final Clock clock;

  public FeedProtocolAdapter getProtocolAdapter() {
    return protocolAdapter;
  }

  public RequestManager getRequestManager() {
    return requestManager;
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }

  public FeedExtensionRegistry getExtensionRegistry() {
    return extensionRegistry;
  }

  public AppLifecycleListener getAppLifecycleListener() {
    return appLifecycleListener;
  }
}
