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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests which verify creating a new Model Provider from an existing session. */
@RunWith(RobolectricTestRunner.class)
public class ExistingSessionTest {
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
  public void createModelProvider() {
    // Create a simple stream with a root and three features
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2),
          WireProtocolResponseBuilder.createFeatureContentId(3)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, WireProtocolResponseBuilder.ROOT_CONTENT_ID);
    }
    protocolAdapter.createModel(responseBuilder.build(), null);
    ModelProvider modelProvider = modelProviderFactory.createNew();

    ModelFeature initRoot = modelProvider.getRootFeature();
    assertThat(initRoot).isNotNull();
    ModelCursor initCursor = initRoot.getCursor();
    assertThat(initCursor).isNotNull();

    String sessionToken = modelProvider.getSessionToken();
    assertThat(sessionToken).isNotEmpty();

    ModelProvider modelProvider2 = modelProviderFactory.create(sessionToken);
    assertThat(modelProvider2).isNotNull();
    ModelFeature root2 = modelProvider2.getRootFeature();
    assertThat(root2).isNotNull();
    ModelCursor cursor2 = root2.getCursor();
    assertThat(cursor2).isNotNull();
    assertThat(modelProvider2.getSessionToken()).isEqualTo(sessionToken);
    modelValidator.assertCursorContents(cursor2, cards);

    // Creating the new session will invalidate the previous ModelProvider and Cursor
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);
    assertThat(initCursor.isAtEnd()).isTrue();
  }

  @Test
  public void createModelProvider_unknownSession() {
    // Create a simple stream with a root and three features
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2),
          WireProtocolResponseBuilder.createFeatureContentId(3)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, WireProtocolResponseBuilder.ROOT_CONTENT_ID);
    }
    protocolAdapter.createModel(responseBuilder.build(), null);
    ModelProvider modelProvider = modelProviderFactory.createNew();

    ModelFeature initRoot = modelProvider.getRootFeature();
    assertThat(initRoot).isNotNull();
    ModelCursor initCursor = initRoot.getCursor();
    assertThat(initCursor).isNotNull();

    String sessionToken = modelProvider.getSessionToken();
    assertThat(sessionToken).isNotEmpty();

    // Create a second model provider using an unknown session token
    ModelProvider modelProvider2 = modelProviderFactory.create("unknown-session");
    assertThat(modelProvider2).isNotNull();
    ModelFeature root2 = modelProvider2.getRootFeature();
    assertThat(root2).isNotNull();
    ModelCursor cursor2 = root2.getCursor();
    assertThat(cursor2).isNotNull();
    modelValidator.assertCursorContents(cursor2, cards);

    // Creating the new session will invalidate the previous ModelProvider and Cursor
    assertThat(modelProvider2.getSessionToken()).isNotEqualTo(sessionToken);
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(initCursor.isAtEnd()).isFalse();
    modelValidator.assertCursorContents(initCursor, cards);
  }
}
