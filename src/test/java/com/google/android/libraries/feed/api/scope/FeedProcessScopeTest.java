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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ConfigManager;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.scope.FeedProcessScope.Builder;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests for {@link FeedProcessScope}. */
@RunWith(RobolectricTestRunner.class)
public class FeedProcessScopeTest {
  // Mocks for required fields
  @Mock private ConfigManager configManager;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private LoggingApi loggingApi;
  @Mock private NetworkClient networkClient;
  @Mock private SchedulerApi schedulerApi;

  // Mocks for optional fields
  @Mock private FeedProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;
  @Mock private SessionManager sessionManager;

  private final ProtoExtensionProvider protoExtensionProvider =
      new ProtoExtensionProvider() {
        @Override
        public List<GeneratedExtension<?, ?>> getProtoExtensions() {
          return new ArrayList<>();
        }
      };

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testBasicBuild() {
    // No crash should happen.
    FeedProcessScope processScope =
        new Builder(configManager, imageLoaderApi, loggingApi, networkClient, schedulerApi).build();

    assertThat(processScope.getProtocolAdapter()).isNotNull();
    assertThat(processScope.getRequestManager()).isNotNull();
    assertThat(processScope.getSessionManager()).isNotNull();
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
  }

  @Test
  public void testComplexBuild() {
    // No crash should happen.
    FeedProcessScope processScope =
        new FeedProcessScope.Builder(
                configManager, imageLoaderApi, loggingApi, networkClient, schedulerApi)
            .setProtocolAdapter(protocolAdapter)
            .setRequestManager(requestManager)
            .setSessionManager(sessionManager)
            .setProtoExtensionProvider(protoExtensionProvider)
            .build();

    assertThat(processScope.getProtocolAdapter()).isEqualTo(protocolAdapter);
    assertThat(processScope.getRequestManager()).isEqualTo(requestManager);
    assertThat(processScope.getSessionManager()).isEqualTo(sessionManager);
    assertThat(processScope.getAppLifecycleListener()).isNotNull();
  }
}
