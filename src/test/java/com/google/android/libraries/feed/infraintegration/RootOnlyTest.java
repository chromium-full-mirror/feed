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
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.WireProtocolInfo;
import com.google.common.util.concurrent.MoreExecutors;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of a Stream with only a root. */
@RunWith(RobolectricTestRunner.class)
public class RootOnlyTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private RequestManager requstManager;

  private ProtocolAdapter protocolAdapter;
  private SessionManager sessionManager;
  private ModelProviderValidator modelValidator;

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, requstManager, MoreExecutors.newDirectExecutorService())
            .build();
    protocolAdapter = scope.getProtocolAdapter();
    sessionManager = scope.getSessionManager();
    modelValidator = new ModelProviderValidator(protocolAdapter);
  }

  @Test
  public void rootOnlyResponse_beforeSession() {
    // ModelProvider is created from $HEAD
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    protocolAdapter.createModel(responseBuilder.build(), null);
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();

    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    modelValidator.assertRoot(modelProvider);

    WireProtocolInfo protocolInfo = responseBuilder.getWireProtocolInfo();
    // 1 root
    assertThat(protocolInfo.featureCount).isEqualTo(1);
    assertThat(protocolInfo.hasClearOperation).isTrue();
  }

  @Test
  public void rootOnlyResponse_afterSession() {
    // ModelProvider created from empty $HEAD, followed by a response adding head
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addRootFeature();
    protocolAdapter.createModel(responseBuilder.build(), null);

    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    modelValidator.assertRoot(modelProvider);

    WireProtocolInfo protocolInfo = responseBuilder.getWireProtocolInfo();
    // 1 root
    assertThat(protocolInfo.featureCount).isEqualTo(1);
    assertThat(protocolInfo.hasClearOperation).isFalse();
  }
}
