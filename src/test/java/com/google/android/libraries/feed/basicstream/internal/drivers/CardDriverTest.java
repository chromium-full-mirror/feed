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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelCursor;
import com.google.common.collect.Lists;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.action.FeedActionPayloadProto.FeedActionPayload;
import com.google.search.now.ui.action.FeedActionProto.DismissData;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamSwipeExtensionProto.SwipeActionExtension;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CardDriver}. */
@RunWith(RobolectricTestRunner.class)
public class CardDriverTest {

  private static final StreamFeature STREAM_FEATURE =
      StreamFeature.newBuilder().setContent(Content.getDefaultInstance()).build();

  private static final FeedActionPayload SWIPE_ACTION =
      FeedActionPayload.newBuilder()
          .setExtension(
              FeedAction.feedActionExtension,
              FeedAction.newBuilder()
                  .setMetadata(
                      FeedActionMetadata.newBuilder()
                          .setDismissData(DismissData.getDefaultInstance()))
                  .build())
          .build();

  private static final Card CARD_WITH_SWIPE_ACTION =
      Card.newBuilder()
          .setExtension(
              SwipeActionExtension.swipeActionExtension,
              SwipeActionExtension.newBuilder().setSwipeAction(SWIPE_ACTION).build())
          .build();

  @Mock private ContentDriver contentDriverChild;
  @Mock private ModelFeature cardModelFeature;
  @Mock private ModelProvider modelProvider;
  @Mock private LeafFeatureDriver leafFeatureDriver;

  private CardDriverForTest cardDriver;

  @Before
  public void setup() {
    initMocks(this);

    // Represents payload of the child of the card.
    ModelFeature childModelFeature = mock(ModelFeature.class);
    when(childModelFeature.getStreamFeature()).thenReturn(STREAM_FEATURE);

    // ModelChild containing content.
    ModelChild pietModelChild = mock(ModelChild.class);
    when(pietModelChild.getModelFeature()).thenReturn(childModelFeature);

    // Cursor to transverse the content of a card.
    FakeModelCursor cardCursor = new FakeModelCursor(Lists.newArrayList(pietModelChild));

    // A ModelFeature representing a card.
    when(cardModelFeature.getCursor()).thenReturn(cardCursor);
    when(cardModelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build());
    when(contentDriverChild.getLeafFeatureDriver()).thenReturn(leafFeatureDriver);

    cardDriver = new CardDriverForTest(cardModelFeature, modelProvider, contentDriverChild);
  }

  @Test
  public void testGetLeafFeatureDriver() {
    assertThat((cardDriver.getLeafFeatureDriver())).isEqualTo(leafFeatureDriver);
  }

  @Test
  public void testGetLeafFeatureDriver_reusesPreviousLeafFeatureDriver() {
    cardDriver.getLeafFeatureDriver();

    verify(cardModelFeature).getCursor();

    reset(cardModelFeature);

    cardDriver.getLeafFeatureDriver();

    verify(cardModelFeature, never()).getCursor();
  }

  @Test
  public void testGetLeafFeatureDriver_notSwipeable() {
    when(cardModelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build());

    cardDriver.getLeafFeatureDriver();

    assertThat(cardDriver.swipeActionArg).isEqualTo(FeedActionPayload.getDefaultInstance());
  }

  @Test
  public void testGetLeafFeatureDriver_swipeable() {
    when(cardModelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setCard(CARD_WITH_SWIPE_ACTION).build());

    cardDriver.getLeafFeatureDriver();

    assertThat(cardDriver.swipeActionArg).isEqualTo(SWIPE_ACTION);
  }

  @Test
  public void testOnDestroy() {
    cardDriver.getLeafFeatureDriver();
    cardDriver.onDestroy();

    verify(contentDriverChild).onDestroy();
  }

  public class CardDriverForTest extends CardDriver {

    private final ContentDriver child;
    private FeedActionPayload swipeActionArg;

    CardDriverForTest(
        ModelFeature cardModel, ModelProvider modelProvider, ContentDriver childContentDriver) {
      super(cardModel, modelProvider);
      this.child = childContentDriver;
    }

    @Override
    ContentDriver createContentDriver(ModelFeature contentModel, FeedActionPayload swipeAction) {
      this.swipeActionArg = swipeAction;
      return child;
    }
  }
}
