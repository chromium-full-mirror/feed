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
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.ResponseBuilder;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests of accessing shared state. */
@RunWith(RobolectricTestRunner.class)
public class SharedStateTest {
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
  public void sharedState_headBeforeModelProvider() {
    // ModelProvider is created from $HEAD containing content, simple shared state added
    ResponseBuilder responseBuilder =
        new ResponseBuilder().addClearOperation().addPietSharedState().addRootFeature();
    requestManager.queueResponse(responseBuilder.build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    StreamSharedState sharedState = modelProvider.getSharedState(ResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNotNull();
    modelValidator.assertStreamContentId(
        sharedState.getContentId(),
        protocolAdapter.getStreamContentId(ResponseBuilder.PIET_SHARED_STATE));
  }

  @Test
  public void sharedState_headAfterModelProvider() {
    // ModelProvider is created from empty $HEAD, simple shared state added
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    StreamSharedState sharedState = modelProvider.getSharedState(ResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNull();

    ResponseBuilder responseBuilder =
        new ResponseBuilder().addClearOperation().addPietSharedState().addRootFeature();
    requestManager.queueResponse(responseBuilder.build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    sharedState = modelProvider.getSharedState(ResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNotNull();
    modelValidator.assertStreamContentId(
        sharedState.getContentId(),
        protocolAdapter.getStreamContentId(ResponseBuilder.PIET_SHARED_STATE));
  }
}
