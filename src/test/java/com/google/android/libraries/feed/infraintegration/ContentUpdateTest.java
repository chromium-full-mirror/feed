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

package com.google.android.libraries.feed.infraintegration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.FeatureChange;
import com.google.android.libraries.feed.api.modelprovider.FeatureChangeObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Tests which update content within an existing model. */
@RunWith(RobolectricTestRunner.class)
public class ContentUpdateTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private RequestManager requestManager;

  private ProtocolAdapter protocolAdapter;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, requestManager, MoreExecutors.newDirectExecutorService())
            .build();
    protocolAdapter = scope.getProtocolAdapter();
    modelProviderFactory = scope.getModelProviderFactory();
    modelValidator = new ModelProviderValidator(protocolAdapter);
  }

  @Test
  public void updateContent_observers() {
    // Create a simple stream with a root and two features
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, WireProtocolResponseBuilder.ROOT_CONTENT_ID);
    }
    protocolAdapter.createModel(responseBuilder.build(), null);
    ModelProvider modelProvider = modelProviderFactory.createNew();

    ModelFeature root = modelProvider.getRootFeature();
    assertThat(root).isNotNull();
    ModelCursor cursor = root.getCursor();
    List<FeatureChangeObserver> observers = new ArrayList<>();
    List<FeatureChangeObserver> contentObservers = new ArrayList<>();
    for (ContentId contentId : cards) {
      cursor.advanceCursor(
          new Consumer<ModelChild>() {
            @Override
            public void accept(ModelChild child) {
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
              cardCursor.advanceCursor(
                  new Consumer<ModelChild>() {
                    @Override
                    public void accept(ModelChild child) {
                      child.getModelFeature().registerObserver(contentObserver);
                    }
                  });
            }
          });
    }
    assertThat(cursor.isAtEnd()).isTrue();

    // Create an update response for the two content items
    responseBuilder = new WireProtocolResponseBuilder();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, WireProtocolResponseBuilder.ROOT_CONTENT_ID);
    }
    protocolAdapter.createModel(responseBuilder.build(), null);

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
