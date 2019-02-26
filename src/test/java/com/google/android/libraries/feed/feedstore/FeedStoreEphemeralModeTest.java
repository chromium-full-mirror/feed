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

package com.google.android.libraries.feed.feedstore;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedstore.testing.AbstractFeedStoreTest;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FeedStore} class when running in ephemeral mode */
@RunWith(RobolectricTestRunner.class)
public class FeedStoreEphemeralModeTest extends AbstractFeedStoreTest {

  @Mock private ThreadUtils threadUtils;
  @Mock private Configuration configuration;
  @Mock private BasicLoggingApi basicLoggingApi;

  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorageDirect contentStorage = new InMemoryContentStorage();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    when(configuration.getValueOrDefault(ConfigKey.USE_DIRECT_STORAGE, false)).thenReturn(false);
  }

  @Override
  protected Store getStore(MainThreadRunner mainThreadRunner) {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    FeedStore feedStore =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            new InMemoryJournalStorage(),
            threadUtils,
            taskQueue,
            clock,
            basicLoggingApi,
            mainThreadRunner);
    feedStore.switchToEphemeralMode();
    return feedStore;
  }
}
