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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.WireProtocolInfo;
import com.google.common.util.concurrent.MoreExecutors;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Test which verifies the ModelProvider state for the empty stream cases. */
@RunWith(RobolectricTestRunner.class)
public class EmptyStreamTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private RequestManager requstManager;

  private ProtocolAdapter protocolAdapter;
  private SessionManager sessionManager;

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, requstManager, MoreExecutors.newDirectExecutorService())
            .build();
    protocolAdapter = scope.getProtocolAdapter();
    sessionManager = scope.getSessionManager();
  }

  @Test
  public void emptyStream_observable() {
    // ModelProvider will be initialized from empty $HEAD
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider.getRootFeature()).isNull();
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);
    verify(observer).onSessionStart();
  }

  @Test
  public void emptyStream_observableInvalidate() {
    // Verify both ModelProviderObserver events are called
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);
    verify(observer).onSessionStart();
    modelProvider.invalidate();
    verify(observer).onSessionFinished();
  }

  @Test
  public void emptyStream_unregisterObservable() {
    // Verify unregister works, so the ModelProviderObserver is not called for invalidate
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    ModelProviderObserver observer = mock(ModelProviderObserver.class);
    modelProvider.registerObserver(observer);
    verify(observer).onSessionStart();
    modelProvider.unregisterObserver(observer);
    modelProvider.invalidate();
    verify(observer, never()).onSessionFinished();
  }

  @Test
  public void emptyStream_emptyResponse() {
    // Create an empty stream through a response
    WireProtocolResponseBuilder responseBuilder = new WireProtocolResponseBuilder();
    protocolAdapter.createModel(responseBuilder.build(), null);
    ModelProvider modelProvider = sessionManager.getNewSession().getModelProvider();

    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getCurrentState()).isEqualTo(State.READY);
    assertThat(modelProvider.getRootFeature()).isNull();

    WireProtocolInfo protocolInfo = responseBuilder.getWireProtocolInfo();
    // No features added
    assertThat(protocolInfo.featureCount).isEqualTo(0);
    assertThat(protocolInfo.hasClearOperation).isFalse();
  }
}
