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

package com.google.android.libraries.feed.mocknetworkclient;

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.wire.feed.ResponseProto.Response;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockServerConfig;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockUpdate;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link MockServerNetworkClient} class. */
@RunWith(RobolectricTestRunner.class)
public class MockServerNetworkClientTest {

  @Mock RequestManager requestManager;

  private final FeedExtensionRegistry extensionRegistry =
      new FeedExtensionRegistry(
          new ProtoExtensionProvider() {
            @Override
            public List<GeneratedExtension<?, ?>> getProtoExtensions() {
              return new ArrayList<>();
            }
          });

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Test
  public void testMockServerNetworkClient() {
    MockServerConfig mockServerConfig = MockServerConfig.getDefaultInstance();
    List<MockUpdate> mockUpdates = new ArrayList<>();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, mockUpdates, extensionRegistry);
    assertThat(networkClient.hasQuit).isFalse();
    assertThat(networkClient.looper).isNotNull();

    networkClient.quitSafely();
    assertThat(networkClient.hasQuit).isTrue();
  }

  @Test
  public void testHandleUpdate() {
    MockServerConfig mockServerConfig = MockServerConfig.getDefaultInstance();
    List<MockUpdate> mockUpdates = new ArrayList<>();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, mockUpdates, extensionRegistry);
    assertThat(networkClient.requestManager).isNull();
    networkClient.setRequestManager(requestManager);
    assertThat(networkClient.requestManager).isEqualTo(requestManager);

    Response response = Response.getDefaultInstance();
    networkClient.handleUpdate(response);
    verify(requestManager).handlePushedResponse(response);
  }

  @Test
  public void testHandleUpdate_notInitialized() {
    MockServerConfig mockServerConfig = MockServerConfig.getDefaultInstance();
    List<MockUpdate> mockUpdates = new ArrayList<>();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, mockUpdates, extensionRegistry);

    Response response = Response.getDefaultInstance();
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                networkClient.handleUpdate(response);
              }
            })
        .throwsAnExceptionOfType(NullPointerException.class);
  }
}
