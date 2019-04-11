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

package com.google.android.libraries.feed.common.testing;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.api.scope.ClearAllListener;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.testing.FakeThreadUtils;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManagerFactory;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.proto.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.host.storage.JournalStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.internalapi.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.testing.host.logging.FakeBasicLoggingApi;
import com.google.android.libraries.feed.testing.host.scheduler.FakeSchedulerApi;
import com.google.android.libraries.feed.testing.requestmanager.FakeActionUploadRequestManager;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is a Scope type object which is used in the Infrastructure Integration tests. It sets the
 * Feed objects from ProtocolAdapter through the SessionManager.
 */
public class InfraIntegrationScope {
  private static final boolean USE_TIMEOUT_SCHEDULER = true;
  private static final long TIMEOUT_TIMEOUT_MS = 2;

  /**
   * For the TimeoutSession tests, this is how long we allow it to run before declaring a timeout
   * error.
   */
  public static final long TIMEOUT_TEST_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

  private final Configuration configuration;
  private final ContentStorageDirect contentStorage;
  private final ExecutorService executorService;
  private final FakeClock fakeClock;
  private final FakeMainThreadRunner fakeMainThreadRunner;
  private final FakeRequestManager fakeRequestManager;
  private final FeedAppLifecycleListener appLifecycleListener;
  private final FeedModelProviderFactory modelProviderFactory;
  private final FeedProtocolAdapter feedProtocolAdapter;
  private final FeedSessionManager feedSessionManager;
  private final FeedStore store;
  private final JournalStorageDirect journalStorage;
  private final SchedulerApi schedulerApi;
  private final TaskQueue taskQueue;
  private final ThreadUtils threadUtils;

  private InfraIntegrationScope(
      ThreadUtils threadUtils,
      ExecutorService executorService,
      SchedulerApi schedulerApi,
      FakeClock fakeClock,
      Configuration configuration,
      ContentStorageDirect contentStorage,
      JournalStorageDirect journalStorage,
      FakeMainThreadRunner fakeMainThreadRunner) {
    this.fakeClock = fakeClock;
    this.configuration = configuration;
    this.contentStorage = contentStorage;
    this.executorService = executorService;
    this.journalStorage = journalStorage;
    this.fakeMainThreadRunner = fakeMainThreadRunner;
    this.schedulerApi = schedulerApi;
    this.threadUtils = threadUtils;
    TimingUtils timingUtils = new TimingUtils();
    appLifecycleListener = new FeedAppLifecycleListener(threadUtils);
    FakeBasicLoggingApi fakeBasicLoggingApi = new FakeBasicLoggingApi();

    FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(new ExtensionProvider());
    taskQueue =
        new TaskQueue(
            fakeBasicLoggingApi,
            executorService,
            fakeMainThreadRunner,
            fakeClock,
            /* checkStarvation= */ true);
    store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorage,
            threadUtils,
            taskQueue,
            fakeClock,
            fakeBasicLoggingApi,
            fakeMainThreadRunner);
    feedProtocolAdapter = new FeedProtocolAdapter(timingUtils);
    fakeRequestManager =
        new FakeRequestManager(
            FakeThreadUtils.withoutThreadChecks(),
            fakeMainThreadRunner,
            feedProtocolAdapter,
            taskQueue);
    FakeActionUploadRequestManager fakeActionUploadRequestManager =
        new FakeActionUploadRequestManager(FakeThreadUtils.withThreadChecks());
    feedSessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                feedProtocolAdapter,
                fakeRequestManager,
                fakeActionUploadRequestManager,
                schedulerApi,
                configuration,
                fakeClock,
                appLifecycleListener)
            .create();
    new ClearAllListener(taskQueue, feedSessionManager, store, threadUtils, appLifecycleListener);
    feedSessionManager.initialize();
    modelProviderFactory =
        new FeedModelProviderFactory(
            feedSessionManager,
            threadUtils,
            timingUtils,
            taskQueue,
            fakeMainThreadRunner,
            configuration);
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

  public FakeClock getFakeClock() {
    return fakeClock;
  }

  public FeedStore getStore() {
    return store;
  }

  public TaskQueue getTaskQueue() {
    return taskQueue;
  }

  public FakeRequestManager getRequestManager() {
    return fakeRequestManager;
  }

  public AppLifecycleListener getAppLifecycleListener() {
    return appLifecycleListener;
  }

  @Override
  public InfraIntegrationScope clone() {
    return new InfraIntegrationScope(
        threadUtils,
        executorService,
        schedulerApi,
        fakeClock,
        configuration,
        contentStorage,
        journalStorage,
        fakeMainThreadRunner);
  }

  private static class ExtensionProvider implements ProtoExtensionProvider {
    @Override
    public List<GeneratedExtension<?, ?>> getProtoExtensions() {
      return new ArrayList<>();
    }
  }

  public static Configuration getTimeoutSchedulerConfig() {
    return new Configuration.Builder()
        .put(ConfigKey.USE_TIMEOUT_SCHEDULER, USE_TIMEOUT_SCHEDULER)
        .put(ConfigKey.TIMEOUT_TIMEOUT_MS, TIMEOUT_TIMEOUT_MS)
        .put(ConfigKey.USE_DIRECT_STORAGE, Boolean.FALSE)
        .build();
  }

  /** Builder for creating the {@link InfraIntegrationScope} */
  public static class Builder {
    private final FakeClock fakeClock = new FakeClock();
    private final FakeMainThreadRunner fakeMainThreadRunner =
        FakeMainThreadRunner.create(fakeClock);
    private final ThreadUtils threadUtils = FakeThreadUtils.withoutThreadChecks();

    private Configuration configuration = Configuration.getDefaultInstance();
    private SchedulerApi schedulerApi = new FakeSchedulerApi(threadUtils);

    public Builder() {}

    public Builder setConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder setSchedulerApi(SchedulerApi schedulerApi) {
      this.schedulerApi = schedulerApi;
      return this;
    }

    public InfraIntegrationScope build() {
      return new InfraIntegrationScope(
          threadUtils,
          MoreExecutors.newDirectExecutorService(),
          schedulerApi,
          fakeClock,
          configuration,
          new InMemoryContentStorage(),
          new InMemoryJournalStorage(),
          fakeMainThreadRunner);
    }
  }
}
