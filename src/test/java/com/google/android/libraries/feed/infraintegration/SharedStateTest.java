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

import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.EMPTY_MUTATION_CONTEXT;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of accessing shared state. */
@RunWith(RobolectricTestRunner.class)
public class SharedStateTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private RequestManager requstManager;

  private ProtocolAdapter protocolAdapter;
  private ModelProviderFactory modelProviderFactory;
  private ModelProviderValidator modelValidator;

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, requstManager, MoreExecutors.newDirectExecutorService())
            .build();
    protocolAdapter = scope.getProtocolAdapter();
    modelProviderFactory = scope.getModelProviderFactory();
    modelValidator = new ModelProviderValidator(protocolAdapter);
  }

  @Test
  public void sharedState_headBeforeModelProvider() {
    // ModelProvider is created from $HEAD containing content, simple shared state added
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addPietSharedState().addRootFeature();
    protocolAdapter.createModel(responseBuilder.build(), EMPTY_MUTATION_CONTEXT);

    ModelProvider modelProvider = modelProviderFactory.createNew();
    StreamSharedState sharedState =
        modelProvider.getSharedState(WireProtocolResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNotNull();
    modelValidator.assertStreamContentId(
        sharedState.getContentId(),
        protocolAdapter.getStreamContentId(WireProtocolResponseBuilder.PIET_SHARED_STATE));
  }

  @Test
  public void sharedState_headAfterModelProvider() {
    // ModelProvider is created from empty $HEAD, simple shared state added
    ModelProvider modelProvider = modelProviderFactory.createNew();
    StreamSharedState sharedState =
        modelProvider.getSharedState(WireProtocolResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNull();

    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addPietSharedState().addRootFeature();
    protocolAdapter.createModel(responseBuilder.build(), EMPTY_MUTATION_CONTEXT);

    sharedState = modelProvider.getSharedState(WireProtocolResponseBuilder.PIET_SHARED_STATE);
    assertThat(sharedState).isNotNull();
    modelValidator.assertStreamContentId(
        sharedState.getContentId(),
        protocolAdapter.getStreamContentId(WireProtocolResponseBuilder.PIET_SHARED_STATE));
  }
}
