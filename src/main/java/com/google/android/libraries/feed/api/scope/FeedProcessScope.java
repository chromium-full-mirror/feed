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

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.client.knowncontent.KnownContent;
import com.google.android.libraries.feed.api.client.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.host.action.ActionApi;
import com.google.android.libraries.feed.api.host.config.ApplicationInfo;
import com.google.android.libraries.feed.api.host.config.Configuration;
import com.google.android.libraries.feed.api.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.api.host.config.DebugBehavior;
import com.google.android.libraries.feed.api.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.api.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.api.host.network.NetworkClient;
import com.google.android.libraries.feed.api.host.offlineindicator.OfflineIndicatorApi;
import com.google.android.libraries.feed.api.host.proto.ProtoExtensionProvider;
import com.google.android.libraries.feed.api.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.api.host.storage.ContentStorage;
import com.google.android.libraries.feed.api.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.api.host.storage.JournalStorage;
import com.google.android.libraries.feed.api.host.storage.JournalStorageDirect;
import com.google.android.libraries.feed.api.host.stream.CardConfiguration;
import com.google.android.libraries.feed.api.host.stream.SnackbarApi;
import com.google.android.libraries.feed.api.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.api.host.stream.TooltipApi;
import com.google.android.libraries.feed.api.host.stream.TooltipSupportedApi;
import com.google.android.libraries.feed.api.internal.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.internal.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.internal.common.ThreadUtils;
import com.google.android.libraries.feed.api.internal.knowncontent.FeedKnownContent;
import com.google.android.libraries.feed.api.internal.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.internal.requestmanager.FeedRequestManager;
import com.google.android.libraries.feed.api.internal.sessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.api.internal.store.Store;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.api.requestmanager.ActionUploadRequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.concurrent.DirectHostSupported;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.SystemClockImpl;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedactionmanager.FeedActionManagerImpl;
import com.google.android.libraries.feed.feedactionreader.FeedActionReader;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedknowncontent.FeedKnownContentImpl;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.feedrequestmanager.FeedActionUploadRequestManager;
import com.google.android.libraries.feed.feedrequestmanager.FeedRequestManagerImpl;
import com.google.android.libraries.feed.feedrequestmanager.RequestManagerImpl;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManagerFactory;
import com.google.android.libraries.feed.feedstore.ContentStorageDirectImpl;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.feedstore.JournalStorageDirectImpl;
import com.google.android.libraries.feed.hostimpl.network.NetworkClientWrapper;
import com.google.android.libraries.feed.hostimpl.scheduler.SchedulerApiWrapper;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Per-process instance of the feed library.
 *
 * <p>It's the host's responsibility to make sure there's only one instance of this per process, per
 * user.
 */
public final class FeedProcessScope implements Dumpable {
  private static final String TAG = "FeedProcessScope";

  private final NetworkClient networkClient;
  private final ProtocolAdapter protocolAdapter;
  private final RequestManager requestManager;
  private final FeedSessionManager feedSessionManager;
  private final Store store;
  private final TimingUtils timingUtils;
  private final ThreadUtils threadUtils;
  private final TaskQueue taskQueue;
  private final MainThreadRunner mainThreadRunner;
  private final AppLifecycleListener appLifecycleListener;
  private final Clock clock;
  private final DebugBehavior debugBehavior;
  private final ActionManager actionManager;
  private final Configuration configuration;
  private final FeedKnownContent feedKnownContent;
  private final FeedExtensionRegistry feedExtensionRegistry;
  private final ClearAllListener clearAllListener;
  private final BasicLoggingApi basicLoggingApi;
  private final TooltipSupportedApi tooltipSupportedApi;
  private final ApplicationInfo applicationInfo;

