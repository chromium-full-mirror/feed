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

import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.ROOT_CONTENT_ID;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** This will test the behavior of clear all. */
@RunWith(RobolectricTestRunner.class)
public class ClearAllTest {
  @Mock private ThreadUtils threadUtils;

  private FakeRequestManager requestManager;
  private SessionManager sessionManager;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;
  private AppLifecycleListener appLifecycleListener;

  @Before
  public void setUp() {
    initMocks(this);
    Configuration configuration =
        new Configuration.Builder().put(ConfigKey.LIMIT_PAGE_UPDATES, false).build();
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, MoreExecutors.newDirectExecutorService())
            .setConfiguration(configuration)
            .build();
    requestManager = scope.getRequestManager();
    sessionManager = scope.getSessionManager();
    modelProviderFactory = scope.getModelProviderFactory();
    appLifecycleListener = scope.getAppLifecycleListener();
    modelValidator = new ModelProviderValidator(scope.getProtocolAdapter());
  }

  /**
   * This test creates two sessions/ModelProviders then will trigger the clear all. We then verify
   * the expected behavior that the previously created ModelProviders are invalid.
   */
  @Test
  public void testClearAll() {
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2),
          WireProtocolResponseBuilder.createFeatureContentId(3)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    requestManager.queueResponse(responseBuilder.build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider1 = modelProviderFactory.createNew(null);
    modelValidator.assertCursorContents(modelProvider1, cards);
    ModelProvider modelProvider2 = modelProviderFactory.createNew(null);
    modelValidator.assertCursorContents(modelProvider2, cards);

    appLifecycleListener.onClearAll();

    assertThat(modelProvider1.getCurrentState()).isEqualTo(State.INVALIDATED);
    assertThat(modelProvider2.getCurrentState()).isEqualTo(State.INVALIDATED);
  }

  /**
   * This test create a session/ModelProvider then calls clear all. It this validates validates
   * creating a new empty ModelProvider and attempts to create the previously created ModelProvider.
   */
  @Test
  public void testPostClearAll() {
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2),
          WireProtocolResponseBuilder.createFeatureContentId(3)
        };
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    requestManager.queueResponse(responseBuilder.build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    modelValidator.assertCursorContents(modelProvider, cards);
    String modelToken = modelProvider.getSessionId();
    appLifecycleListener.onClearAll();

    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);

    // create a new (Empty) ModelProvider
    modelProvider = modelProviderFactory.createNew(null);
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider.getRootFeature()).isNull();

    // try to create the old existing ModelProvider
    modelProvider = modelProviderFactory.create(modelToken);
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.INVALIDATED);
  }
}
