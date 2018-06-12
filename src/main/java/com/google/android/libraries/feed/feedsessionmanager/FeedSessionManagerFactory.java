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

package com.google.android.libraries.feed.feedsessionmanager;

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.ContentCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.MutationCommitterFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionCache;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.TaskQueue;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Factory which creates the {@link FeedSessionManager}. This creates all the support classes before
 * creating the FeedSessionManager instead of creating the support objects inside the class
 * constructor.
 */
public class FeedSessionManagerFactory {
  private static final long DEFAULT_LIFETIME_MS = TimeUnit.HOURS.toMillis(1);

  private final ExecutorService executor;
  private final Store store;
  private final TimingUtils timingUtils;
  private final ThreadUtils threadUtils;
  private final MainThreadRunner mainThreadRunner;
  private final ProtocolAdapter protocolAdapter;
  private final RequestManager requestManager;
  private final SchedulerApi schedulerApi;
  private final Configuration configuration;
  private final Clock clock;

  public FeedSessionManagerFactory(
      ExecutorService executor,
      Store store,
      TimingUtils timingUtils,
      ThreadUtils threadUtils,
      MainThreadRunner mainThreadRunner,
      ProtocolAdapter protocolAdapter,
      RequestManager requestManager,
      SchedulerApi schedulerApi,
      Configuration configuration,
      Clock clock) {
    this.executor = executor;
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
    this.mainThreadRunner = mainThreadRunner;
    this.protocolAdapter = protocolAdapter;
    this.requestManager = requestManager;
    this.schedulerApi = schedulerApi;
    this.configuration = configuration;
    this.clock = clock;
  }

  /** Creates a new FeedSessionManager and initializes it */
  public FeedSessionManager create() {
    FeedSessionManager feedSessionManager = createUninitialized();
    feedSessionManager.initialize();
    return feedSessionManager;
  }

  /** Creates an uninitialized FeedSessionManager. */
  @VisibleForTesting
  FeedSessionManager createUninitialized() {
    TaskQueue taskQueue = new TaskQueue(executor);
    long lifetimeMs =
        configuration.getValueOrDefault(ConfigKey.SESSION_LIFETIME_MS, DEFAULT_LIFETIME_MS);
    SessionFactory sessionFactory =
        new SessionFactory(store, taskQueue, timingUtils, threadUtils, configuration);
    SessionCache sessionCache =
        new SessionCache(
            store, taskQueue, sessionFactory, lifetimeMs, timingUtils, threadUtils, clock);
    ContentCache contentCache = new ContentCache();
    MutationCommitterFactory mutationCommitterFactory =
        new MutationCommitterFactory(
            store,
            sessionCache,
            contentCache,
            taskQueue,
            mainThreadRunner,
            schedulerApi,
            threadUtils,
            timingUtils,
            clock);

    return new FeedSessionManager(
        taskQueue,
        sessionFactory,
        sessionCache,
        mutationCommitterFactory,
        contentCache,
        store,
        timingUtils,
        threadUtils,
        mainThreadRunner,
        protocolAdapter,
        requestManager,
        schedulerApi,
        configuration,
        clock);
  }
}
