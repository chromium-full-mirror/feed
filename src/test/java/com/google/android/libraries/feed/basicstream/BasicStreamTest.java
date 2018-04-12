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

package com.google.android.libraries.feed.basicstream;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.os.Build.VERSION_CODES;
import android.view.View;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.basicstream.internal.piet.PietSharedStateSupplier;
import com.google.android.libraries.feed.common.testing.TestClockImpl;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link BasicStream}. */
@RunWith(RobolectricTestRunner.class)
public class BasicStreamTest {

  private static final int START_PADDING = 1;
  private static final int END_PADDING = 2;
  private static final int TOP_PADDING = 3;
  private static final int BOTTOM_PADDING = 4;

  @Mock private StreamConfiguration streamConfiguration;
  @Mock private CardConfiguration cardConfiguration;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private ActionParser actionParser;
  @Mock private ActionApi actionApi;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private PietSharedStateSupplier pietSharedStateSupplier;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private ModelProvider initialModelProvider;
  @Mock private ModelProvider modelProvider;

  private final List<View> headers = new ArrayList<>();
  private final ThreadUtils threadUtils = new ThreadUtils();

  private BasicStream basicStream;

  @Before
  public void setUp() {
    initMocks(this);

    when(streamConfiguration.getPaddingStart()).thenReturn(START_PADDING);
    when(streamConfiguration.getPaddingEnd()).thenReturn(END_PADDING);
    when(streamConfiguration.getPaddingTop()).thenReturn(TOP_PADDING);
    when(streamConfiguration.getPaddingBottom()).thenReturn(BOTTOM_PADDING);
    when(modelProviderFactory.createNew()).thenReturn(initialModelProvider, modelProvider);

    basicStream =
        new BasicStream(
            Robolectric.setupActivity(Activity.class),
            streamConfiguration,
            cardConfiguration,
            imageLoaderApi,
            actionParser,
            actionApi,
            customElementProvider,
            DebugBehavior.VERBOSE,
            threadUtils,
            headers,
            new TestClockImpl(),
            modelProviderFactory) {
          @Override
          PietSharedStateSupplier createPietSharedStateSupplier() {
            return pietSharedStateSupplier;
          }
        };
    basicStream.onCreate(null);
  }

  @Test
  public void testOnSessionStart() {
    basicStream.onSessionStart();

    verify(pietSharedStateSupplier).setModelProvider(initialModelProvider);
  }

  @Test
  public void testOnSessionFinished() {
    basicStream.onSessionFinished();

    verify(initialModelProvider).unregisterObserver(basicStream);
    verify(modelProviderFactory, times(2)).createNew();
    verify(modelProvider).registerObserver(basicStream);
  }

  @Test
  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  public void testPadding_jellyBean() {
    // Padding is setup in constructor.
    View view = basicStream.getView();

    assertThat(view.getPaddingLeft()).isEqualTo(START_PADDING);
    assertThat(view.getPaddingRight()).isEqualTo(END_PADDING);
    assertThat(view.getPaddingTop()).isEqualTo(TOP_PADDING);
    assertThat(view.getPaddingBottom()).isEqualTo(BOTTOM_PADDING);
  }

  @Test
  @Config(sdk = VERSION_CODES.KITKAT)
  public void testPadding_kitKat() {
    // Padding is setup in constructor.
    View view = basicStream.getView();

    assertThat(view.getPaddingStart()).isEqualTo(START_PADDING);
    assertThat(view.getPaddingEnd()).isEqualTo(END_PADDING);
    assertThat(view.getPaddingTop()).isEqualTo(TOP_PADDING);
    assertThat(view.getPaddingBottom()).isEqualTo(BOTTOM_PADDING);
  }
}
