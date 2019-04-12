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

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.ResponseBuilder;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelCursor;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelFeature;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProvider;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests which verify creating a new Model Provider from an existing session. */
@RunWith(RobolectricTestRunner.class)
public class ExistingSessionTest {
  private static final ContentId[] CARDS =
      new ContentId[] {
        ResponseBuilder.createFeatureContentId(1),
        ResponseBuilder.createFeatureContentId(2),
        ResponseBuilder.createFeatureContentId(3)
      };

  private final InfraIntegrationScope scope = new InfraIntegrationScope.Builder().build();
  private final ModelProviderFactory modelProviderFactory = scope.getModelProviderFactory();
  private final ModelProviderValidator modelValidator =
      new ModelProviderValidator(scope.getProtocolAdapter());

  @Before
  public void setUp() {
    // Create a simple stream with a root and three features
    scope
        .getRequestManager()
        .queueResponse(ResponseBuilder.forClearAllWithCards(CARDS).build())
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
  }

  @After
  public void tearDown() {
    assertThat(scope.getTaskQueue().hasBacklog()).isFalse();
    assertThat(scope.getFakeMainThreadRunner().hasPendingTasks()).isFalse();
  }

  @Test
  public void createModelProvider() {
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
    modelValidator.assertCursorContents(cursor2, CARDS);

    // Creating the new session will invalidate the previous ModelProvider and Cursor
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);
    assertThat(initCursor.isAtEnd()).isTrue();
  }

  @Test
  public void createModelProvider_restoreSession() {
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    String sessionId = modelProvider.getSessionId();
    modelProvider.detachModelProvider();

    // Restore the session.
    ModelProvider restoredModelProvider = modelProviderFactory.create(sessionId);
    assertThat(restoredModelProvider).isNotNull();
    assertThat(restoredModelProvider.getAllRootChildren()).hasSize(CARDS.length);
  }

  @Test
  public void createModelProvider_restoreSessionWithOutstandingRequest() {
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    String sessionId = modelProvider.getSessionId();
    modelProvider.detachModelProvider();

    // Start a request.
    scope
        .getRequestManager()
        .queueResponse(ResponseBuilder.forClearAllWithCards(CARDS).build(), /* delayMs= */ 100)
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // Restore the session.
    ModelProvider restoredModelProvider = modelProviderFactory.create(sessionId);
    assertThat(restoredModelProvider.getAllRootChildren()).hasSize(CARDS.length);

    // Advance the clock to process the outstanding request.
    scope.getFakeClock().advance(100);
  }

  @Test
  public void createModelProvider_unknownSession() {
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
    modelValidator.assertCursorContents(cursor2, CARDS);

    // two sessions against the same $HEAD instance
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.READY);
  }

  @Test
  public void createModelProvider_invalidatedSession() {
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
