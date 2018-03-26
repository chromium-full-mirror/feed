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

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.Clock;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.scope.FeedStreamScope.Builder;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.common.testing.TestClockImpl;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests for {@link FeedStreamScope}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class FeedStreamScopeTest {

  // Mocks for required fields
  @Mock private ActionApi actionApi;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private LoggingApi loggingApi;

  // Mocks for optional fields
  @Mock private FeedProtocolAdapter protocolAdapter;
  @Mock private SessionManager sessionManager;
  @Mock private ActionParser actionParser;
  @Mock private Stream stream;
  @Mock private StreamConfiguration streamConfiguration;
  @Mock private CardConfiguration cardConfiguration;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private CustomElementProvider customElementProvider;

  private Context context;
  private UiRunnableHandler uiRunnableHandler;
  private ThreadUtils threadUtils;
  private TimingUtils timingUtils;
  private Clock clock;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    uiRunnableHandler = new UiRunnableHandlerImpl();
    threadUtils = new ThreadUtils();
    timingUtils = new TimingUtils();
    clock = new TestClockImpl();
  }

  @Test
  public void testBasicBuild() {
    FeedStreamScope streamScope =
        new Builder(
                context,
                actionApi,
                imageLoaderApi,
                loggingApi,
                protocolAdapter,
                sessionManager,
                threadUtils,
                timingUtils,
                uiRunnableHandler,
                clock)
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
                loggingApi,
                protocolAdapter,
                sessionManager,
                threadUtils,
                timingUtils,
                uiRunnableHandler,
                clock)
            .setActionParser(actionParser)
            .setStreamConfiguration(streamConfiguration)
            .setCardConfiguration(cardConfiguration)
            .setStream(stream)
            .setModelProviderFactory(modelProviderFactory)
            .setCustomElementProvider(customElementProvider)
            .build();
    assertThat(streamScope.getStream()).isEqualTo(stream);
    assertThat(streamScope.getModelProviderFactory()).isEqualTo(modelProviderFactory);
  }
}
