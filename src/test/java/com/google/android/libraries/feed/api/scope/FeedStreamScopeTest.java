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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.internal.knowncontent.FeedKnownContent;
import com.google.android.libraries.feed.api.scope.FeedStreamScope.Builder;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.ApplicationInfo;
import com.google.android.libraries.feed.host.config.ApplicationInfo.BuildType;
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
import com.google.android.libraries.feed.internalapi.actionmanager.ActionManager;
import com.google.android.libraries.feed.internalapi.common.ThreadUtils;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.internalapi.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.internalapi.stream.StreamFactory;
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

  @BuildType private static final int BUILD_TYPE = BuildType.ALPHA;
  public static final DebugBehavior DEBUG_BEHAVIOR = DebugBehavior.VERBOSE;

  // Mocks for required fields
  @Mock private ActionApi actionApi;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private TooltipSupportedApi tooltipSupportedApi;

  // Mocks for optional fields
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private SessionManager sessionManager;
  @Mock private Stream stream;
  @Mock private StreamConfiguration streamConfiguration;
  @Mock private CardConfiguration cardConfiguration;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private HostBindingProvider hostBindingProvider;
  @Mock private ActionManager actionManager;
  @Mock private Configuration config;
  @Mock private SnackbarApi snackbarApi;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private OfflineIndicatorApi offlineIndicatorApi;
  @Mock private FeedKnownContent feedKnownContent;
  @Mock private TaskQueue taskQueue;
  @Mock private TooltipApi tooltipApi;
  @Mock private FeedExtensionRegistry feedExtensionRegistry;

  private Context context;
  private MainThreadRunner mainThreadRunner;
  private ThreadUtils threadUtils;
  private TimingUtils timingUtils;
  private Clock clock;
  private ApplicationInfo applicationInfo;
  private StreamFactory streamFactory;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    mainThreadRunner = new MainThreadRunner();
    threadUtils = new ThreadUtils();
    timingUtils = new TimingUtils();
    clock = new FakeClock();
    applicationInfo = new ApplicationInfo.Builder(context).setBuildType(BUILD_TYPE).build();
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
                DEBUG_BEHAVIOR,
                streamConfiguration,
                cardConfiguration,
                actionManager,
                config,
                snackbarApi,
                basicLoggingApi,
                offlineIndicatorApi,
                feedKnownContent,
                tooltipApi,
                tooltipSupportedApi,
                applicationInfo,
                feedExtensionRegistry)
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
                DEBUG_BEHAVIOR,
                streamConfiguration,
                cardConfiguration,
                actionManager,
                config,
                snackbarApi,
                basicLoggingApi,
                offlineIndicatorApi,
                feedKnownContent,
                tooltipApi,
                tooltipSupportedApi,
                applicationInfo,
                feedExtensionRegistry)
            .setModelProviderFactory(modelProviderFactory)
            .setCustomElementProvider(customElementProvider)
            .setHostBindingProvider(new HostBindingProvider())
            .build();
    assertThat(streamScope.getStream()).isNotNull();
    assertThat(streamScope.getModelProviderFactory()).isEqualTo(modelProviderFactory);
  }

  @Test
  public void testStreamFactoryBuild() {
    setupStreamFactory(stream);

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
                DEBUG_BEHAVIOR,
                streamConfiguration,
                cardConfiguration,
                actionManager,
                config,
                snackbarApi,
                basicLoggingApi,
                offlineIndicatorApi,
                feedKnownContent,
                tooltipApi,
                tooltipSupportedApi,
                applicationInfo,
                feedExtensionRegistry)
            .setStreamFactory(streamFactory)
            .setCustomElementProvider(customElementProvider)
            .setHostBindingProvider(hostBindingProvider)
            .build();
    assertThat(streamScope.getStream()).isEqualTo(stream);
  }

  private void setupStreamFactory(Stream streamToReturn) {
    streamFactory =
        (actionParserFactory,
            context,
            buildType,
            cardConfiguration,
            imageLoaderApi,
            customElementProvider,
            debugBehavior,
            clock,
            modelProviderFactory,
            hostBindingProvider,
            offlineIndicatorApi,
            configuration,
            actionApi,
            actionManager,
            snackbarApi,
            streamConfiguration,
            feedExtensionRegistry,
            basicLoggingApi,
            mainThreadRunner,
            isBackgroundDark,
            tooltipApi,
            threadUtils,
            knownContentApi) -> streamToReturn;
  }
}
