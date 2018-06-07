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

import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedstore.testing.AbstractFeedStoreTest;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FeedStore} class when running in ephemeral mode */
@RunWith(RobolectricTestRunner.class)
public class FeedStoreEphemeralModeTest extends AbstractFeedStoreTest {

  @Mock private ThreadUtils threadUtils;

  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorage contentStorage = new InMemoryContentStorage();
  private final MainThreadRunner mainThreadRunner = new MainThreadRunner();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Override
  protected Store getStore() {
    FeedStore feedStore =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            new InMemoryJournalStorage(threadUtils),
            threadUtils,
            mainThreadRunner,
            clock,
            new FeedStoreHelper());
    feedStore.switchToEphemeralMode();
    return feedStore;
  }
}
