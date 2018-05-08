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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.api.modelprovider.TokenCompletedObserver;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver.StreamContentListener;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelChild;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelCursor;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelFeature;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelToken;
import com.google.common.collect.Lists;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Cluster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamDriver}. */
@RunWith(RobolectricTestRunner.class)
public class StreamDriverTest {

  private StreamDriver streamDriver;

  @Mock private ModelFeature streamFeature;
  @Mock private ContentModel continuationContentModel;
  @Mock private ContentModel clusterContentModel;
  @Mock private ContentModel cardContentModel;
  @Mock private ModelProvider modelProvider;
  @Mock private ContinuationDriver continuationDriver;
  @Mock private ClusterDriver clusterDriver;
  @Mock private CardDriver cardDriver;
  @Mock private StreamContentListener contentListener;

  @Before
  public void setup() {
    initMocks(this);
    ThreadUtils threadUtils = new ThreadUtils();

    when(continuationDriver.getContentModel()).thenReturn(continuationContentModel);
    when(clusterDriver.getContentModel()).thenReturn(clusterContentModel);
    when(cardDriver.getContentModel()).thenReturn(cardContentModel);
    when(modelProvider.getRootFeature()).thenReturn(streamFeature);

    streamDriver = new StreamDriverForTest(modelProvider, threadUtils);

    streamDriver.setStreamContentListener(contentListener);
  }

  @Test
  public void testContinuationToken_createsContinuationContentModel() {
    when(streamFeature.getCursor()).thenReturn(new FakeCursorBuilder().addToken().build());

    List<ContentModel> contentModels = streamDriver.getContentModels();
    assertThat(contentModels).hasSize(1);
    assertThat(contentModels.get(0)).isEqualTo(continuationContentModel);
  }

  @Test
  public void testContinuationToken_tokenHandling() {
    List<ContentModel> finalContentModels =
        new TokenHandlingTestBuilder()
            .setInitialCursor(new FakeCursorBuilder().addToken().build())
            .setTokenPayloadCursor(new FakeCursorBuilder().addCluster().build())
            .setTokenIndex(0)
            .run();

    assertThat(finalContentModels).hasSize(1);
    assertThat(finalContentModels.get(0)).isEqualTo(clusterContentModel);

    // If the above two assertions pass, this is also guaranteed to pass. This is just to explicitly
    // check that the continuationContentModel has been removed.
    assertThat(finalContentModels).doesNotContain(continuationContentModel);
  }

  @Test
  public void testContinuationToken_tokenHandling_notifiesObservers() {
    new TokenHandlingTestBuilder()
        .setInitialCursor(new FakeCursorBuilder().addToken().build())
        .setTokenPayloadCursor(new FakeCursorBuilder().addCluster().build())
        .setTokenIndex(0)
        .run();

    verify(contentListener).notifyContentRemoved(0);
    verify(contentListener).notifyContentsAdded(0, Lists.newArrayList(clusterContentModel));
  }

  @Test
  public void testContinuationToken_tokenChildrenAddedAtTokenPosition() {
    List<ContentModel> finalContentModels =
        new TokenHandlingTestBuilder()
            .setInitialCursor(new FakeCursorBuilder().addCluster().addToken().build())
            .setTokenPayloadCursor(new FakeCursorBuilder().addCluster().addToken().build())
            .setTokenIndex(1)
            .run();

    assertThat(finalContentModels).hasSize(3);
    assertThat(finalContentModels)
        .containsExactly(clusterContentModel, clusterContentModel, continuationContentModel);
  }

  @Test
  public void testContinuationToken_tokenChildrenAddedAtTokenPosition_tokenNotAtEnd() {
    List<ContentModel> finalContentModels =
        new TokenHandlingTestBuilder()
            .setInitialCursor(new FakeCursorBuilder().addCluster().addToken().addCluster().build())
            .setTokenIndex(1)
            .setTokenPayloadCursor(
                new FakeCursorBuilder().addCluster().addCard().addCluster().build())
            .run();

    assertThat(finalContentModels).hasSize(5);
    assertThat(finalContentModels)
        .containsExactly(
            clusterContentModel,
            clusterContentModel,
            cardContentModel,
            clusterContentModel,
            clusterContentModel);
  }

  private class StreamDriverForTest extends StreamDriver {

    StreamDriverForTest(ModelProvider modelProvider, ThreadUtils threadUtils) {
      super(modelProvider, threadUtils);
    }

    @Override
    ContinuationDriver createContinuationDriver(
        ModelProvider modelProvider, ModelToken modelToken) {
      return continuationDriver;
    }

    @Override
    ClusterDriver createClusterDriver(ModelFeature modelFeature) {
      return clusterDriver;
    }

    @Override
    CardDriver createCardDriver(ModelFeature modelFeature) {
      return cardDriver;
    }
  }

  private static class FakeCursorBuilder {

    List<ModelChild> cursorChildren = new ArrayList<>();

    FakeCursorBuilder addCard() {
      ModelChild cardChild =
          new FakeModelChild.Builder()
              .setModelFeature(
                  new FakeModelFeature.Builder()
                      .setStreamFeature(
                          StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build())
                      .build())
              .build();

      cursorChildren.add(cardChild);

      return this;
    }

    FakeCursorBuilder addCluster() {
      ModelChild clusterChild =
          new FakeModelChild.Builder()
              .setModelFeature(
                  new FakeModelFeature.Builder()
                      .setStreamFeature(
                          StreamFeature.newBuilder()
                              .setCluster(Cluster.getDefaultInstance())
                              .build())
                      .build())
              .build();

      cursorChildren.add(clusterChild);

      return this;
    }

    FakeCursorBuilder addToken() {
      ModelChild tokenChild =
          new FakeModelChild.Builder().setModelToken(new FakeModelToken.Builder().build()).build();

      cursorChildren.add(tokenChild);

      return this;
    }

    public FakeModelCursor build() {
      return new FakeModelCursor(cursorChildren);
    }
  }

  private class TokenHandlingTestBuilder {

    private FakeModelCursor initialCursor;
    private FakeModelCursor payloadCursor;
    private int tokenIndex;

    TokenHandlingTestBuilder setInitialCursor(FakeModelCursor initialCursor) {
      this.initialCursor = initialCursor;
      return this;
    }

    TokenHandlingTestBuilder setTokenPayloadCursor(FakeModelCursor initialCursor) {
      this.payloadCursor = initialCursor;
      return this;
    }

    TokenHandlingTestBuilder setTokenIndex(int tokenIndex) {
      this.tokenIndex = tokenIndex;
      return this;
    }

    List<ContentModel> run() {
      when(streamFeature.getCursor()).thenReturn(initialCursor);

      // Causes StreamDriver to build a list of children based on the children from the cursor.
      streamDriver.getContentModels();

      FakeModelToken token = (FakeModelToken) initialCursor.getChildAt(tokenIndex).getModelToken();

      HashSet<TokenCompletedObserver> tokenCompletedObservers = token.getObservers();

      assertThat(tokenCompletedObservers).hasSize(1);

      for (TokenCompletedObserver tokenCompletedObserver : tokenCompletedObservers) {
        tokenCompletedObserver.onTokenCompleted(new TokenCompleted(payloadCursor));
      }

      return streamDriver.getContentModels();
    }
  }
}
