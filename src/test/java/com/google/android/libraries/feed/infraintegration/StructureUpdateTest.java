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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
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
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

/** Tests which update (append) content to an existing model. */
@RunWith(RobolectricTestRunner.class)
public class StructureUpdateTest {
  private FakeRequestManager requestManager;
  private SessionManager sessionManager;
  private ProtocolAdapter protocolAdapter;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;

  @Before
  public void setUp() {
    initMocks(this);
    InfraIntegrationScope scope = new InfraIntegrationScope.Builder().build();
    requestManager = scope.getRequestManager();
    sessionManager = scope.getSessionManager();
    modelProviderFactory = scope.getModelProviderFactory();
    protocolAdapter = scope.getProtocolAdapter();
    modelValidator = new ModelProviderValidator(protocolAdapter);
  }

  @Test
  public void appendChildren() {
    // Create a simple stream with a root and two features
    ContentId[] startingCards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1), ResponseBuilder.createFeatureContentId(2)
        };
    // Define two features to be appended to the root
    ContentId[] appendedCards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(3), ResponseBuilder.createFeatureContentId(4)
        };

    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(startingCards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    ModelFeature root = modelProvider.getRootFeature();
    assertThat(root).isNotNull();
    FeatureChangeObserver rootObserver = mock(FeatureChangeObserver.class);
    root.registerObserver(rootObserver);
    modelValidator.assertCursorSize(root.getCursor(), startingCards.length);

    // Append new children to root
    requestManager.queueResponse(ResponseBuilder.builder().addCardsToRoot(appendedCards).build());
    // TODO: sessions reject updates without a CLEAR_ALL or paging with a different token.
    requestManager.loadMore(
        StreamToken.getDefaultInstance(),
        ConsistencyToken.getDefaultInstance(),
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // assert the new state of the stream
    modelValidator.assertCursorSize(root.getCursor(), startingCards.length + appendedCards.length);
    ArgumentCaptor<FeatureChange> capture = ArgumentCaptor.forClass(FeatureChange.class);
    verify(rootObserver).onChange(capture.capture());
    List<FeatureChange> featureChanges = capture.getAllValues();
    assertThat(featureChanges).hasSize(1);
    FeatureChange change = featureChanges.get(0);
    assertThat(change.getChildChanges().getAppendedChildren()).hasSize(appendedCards.length);
    assertThat(change.isFeatureChanged()).isFalse();
    int i = 0;
    for (ModelChild appendedChild : change.getChildChanges().getAppendedChildren()) {
      modelValidator.assertStreamContentId(
          appendedChild.getContentId(), protocolAdapter.getStreamContentId(appendedCards[i++]));
    }
  }

  @Test
  public void appendChildren_concurrentModification() {
    // Test which verifies the root can be updated while we advance a cursor (without
    // a ConcurrentModificationException
    // Create a simple stream with a root and two features
    ContentId[] startingCards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1), ResponseBuilder.createFeatureContentId(2)
        };
    // Define two features to be appended to the root
    ContentId[] appendedCards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(3), ResponseBuilder.createFeatureContentId(4)
        };

    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(startingCards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    ModelFeature root = modelProvider.getRootFeature();
    assertThat(root).isNotNull();
    FeatureChangeObserver rootObserver = mock(FeatureChangeObserver.class);
    root.registerObserver(rootObserver);
    ModelCursor cursor = root.getCursor();
    cursor.getNextItem();

    // Now append additional children to the stream (and cursor)
    requestManager.queueResponse(ResponseBuilder.builder().addCardsToRoot(appendedCards).build());
    // TODO: sessions reject updates without a CLEAR_ALL or paging with a different token.
    requestManager.loadMore(
        StreamToken.getDefaultInstance(),
        ConsistencyToken.getDefaultInstance(),
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    modelValidator.assertCursorSize(cursor, 3);
  }
}
