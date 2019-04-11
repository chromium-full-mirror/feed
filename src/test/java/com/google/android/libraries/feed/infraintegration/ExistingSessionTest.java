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

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.ResponseBuilder;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelCursor;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelFeature;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProvider;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests which verify creating a new Model Provider from an existing session. */
@RunWith(RobolectricTestRunner.class)
public class ExistingSessionTest {
  private FakeRequestManager requestManager;
  private SessionManager sessionManager;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;

  @Before
  public void setUp() {
    initMocks(this);
    InfraIntegrationScope scope = new InfraIntegrationScope.Builder().build();
    requestManager = scope.getRequestManager();
    sessionManager = scope.getSessionManager();
    modelProviderFactory = scope.getModelProviderFactory();
    modelValidator = new ModelProviderValidator(scope.getProtocolAdapter());
  }

  @Test
  public void createModelProvider() {
    // Create a simple stream with a root and three features
    ContentId[] cards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1),
          ResponseBuilder.createFeatureContentId(2),
          ResponseBuilder.createFeatureContentId(3)
        };
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(cards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    ModelFeature initRoot = modelProvider.getRootFeature();
    assertThat(initRoot).isNotNull();
    ModelCursor initCursor = initRoot.getCursor();
    assertThat(initCursor).isNotNull();

    String sessionId = modelProvider.getSessionId();
    assertThat(sessionId).isNotEmpty();

    ModelProvider modelProvider2 = modelProviderFactory.create(sessionId);
    assertThat(modelProvider2).isNotNull();
    ModelFeature root2 = modelProvider2.getRootFeature();
    assertThat(root2).isNotNull();
    ModelCursor cursor2 = root2.getCursor();
    assertThat(cursor2).isNotNull();
    assertThat(modelProvider2.getSessionId()).isEqualTo(sessionId);
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
          ResponseBuilder.createFeatureContentId(1),
          ResponseBuilder.createFeatureContentId(2),
          ResponseBuilder.createFeatureContentId(3)
        };
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(cards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    ModelFeature initRoot = modelProvider.getRootFeature();
    assertThat(initRoot).isNotNull();
    ModelCursor initCursor = initRoot.getCursor();
    assertThat(initCursor).isNotNull();

    String sessionId = modelProvider.getSessionId();
    assertThat(sessionId).isNotEmpty();

    // Create a second model provider using an unknown session token, this should return null
    ModelProvider modelProvider2 = modelProviderFactory.create("unknown-session");
    assertThat(modelProvider2).isNotNull();
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.INVALIDATED);

    // Now create one from head
    modelProvider2 = modelProviderFactory.createNew(null);
    assertThat(modelProvider2).isNotNull();
    ModelFeature root2 = modelProvider2.getRootFeature();
    assertThat(root2).isNotNull();
    ModelCursor cursor2 = root2.getCursor();
    assertThat(cursor2).isNotNull();
    modelValidator.assertCursorContents(cursor2, cards);

    // two sessions against the same $HEAD instance
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.READY);
  }

  @Test
  public void createModelProvider_invalidatedSession() {
    // Create a simple stream with a root and three features
    ContentId[] cards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1),
          ResponseBuilder.createFeatureContentId(2),
          ResponseBuilder.createFeatureContentId(3)
        };
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(cards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    String sessionId = modelProvider.getSessionId();
    assertThat(sessionId).isNotEmpty();

    modelProvider.invalidate();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);

    // Attempt to connect with the session token we just invalidated.  This will result in a
    // INVALIDATED ModelProvider.
    ModelProvider modelProvider2 = modelProviderFactory.create(sessionId);
    assertThat(modelProvider2).isNotNull();
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.INVALIDATED);
  }

  @Test
  public void createModelProvider_detachSession() {
    // Create a simple stream with a root and three features
    ContentId[] cards =
        new ContentId[] {
          ResponseBuilder.createFeatureContentId(1),
          ResponseBuilder.createFeatureContentId(2),
          ResponseBuilder.createFeatureContentId(3)
        };
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(cards).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    String sessionId = modelProvider.getSessionId();
    assertThat(sessionId).isNotEmpty();

    modelProvider.detachModelProvider();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);

    // Attempt to connect with the session token we just detached.  This will work as expected.
    ModelProvider modelProvider2 = modelProviderFactory.create(sessionId);
    assertThat(modelProvider2).isNotNull();
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.READY);
  }
}
