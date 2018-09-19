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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParserFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.util.ContextMenuHelper;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.sharedstream.offlinemonitor.StreamOfflineMonitor;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelCursor;
import com.google.common.collect.Lists;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ClusterDriver}. */
@RunWith(RobolectricTestRunner.class)
public class ClusterDriverTest {

  private static final StreamFeature CARD_STREAM_FEATURE =
      StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build();

  @Mock private CardDriver cardDriver;
  @Mock private LeafFeatureDriver leafFeatureDriver;
  @Mock private ModelFeature clusterModelFeature;
  @Mock private ContentChangedListener contentChangedListener;

  private ClusterDriverForTest clusterDriver;

  @Before
  public void setup() {
    initMocks(this);

    // Child produced by Cursor representing a card.
    ModelChild cardChild = mock(ModelChild.class);
    when(cardChild.getType()).thenReturn(Type.FEATURE);

    // ModelFeature representing a card.
    ModelFeature cardModelFeature = mock(ModelFeature.class);
    when(cardModelFeature.getStreamFeature()).thenReturn(CARD_STREAM_FEATURE);
    when(cardChild.getModelFeature()).thenReturn(cardModelFeature);

    // Cursor representing the content of the cluster, which is one card.
    FakeModelCursor clusterCursor = new FakeModelCursor(Lists.newArrayList(cardChild));
    when(clusterModelFeature.getCursor()).thenReturn(clusterCursor);

    when(cardDriver.getLeafFeatureDriver()).thenReturn(leafFeatureDriver);

    Context context = Robolectric.setupActivity(Activity.class);
    clusterDriver =
        new ClusterDriverForTest(
            mock(ActionApi.class),
            mock(ActionManager.class),
            mock(ActionParserFactory.class),
            mock(BasicLoggingApi.class),
            context,
            clusterModelFeature,
            mock(ModelProvider.class),
            /* position= */ 0,
            cardDriver,
            mock(StreamOfflineMonitor.class),
            mock(ContextMenuHelper.class));
  }

  @Test
  public void testGetContentModel() {
    assertThat(clusterDriver.getLeafFeatureDriver()).isEqualTo(leafFeatureDriver);
  }

  @Test
  public void testGetContentModel_reusesPreviousContentModel() {
    clusterDriver.getLeafFeatureDriver();

    verify(clusterModelFeature).getCursor();

    clusterDriver.getLeafFeatureDriver();

    verifyNoMoreInteractions(clusterModelFeature);
  }

  @Test
  public void testOnDestroy() {
    clusterDriver.getLeafFeatureDriver();
    clusterDriver.onDestroy();

    verify(cardDriver).onDestroy();
  }

  class ClusterDriverForTest extends ClusterDriver {

    private final CardDriver cardDriver;

    ClusterDriverForTest(
        ActionApi actionApi,
        ActionManager actionManager,
        ActionParserFactory actionParserFactory,
        BasicLoggingApi basicLoggingApi,
        Context context,
        ModelFeature clusterModel,
        ModelProvider modelProvider,
        int position,
        CardDriver cardDriver,
        StreamOfflineMonitor streamOfflineMonitor,
        ContextMenuHelper contextMenuHelper) {
      super(
          actionApi,
          actionManager,
          actionParserFactory,
          basicLoggingApi,
          context,
          clusterModel,
          modelProvider,
          position,
          streamOfflineMonitor,
          contentChangedListener,
          contextMenuHelper);
      this.cardDriver = cardDriver;
    }

    @Override
    CardDriver createCardDriver(ModelFeature content) {
      return cardDriver;
    }
  }
}
