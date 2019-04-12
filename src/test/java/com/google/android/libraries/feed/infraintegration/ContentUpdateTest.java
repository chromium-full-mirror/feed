// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.infraintegration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.concurrent.testing.FakeThreadUtils;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.ResponseBuilder;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.internalapi.modelprovider.FeatureChange;
import com.google.android.libraries.feed.internalapi.modelprovider.FeatureChangeObserver;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelChild;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelCursor;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelFeature;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProvider;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.internalapi.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.feed.client.StreamDataProto.UiContext;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

/** Tests which update content within an existing model. */
@RunWith(RobolectricTestRunner.class)
public class ContentUpdateTest {
  private final InfraIntegrationScope scope = new InfraIntegrationScope.Builder().build();
  private final FakeRequestManager requestManager = scope.getRequestManager();
  private final FakeThreadUtils fakeThreadUtils = scope.getFakeThreadUtils();
  private final ModelProviderFactory modelProviderFactory = scope.getModelProviderFactory();
  private final ModelProviderValidator modelValidator =
      new ModelProviderValidator(scope.getProtocolAdapter());
  private final ProtocolAdapter protocolAdapter = scope.getProtocolAdapter();
  private final SessionManager sessionManager = scope.getSessionManager();

  @Test
  public void updateContent_observers() {
    // Create a simple stream with a root and two features
    ContentId[] cards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1), ResponseBuilder.createFeatureContentId(2)
        };
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(cards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider =
        modelProviderFactory.createNew(null, UiContext.getDefaultInstance());

    ModelFeature root = modelProvider.getRootFeature();
    assertThat(root).isNotNull();
    ModelCursor cursor = root.getCursor();
    List<FeatureChangeObserver> observers = new ArrayList<>();
    List<FeatureChangeObserver> contentObservers = new ArrayList<>();
    for (ContentId contentId : cards) {
      ModelChild child = cursor.getNextItem();
      assertThat(child).isNotNull();
      modelValidator.assertStreamContentId(
          child.getContentId(), protocolAdapter.getStreamContentId(contentId));
      modelValidator.assertCardStructure(child);
      // register observer on the card
      ModelFeature feature = child.getModelFeature();
      FeatureChangeObserver observer = mock(FeatureChangeObserver.class);
      observers.add(observer);
      feature.registerObserver(observer);

      // register observer on the content of the card
      FeatureChangeObserver contentObserver = mock(FeatureChangeObserver.class);
      contentObservers.add(contentObserver);
      ModelCursor cardCursor = feature.getCursor();
      ModelChild cardCursorNextItem = cardCursor.getNextItem();
      assertThat(cardCursorNextItem).isNotNull();
      cardCursorNextItem.getModelFeature().registerObserver(contentObserver);
    }
    assertThat(cursor.isAtEnd()).isTrue();

    // Create an update response for the two content items
    requestManager.queueResponse(ResponseBuilder.builder().addCardsToRoot(cards).build());
    // TODO: sessions reject updates without a CLEAR_ALL or paging with a different token.
    fakeThreadUtils.enforceMainThread(false);
    requestManager.loadMore(
        StreamToken.getDefaultInstance(),
        ConsistencyToken.getDefaultInstance(),
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    int id = 0;
    for (FeatureChangeObserver observer : observers) {
      ArgumentCaptor<FeatureChange> capture = ArgumentCaptor.forClass(FeatureChange.class);
      verify(observer).onChange(capture.capture());
      List<FeatureChange> featureChanges = capture.getAllValues();
      assertThat(featureChanges).hasSize(1);
      FeatureChange change = featureChanges.get(0);
      modelValidator.assertStreamContentId(
          change.getContentId(), protocolAdapter.getStreamContentId(cards[id]));
      assertThat(change.isFeatureChanged()).isTrue();
      assertThat(change.getChildChanges().getAppendedChildren()).isEmpty();
      id++;
    }
    for (FeatureChangeObserver observer : contentObservers) {
      ArgumentCaptor<FeatureChange> capture = ArgumentCaptor.forClass(FeatureChange.class);
      verify(observer).onChange(capture.capture());
      List<FeatureChange> featureChanges = capture.getAllValues();
      assertThat(featureChanges).hasSize(1);
      FeatureChange change = featureChanges.get(0);
      assertThat(change.isFeatureChanged()).isTrue();
    }
  }
}