  /**
   * Return a {@link Builder} to create a FeedProcessScope
   *
   * <p>This is called by hosts so it must be public
   */
  public FeedStreamScope.Builder createFeedStreamScopeBuilder(
      Context context,
      ImageLoaderApi imageLoaderApi,
      ActionApi actionApi,
      StreamConfiguration streamConfiguration,
      CardConfiguration cardConfiguration,
      SnackbarApi snackbarApi,
      OfflineIndicatorApi offlineIndicatorApi,
      TooltipApi tooltipApi) {
    return new FeedStreamScope.Builder(
        context,
        actionApi,
        imageLoaderApi,
        protocolAdapter,
        feedSessionManager,
        threadUtils,
        timingUtils,
        taskQueue,
        mainThreadRunner,
        clock,
        debugBehavior,
        streamConfiguration,
        cardConfiguration,
        actionManager,
        configuration,
        snackbarApi,
        basicLoggingApi,
        offlineIndicatorApi,
        feedKnownContent,
        tooltipApi,
        tooltipSupportedApi,
        applicationInfo,
        feedExtensionRegistry);
  }

  /** Created through the {@link Builder}. */
  private FeedProcessScope(
      BasicLoggingApi basicLoggingApi,
      NetworkClient networkClient,
      ProtocolAdapter protocolAdapter,
      RequestManager requestManager,
      FeedSessionManager feedSessionManager,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      TaskQueue taskQueue,
      MainThreadRunner mainThreadRunner,
      AppLifecycleListener appLifecycleListener,
      Clock clock,
      DebugBehavior debugBehavior,
      ActionManager actionManager,
      Configuration configuration,
      FeedKnownContent feedKnownContent,
      FeedExtensionRegistry feedExtensionRegistry,
      ClearAllListener clearAllListener,
      TooltipSupportedApi tooltipSupportedApi,
      ApplicationInfo applicationInfo) {
    this.basicLoggingApi = basicLoggingApi;
    this.networkClient = networkClient;
    this.protocolAdapter = protocolAdapter;
    this.requestManager = requestManager;
    this.feedSessionManager = feedSessionManager;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.taskQueue = taskQueue;
    this.mainThreadRunner = mainThreadRunner;
    this.appLifecycleListener = appLifecycleListener;
    this.clock = clock;
    this.debugBehavior = debugBehavior;
    this.actionManager = actionManager;
    this.configuration = configuration;
    this.feedKnownContent = feedKnownContent;
    this.feedExtensionRegistry = feedExtensionRegistry;
    this.clearAllListener = clearAllListener;
    this.tooltipSupportedApi = tooltipSupportedApi;
    this.applicationInfo = applicationInfo;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    if (protocolAdapter instanceof Dumpable) {
      dumper.dump((Dumpable) protocolAdapter);
    }
    dumper.dump(timingUtils);
    if (feedSessionManager instanceof Dumpable) {
      dumper.dump((Dumpable) feedSessionManager);
    }
    if (store instanceof Dumpable) {
      dumper.dump((Dumpable) store);
    }
    dumper.dump(clearAllListener);
  }

  public void onDestroy() {
    try {
      Logger.i(TAG, "FeedProcessScope onDestroy called");
      networkClient.close();
      taskQueue.reset();
      taskQueue.completeReset();
    } catch (Exception ignored) {
      // Ignore exception when closing.
    }
  }

  @Deprecated
  public ProtocolAdapter getProtocolAdapter() {
    return protocolAdapter;
  }


  public RequestManager getRequestManager() {
    return requestManager;
  }

  public SessionManager getSessionManager() {
    return feedSessionManager;
  }

  @Deprecated
  public TimingUtils getTimingUtils() {
    return timingUtils;
  }

  public TaskQueue getTaskQueue() {
    return taskQueue;
  }

  public AppLifecycleListener getAppLifecycleListener() {
    return appLifecycleListener;
  }

  @Deprecated
  public ActionManager getActionManager() {
    return actionManager;
  }

  public KnownContent getKnownContent() {
    return feedKnownContent;
  }

  @Deprecated
  public FeedExtensionRegistry getFeedExtensionRegistry() {
    return feedExtensionRegistry;
  }

  /** A builder that creates a {@link FeedProcessScope}. */
  public static final class Builder {

    // Required fields.
    private final Configuration configuration;
    final Executor singleThreadExecutor;
    private final BasicLoggingApi basicLoggingApi;
    private final TooltipSupportedApi tooltipSupportedApi;
    private final NetworkClient unwrappedNetworkClient;
    private final SchedulerApi unwrappedSchedulerApi;
    private final DebugBehavior debugBehavior;
    private final Context context;
    private final ApplicationInfo applicationInfo;

