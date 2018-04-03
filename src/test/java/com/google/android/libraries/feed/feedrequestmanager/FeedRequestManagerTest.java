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

package com.google.android.libraries.feed.feedrequestmanager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.util.Base64;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.common.truth.extensions.proto.LiteProtoTruth;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.RequestProto.Request.RequestVersion;
import com.google.search.now.wire.feed.ResponseProto.Response;
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

/** Test of the {@link FeedRequestManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedRequestManagerTest {

  public static final int NOT_FOUND = 404;
  @Mock private NetworkClient networkClient;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private SchedulerApi scheduler;
  @Captor private ArgumentCaptor<ByteString> byteStringCaptor;
  @Captor private ArgumentCaptor<Consumer<HttpResponse>> responseConsumerCaptor;
  @Captor private ArgumentCaptor<HttpRequest> requestCaptor;
  @Captor private ArgumentCaptor<Response> responseCaptor;
  private ExtensionRegistryLite registry;
  private FeedExtensionRegistry feedExtensionRegistry;
  private FeedRequestManager requestManager;

  private final Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setUp() {
    initMocks(this);
    feedExtensionRegistry =
        new FeedExtensionRegistry(
            new ProtoExtensionProvider() {
              @Override
              public List<GeneratedExtension<?, ?>> getProtoExtensions() {
                return new ArrayList<>();
              }
            });
    registry = ExtensionRegistryLite.newInstance();
    registry.add(FeedRequest.feedRequest);
    requestManager =
        new FeedRequestManager(
            configuration, networkClient, protocolAdapter, feedExtensionRegistry, scheduler);
  }

  @Test
  public void testTriggerRefresh() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertThat(httpRequest.getBody().array()).hasLength(0);
    assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(httpRequest.getUri().getQueryParameter("fmt")).isEqualTo("bin");
    assertThat(httpRequest.getUri().getQueryParameter("reqpld")).isNotNull();

    Request request =
        Request.parseFrom(
            Base64.decode(httpRequest.getUri().getQueryParameter("reqpld"), Base64.URL_SAFE),
            registry);
    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                FeedRequest.newBuilder().setFeedQuery(FeedQuery.getDefaultInstance()).build())
            .build();
    LiteProtoTruth.assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testHandleResponse() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));

    verify(protocolAdapter).createModel(responseCaptor.capture(), byteStringCaptor.capture());
    LiteProtoTruth.assertThat(responseCaptor.getValue()).isEqualTo(response);
    assertThat(byteStringCaptor.getValue()).isNull();

    verify(scheduler).onReceiveNewContent();
  }

  @Test
  public void testHandleResponse_notFound() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response = Response.getDefaultInstance();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ NOT_FOUND, response));

    verify(scheduler).onRequestError(NOT_FOUND);
  }

  @Test
  public void testHandleResponse_missingLengthPrefixNotSupported() {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    responseConsumerCaptor
        .getValue()
        .accept(
            new HttpResponse(
                /* responseCode= */ 200,
                ByteBuffer.wrap(Response.getDefaultInstance().toByteArray())));

    verify(protocolAdapter, never()).createModel(any(), any());
  }

  @Test
  public void testHandlePushedResponseBytes() {
    Response response = Response.getDefaultInstance();
    requestManager.handlePushedResponseBytes(response.toByteArray());

    verify(protocolAdapter).createModel(responseCaptor.capture(), byteStringCaptor.capture());
    LiteProtoTruth.assertThat(responseCaptor.getValue()).isEqualTo(response);
    assertThat(byteStringCaptor.getValue()).isNull();
  }

  private HttpResponse createHttpResponse(int responseCode, Response response) throws IOException {
    byte[] rawResponse = response.toByteArray();
    ByteBuffer buffer = ByteBuffer.allocate(rawResponse.length + (Integer.SIZE / 8));
    CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
    codedOutputStream.writeUInt32NoTag(rawResponse.length);
    codedOutputStream.writeRawBytes(rawResponse);
    codedOutputStream.flush();
    return new HttpResponse(responseCode, buffer);
  }
}
