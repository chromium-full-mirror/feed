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

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.view.View;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.stream.CardConfiguration;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter;
import com.google.android.libraries.feed.basicstream.internal.piet.PietSharedStateSupplier;
import com.google.android.libraries.feed.common.testing.TestClockImpl;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.action.ActionParser;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

/** Tests for {@link BasicStream}. */
@RunWith(RobolectricTestRunner.class)
public class BasicStreamTest {

  @Mock private CardConfiguration cardConfiguration;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private ActionParser actionParser;
  @Mock private ActionApi actionApi;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private PietSharedStateSupplier pietSharedStateSupplier;
  @Mock private StreamRecyclerViewAdapter streamRecyclerViewAdapter;
  @Mock private ModelProvider modelProvider;

  private final List<View> headers = new ArrayList<>();
  private final ThreadUtils threadUtils = new ThreadUtils();

  private BasicStream basicStream;

  @Before
  public void setUp() {
    initMocks(this);

    basicStream =
        new BasicStream(
            Robolectric.setupActivity(Activity.class),
            cardConfiguration,
            imageLoaderApi,
            actionParser,
            actionApi,
            customElementProvider,
            threadUtils,
            headers,
            new TestClockImpl()) {
          @Override
          PietSharedStateSupplier createPietSharedStateSupplier() {
            return pietSharedStateSupplier;
          }
        };
  }

  @Test
  public void testSetModelProvider() {
    basicStream.setModelProvider(modelProvider);

    verify(pietSharedStateSupplier).setModelProvider(modelProvider);
  }
}