    // Optional fields - if they are not provided, we will use default implementations.
    /*@MonotonicNonNull*/ private ProtoExtensionProvider protoExtensionProvider = null;
    /*@MonotonicNonNull*/ ContentStorageDirect contentStorage = null;
    /*@MonotonicNonNull*/ JournalStorageDirect journalStorage = null;
    /*@MonotonicNonNull*/ ContentStorage rawContentStorage = null;
    /*@MonotonicNonNull*/ JournalStorage rawJournalStorage = null;
    /*@MonotonicNonNull*/ private Clock clock;

    // This will be overridden in tests.
    private ThreadUtils threadUtils = new ThreadUtils();

    /** The APIs are all required to construct the scope. */
    public Builder(
        Configuration configuration,
        Executor singleThreadExecutor,
        BasicLoggingApi basicLoggingApi,
        NetworkClient networkClient,
        SchedulerApi schedulerApi,
        DebugBehavior debugBehavior,
        Context context,
        ApplicationInfo applicationInfo,
        TooltipSupportedApi tooltipSupportedApi) {
      this.configuration = configuration;
      this.singleThreadExecutor = singleThreadExecutor;
      this.basicLoggingApi = basicLoggingApi;
      this.debugBehavior = debugBehavior;
      this.context = context;
      this.applicationInfo = applicationInfo;
      this.unwrappedNetworkClient = networkClient;
      this.unwrappedSchedulerApi = schedulerApi;
      this.tooltipSupportedApi = tooltipSupportedApi;
    }

    public Builder setProtoExtensionProvider(ProtoExtensionProvider protoExtensionProvider) {
      this.protoExtensionProvider = protoExtensionProvider;
      return this;
    }

    public Builder setContentStorage(ContentStorage contentStorage) {
      rawContentStorage = contentStorage;
      return this;
    }

    public Builder setContentStorageDirect(ContentStorageDirect contentStorage) {
      this.contentStorage = contentStorage;
      return this;
    }

    public Builder setJournalStorage(JournalStorage journalStorage) {
      rawJournalStorage = journalStorage;
      return this;
    }

    public Builder setJournalStorageDirect(JournalStorageDirect journalStorage) {
      this.journalStorage = journalStorage;
      return this;
    }

    // This is really exposed for tests to override the thread checking
    Builder setThreadUtils(ThreadUtils threadUtils) {
      this.threadUtils = threadUtils;
      return this;
    }

    public Builder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    @VisibleForTesting
    ContentStorageDirect buildContentStorage(MainThreadRunner mainThreadRunner) {
      if (contentStorage == null) {
        boolean useDirect = configuration.getValueOrDefault(ConfigKey.USE_DIRECT_STORAGE, false);
        if (useDirect
            && rawContentStorage != null
            && rawContentStorage instanceof ContentStorageDirect) {
          contentStorage = (ContentStorageDirect) rawContentStorage;
        } else if (rawContentStorage != null) {
          contentStorage = new ContentStorageDirectImpl(rawContentStorage, mainThreadRunner);
        } else {
          contentStorage = new InMemoryContentStorage();
        }
      }
      return contentStorage;
    }

    @VisibleForTesting
    JournalStorageDirect buildJournalStorage(MainThreadRunner mainThreadRunner) {
      if (journalStorage == null) {
        boolean useDirect = configuration.getValueOrDefault(ConfigKey.USE_DIRECT_STORAGE, false);
        if (useDirect
            && rawJournalStorage != null
            && rawJournalStorage instanceof JournalStorageDirect) {
          journalStorage = (JournalStorageDirect) rawJournalStorage;
        } else if (rawJournalStorage != null) {
          journalStorage = new JournalStorageDirectImpl(rawJournalStorage, mainThreadRunner);
        } else {
          journalStorage = new InMemoryJournalStorage();
        }
      }
      return journalStorage;
    }

