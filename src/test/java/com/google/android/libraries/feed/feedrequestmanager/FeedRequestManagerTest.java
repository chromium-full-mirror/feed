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
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.util.Base64;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.host.config.ConfigManager;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.common.truth.extensions.proto.LiteProtoTruth;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.RequestProto.Request.RequestVersion;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/** Test of the {@link FeedRequestManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedRequestManagerTest {

  @Mock private ConfigManager configManager;
  @Mock private NetworkClient networkClient;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private FeedExtensionRegistry feedExtensionRegistry;
  @Captor private ArgumentCaptor<HttpRequest> requestCaptor;
  @Captor private ArgumentCaptor<Consumer<HttpResponse>> responseConsumerCaptor;
  private ExtensionRegistryLite registry;
  private FeedRequestManager requestManager;

  @Before
  public void setUp() {
    initMocks(this);
    registry = ExtensionRegistryLite.newInstance();
    registry.add(FeedRequest.feedRequest);
    requestManager =
        new FeedRequestManager(
            configManager, networkClient, protocolAdapter, feedExtensionRegistry);
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
}
