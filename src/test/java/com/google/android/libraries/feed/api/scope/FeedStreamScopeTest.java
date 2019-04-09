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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParserFactory;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.knowncontent.KnownContentApi;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.scope.FeedStreamScope.Builder;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.offlineindicator.OfflineIndicatorApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.host.stream.SnackbarApi;
import com.google.android.libraries.feed.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.host.stream.TooltipApi;
import com.google.android.libraries.feed.host.stream.TooltipSupportedApi;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FeedStreamScope}. */
@RunWith(RobolectricTestRunner.class)
public class FeedStreamScopeTest {

  // Mocks for required fields
  @Mock private ActionApi actionApi;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private TooltipSupportedApi tooltipSupportedApi;

  // Mocks for optional fields
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private SessionManager sessionManager;
  @Mock private ActionParserFactory actionParserFactory;
  @Mock private Stream stream;
  @Mock private StreamConfiguration streamConfiguration;
  @Mock private CardConfiguration cardConfiguration;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private ActionManager actionManager;
  @Mock private Configuration config;
  @Mock private SnackbarApi snackbarApi;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private OfflineIndicatorApi offlineIndicatorApi;
  @Mock private KnownContentApi knownContentApi;
  @Mock private TaskQueue taskQueue;
  @Mock private TooltipApi tooltipApi;

  private Context context;
  private MainThreadRunner mainThreadRunner;
  private ThreadUtils threadUtils;
  private TimingUtils timingUtils;
  private Clock clock;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    mainThreadRunner = new MainThreadRunner();
    threadUtils = new ThreadUtils();
    timingUtils = new TimingUtils();
    clock = new FakeClock();
    when(config.getValueOrDefault(eq(ConfigKey.LOGGING_IMMEDIATE_CONTENT_THRESHOLD_MS), anyLong()))
        .thenReturn(1000L);

    when(config.getValueOrDefault(eq(ConfigKey.FADE_IMAGE_THRESHOLD_MS), anyInt())).thenReturn(80);
  }

  @Test
  public void testBasicBuild() {
    FeedStreamScope streamScope =
        new Builder(
                context,
                actionApi,
                imageLoaderApi,
                protocolAdapter,
                sessionManager,
                threadUtils,
                timingUtils,
                taskQueue,
                mainThreadRunner,
                clock,
                DebugBehavior.VERBOSE,
                streamConfiguration,
                cardConfiguration,
                actionManager,
                config,
                snackbarApi,
                basicLoggingApi,
                offlineIndicatorApi,
                knownContentApi,
                tooltipApi,
                tooltipSupportedApi)
            .build();
    assertThat(streamScope.getStream()).isNotNull();
    assertThat(streamScope.getModelProviderFactory()).isNotNull();
  }

  @Test
  public void testComplexBuild() {
    FeedStreamScope streamScope =
        new Builder(
                context,
                actionApi,
                imageLoaderApi,
                protocolAdapter,
                sessionManager,
                threadUtils,
                timingUtils,
                taskQueue,
                mainThreadRunner,
                clock,
                DebugBehavior.VERBOSE,
                streamConfiguration,
                cardConfiguration,
                actionManager,
                config,
                snackbarApi,
                basicLoggingApi,
                offlineIndicatorApi,
                knownContentApi,
                tooltipApi,
                tooltipSupportedApi)
            .setActionParserFactory(actionParserFactory)
            .setStream(stream)
            .setModelProviderFactory(modelProviderFactory)
            .setCustomElementProvider(customElementProvider)
            .setHostBindingProvider(new HostBindingProvider())
            .build();
    assertThat(streamScope.getStream()).isEqualTo(stream);
    assertThat(streamScope.getModelProviderFactory()).isEqualTo(modelProviderFactory);
  }
}