    public FeedProcessScope build() {
      MainThreadRunner mainThreadRunner = new MainThreadRunner();
      contentStorage = buildContentStorage(mainThreadRunner);
      journalStorage = buildJournalStorage(mainThreadRunner);

      boolean directHostCallEnabled =
          configuration.getValueOrDefault(ConfigKey.USE_DIRECT_STORAGE, false);
      NetworkClient networkClient;
      SchedulerApi schedulerApi;
      if (unwrappedNetworkClient instanceof DirectHostSupported && directHostCallEnabled) {
        networkClient = unwrappedNetworkClient;
      } else {
        networkClient =
            new NetworkClientWrapper(unwrappedNetworkClient, threadUtils, mainThreadRunner);
      }
      if (unwrappedSchedulerApi instanceof DirectHostSupported && directHostCallEnabled) {
        schedulerApi = unwrappedSchedulerApi;
      } else {
        schedulerApi =
            new SchedulerApiWrapper(unwrappedSchedulerApi, threadUtils, mainThreadRunner);
      }

      // Build default component instances if necessary.
      if (protoExtensionProvider == null) {
        // Return an empty list of extensions by default.
        protoExtensionProvider = ArrayList::new;
      }
      FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(protoExtensionProvider);
      if (clock == null) {
        clock = new SystemClockImpl();
      }
      TimingUtils timingUtils = new TimingUtils();
      TaskQueue taskQueue =
          new TaskQueue(basicLoggingApi, singleThreadExecutor, mainThreadRunner, clock);
      FeedStore store =
          new FeedStore(
              timingUtils,
              extensionRegistry,
              contentStorage,
              journalStorage,
              threadUtils,
              taskQueue,
              clock,
              basicLoggingApi,
              mainThreadRunner);

      FeedAppLifecycleListener lifecycleListener = new FeedAppLifecycleListener(threadUtils);
      lifecycleListener.registerObserver(store);

      ProtocolAdapter protocolAdapter = new FeedProtocolAdapter(timingUtils);
      ActionReader actionReader =
          new FeedActionReader(store, clock, protocolAdapter, taskQueue, configuration);
      FeedRequestManager feedRequestManager =
          new FeedRequestManagerImpl(
              configuration,
              networkClient,
              protocolAdapter,
              extensionRegistry,
              schedulerApi,
              taskQueue,
              timingUtils,
              threadUtils,
              actionReader,
              context,
              applicationInfo,
              mainThreadRunner,
              basicLoggingApi,
              tooltipSupportedApi);
      ActionUploadRequestManager actionUploadRequestManager =
          new FeedActionUploadRequestManager(
              configuration,
              networkClient,
              protocolAdapter,
              extensionRegistry,
              taskQueue,
              threadUtils,
              actionReader,
              store,
              clock);
      FeedSessionManagerFactory fsmFactory =
          new FeedSessionManagerFactory(
              taskQueue,
              store,
              timingUtils,
              threadUtils,
              protocolAdapter,
              feedRequestManager,
              actionUploadRequestManager,
              schedulerApi,
              configuration,
              clock,
              lifecycleListener);
      FeedSessionManager feedSessionManager = fsmFactory.create();
      RequestManagerImpl clientRequestManager =
          new RequestManagerImpl(feedRequestManager, feedSessionManager);
      ActionManager actionManager =
          new FeedActionManagerImpl(
              feedSessionManager, store, threadUtils, taskQueue, mainThreadRunner, clock);

      FeedKnownContent feedKnownContent =
          new FeedKnownContentImpl(feedSessionManager, mainThreadRunner, threadUtils);

      ClearAllListener clearAllListener =
          new ClearAllListener(
              taskQueue, feedSessionManager, store, threadUtils, lifecycleListener);
      return new FeedProcessScope(
          basicLoggingApi,
          networkClient,
          Validators.checkNotNull(protocolAdapter),
          Validators.checkNotNull(clientRequestManager),
          Validators.checkNotNull(feedSessionManager),
          store,
          timingUtils,
          threadUtils,
          taskQueue,
          mainThreadRunner,
          lifecycleListener,
          clock,
          debugBehavior,
          actionManager,
          configuration,
          feedKnownContent,
          extensionRegistry,
          clearAllListener,
          tooltipSupportedApi,
          applicationInfo);
    }
  }
}
