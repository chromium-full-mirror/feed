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

import android.net.Uri;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.feedrequestmanager.FeedRequestManager;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.config.ConfigManager;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.common.truth.extensions.proto.LiteProtoTruth;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.wire.feed.ResponseProto.Response;
import com.google.search.now.wire.feed.ResponseProto.Response.ResponseVersion;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockServerConfig;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockServerConfig.ConditionalResponse;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockServerConfig.ConditionalResponse.Condition;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockUpdate;
import org.robolectric.RobolectricTestRunner;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/** Tests of the {@link MockServerNetworkClient} class. */
@RunWith(RobolectricTestRunner.class)
public class MockServerNetworkClientTest {

  @Mock ConfigManager configManager;
  @Mock ProtocolAdapter protocolAdapter;
  @Mock RequestManager requestManager;
  @Captor private ArgumentCaptor<ByteString> tokenCaptor;
  @Captor private ArgumentCaptor<Response> responseCaptor;

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
  public void testSend() throws IOException {
    Response defaultResponse =
        Response.newBuilder().setResponseVersion(ResponseVersion.FEED_RESPONSE).build();
    MockServerConfig mockServerConfig =
        MockServerConfig.newBuilder().setDefaultResponse(defaultResponse).build();
    List<MockUpdate> mockUpdates = new ArrayList<>();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, mockUpdates, extensionRegistry);
    Consumer<HttpResponse> responseConsumer =
        new Consumer<HttpResponse>() {
          @Override
          public void accept(HttpResponse input) {
            try {
              CodedInputStream inputStream =
                  CodedInputStream.newInstance(input.getResponseBody().array());
              int length = inputStream.readRawVarint32();
              assertThat(inputStream.readRawBytes(length)).isEqualTo(defaultResponse.toByteArray());
              assertThat(input.getResponseCode()).isEqualTo(200);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        };
    networkClient.send(
        new HttpRequest(Uri.EMPTY, ByteBuffer.allocate(0), HttpMethod.POST), responseConsumer);
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
    verify(requestManager).handlePushedResponseBytes(response.toByteArray());
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

  @Test
  public void testPaging() {
    Response response =
        Response.newBuilder().setResponseVersion(ResponseVersion.FEED_RESPONSE).build();
    ByteString token = ByteString.copyFromUtf8("fooToken");
    MockServerConfig mockServerConfig =
        MockServerConfig.newBuilder()
            .addConditionalResponses(
                ConditionalResponse.newBuilder()
                    .addConditions(
                        Condition.newBuilder()
                            .setType(Condition.Type.CONTINUATION_TOKEN_MATCH)
                            .setContinuationToken(token))
                    .setResponse(response))
            .build();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, new ArrayList<>(), extensionRegistry);
    FeedRequestManager feedRequestManager =
        new FeedRequestManager(configManager, networkClient, protocolAdapter, extensionRegistry);

    feedRequestManager.loadMore(token);

    verify(protocolAdapter).createModel(responseCaptor.capture(), tokenCaptor.capture());
    LiteProtoTruth.assertThat(responseCaptor.getValue()).isEqualTo(response);
    assertThat(tokenCaptor.getValue()).isEqualTo(token);
  }

  @Test
  public void testPaging_noMatch() {
    Response response =
        Response.newBuilder().setResponseVersion(ResponseVersion.FEED_RESPONSE).build();
    // Create a MockServerConfig without a matching token.
    MockServerConfig mockServerConfig =
        MockServerConfig.newBuilder()
            .addConditionalResponses(
                ConditionalResponse.newBuilder()
                    .addConditions(
                        Condition.newBuilder().setType(Condition.Type.CONTINUATION_TOKEN_MATCH))
                    .setResponse(response))
            .build();
    MockServerNetworkClient networkClient =
        new MockServerNetworkClient(mockServerConfig, new ArrayList<>(), extensionRegistry);
    FeedRequestManager feedRequestManager =
        new FeedRequestManager(configManager, networkClient, protocolAdapter, extensionRegistry);

    ByteString token = ByteString.copyFromUtf8("fooToken");
    feedRequestManager.loadMore(token);

    verify(protocolAdapter).createModel(responseCaptor.capture(), tokenCaptor.capture());
    LiteProtoTruth.assertThat(responseCaptor.getValue()).isEqualToDefaultInstance();
    assertThat(tokenCaptor.getValue()).isEqualTo(token);
  }
}
