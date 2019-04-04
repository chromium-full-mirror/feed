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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.scope.FeedProcessScope.Builder;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.host.config.ApplicationInfo;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.proto.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.android.libraries.feed.host.storage.JournalStorageDirect;
import com.google.android.libraries.feed.host.stream.TooltipSupportedApi;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FeedProcessScope}. */
@RunWith(RobolectricTestRunner.class)
public class FeedProcessScopeTest {
  // Mocks for required fields
  @Mock private LoggingApi loggingApi;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private NetworkClient networkClient;
  @Mock private SchedulerApi schedulerApi;
  @Mock private ApplicationInfo applicationInfo;
  @Mock private TooltipSupportedApi tooltipSupportedApi;

  // Mocks for optional fields
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;
  @Mock private SessionManager sessionManager;
  @Mock private ThreadUtils threadUtils;

  private final ProtoExtensionProvider protoExtensionProvider = ArrayList::new;
  private Configuration configuration = new Configuration.Builder().build();
  private Context context;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
  }

  @Test
  public void testBasicBuild() {
    // No crash should happen.
    FeedProcessScope processScope =
        new Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setThreadUtils(threadUtils)
            .build();

    assertThat(processScope.getProtocolAdapter()).isNotNull();
    assertThat(processScope.getRequestManager()).isNotNull();
    assertThat(processScope.getSessionManager()).isNotNull();
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
    assertThat(processScope.getActionManager()).isNotNull();
    assertThat(processScope.getClock()).isNotNull();
    assertThat(processScope.getTimingUtils()).isNotNull();
    assertThat(processScope.getMainThreadRunner()).isNotNull();
    assertThat(processScope.getTaskQueue()).isNotNull();
    assertThat(processScope.getStore()).isNotNull();
    assertThat(processScope.getKnownContentApi()).isNotNull();
    assertThat(processScope.getFeedExtensionRegistry()).isNotNull();
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
  }

  @Test
  public void testComplexBuild() {
    FakeClock fakeClock = new FakeClock();
    // No crash should happen.
    FeedProcessScope processScope =
        new FeedProcessScope.Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setProtocolAdapter(protocolAdapter)
            .setRequestManager(requestManager)
            .setSessionManager(sessionManager)
            .setProtoExtensionProvider(protoExtensionProvider)
            .setClock(fakeClock)
            .build();

    assertThat(processScope.getProtocolAdapter()).isEqualTo(protocolAdapter);
    assertThat(processScope.getRequestManager()).isEqualTo(requestManager);
    assertThat(processScope.getSessionManager()).isEqualTo(sessionManager);
    assertThat(processScope.getClock()).isEqualTo(fakeClock);
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
    assertThat(processScope.getActionManager()).isNotNull();
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
  }

  @Test
  public void testDirectStorage() {
    ContentStorageDirect contentStorageDirect = mock(ContentStorageDirect.class);
    JournalStorageDirect journalStorageDirect = mock(JournalStorageDirect.class);
    FeedProcessScope.Builder builder =
        new FeedProcessScope.Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setContentStorageDirect(contentStorageDirect)
            .setJournalStorageDirect(journalStorageDirect);
    assertThat(builder.contentStorage).isEqualTo(contentStorageDirect);
    assertThat(builder.journalStorage).isEqualTo(journalStorageDirect);
  }

  @Test
  public void testStorage_direct() {
    configuration = new Configuration.Builder().put(ConfigKey.USE_DIRECT_STORAGE, true).build();
    ContentStorage contentStorageDirect =
        mock(ContentStorage.class, withSettings().extraInterfaces(ContentStorageDirect.class));
    JournalStorage journalStorageDirect =
        mock(JournalStorage.class, withSettings().extraInterfaces(JournalStorageDirect.class));
    FeedProcessScope.Builder builder =
        new FeedProcessScope.Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setContentStorage(contentStorageDirect)
            .setJournalStorage(journalStorageDirect);
    assertThat(builder.buildContentStorage()).isEqualTo(contentStorageDirect);
    assertThat(builder.buildJournalStorage()).isEqualTo(journalStorageDirect);
  }

  @Test
  public void testStorage_wrapped() {
    configuration = new Configuration.Builder().put(ConfigKey.USE_DIRECT_STORAGE, false).build();
    ContentStorage contentStorageDirect =
        mock(ContentStorage.class, withSettings().extraInterfaces(ContentStorageDirect.class));
    JournalStorage journalStorageDirect =
        mock(JournalStorage.class, withSettings().extraInterfaces(JournalStorageDirect.class));
    FeedProcessScope.Builder builder =
        new FeedProcessScope.Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setContentStorage(contentStorageDirect)
            .setJournalStorage(journalStorageDirect);
    assertThat(builder.contentStorage).isNotEqualTo(contentStorageDirect);
    assertThat(builder.journalStorage).isNotEqualTo(journalStorageDirect);
  }

  @Test
  public void testDestroy() throws Exception {
    FeedProcessScope processScope =
        new Builder(
                configuration,
                MoreExecutors.newDirectExecutorService(),
                loggingApi,
                basicLoggingApi,
                networkClient,
                schedulerApi,
                DebugBehavior.VERBOSE,
                context,
                applicationInfo,
                tooltipSupportedApi)
            .setThreadUtils(threadUtils)
            .build();
    processScope.onDestroy();
    verify(networkClient).close();
  }
}
