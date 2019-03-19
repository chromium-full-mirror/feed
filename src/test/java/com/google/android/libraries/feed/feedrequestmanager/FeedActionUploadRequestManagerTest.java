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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Base64;
import com.google.android.libraries.feed.api.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.api.store.UploadableActionMutation;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.RequiredConsumer;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.wire.feed.ActionRequestProto.ActionRequest;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedActionResponseProto.FeedActionResponse;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.OpaqueActionDataForTestProto.OpaqueActionDataForTest;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

/** Test of the {@link FeedActionUploadRequestManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedActionUploadRequestManagerTest {

  public static final long ID = 42;
  private static final ConsistencyToken TOKEN_1 =
      ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x1, 0xa})).build();
  private static final ConsistencyToken TOKEN_2 =
      ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x1, 0xf})).build();
  private static final ConsistencyToken TOKEN_3 =
      ConsistencyToken.newBuilder().setToken(ByteString.copyFrom(new byte[] {0x2, 0xa})).build();

  @Mock private NetworkClient networkClient;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private ThreadUtils threadUtils;
  @Mock private ActionReader actionReader;
  @Mock private Consumer<Result<List<StreamDataOperation>>> consumer;
  @Mock private Clock clock;
  @Mock private Store store;
  @Mock private UploadableActionMutation actionMutation;

  @Captor private ArgumentCaptor<Consumer<HttpResponse>> responseConsumerCaptor;
  @Captor private ArgumentCaptor<HttpRequest> requestCaptor;
  private ExtensionRegistryLite registry;
  private FeedActionUploadRequestManager requestManager;
  private FakeMainThreadRunner fakeMainThreadRunner;
  private final Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setUp() {
    initMocks(this);
    FeedExtensionRegistry feedExtensionRegistry = new FeedExtensionRegistry(ArrayList::new);
    registry = ExtensionRegistryLite.newInstance();
    registry.add(FeedRequest.feedRequest);
    fakeMainThreadRunner = FakeMainThreadRunner.runTasksImmediately();

    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", VERSION_CODES.KITKAT);
    ReflectionHelpers.setStaticField(Build.VERSION.class, "RELEASE", "4.4.3");
    ReflectionHelpers.setStaticField(Build.class, "CPU_ABI", "armeabi");
    ReflectionHelpers.setStaticField(Build.class, "TAGS", "dev-keys");
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            feedExtensionRegistry,
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
  }

  @Test
  public void testTriggerUploadActions_ttlExceededRemove() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 5)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action =
        StreamUploadableAction.newBuilder()
            .setUploadAttempts(1)
            .setTimestampSeconds(1L)
            .setFeatureContentId(contentId)
            .build();
    actionSet.add(action);
    RequiredConsumer<Result<ConsistencyToken>> consumer = new RequiredConsumer<>(input -> {});
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(clock.currentTimeMillis()).thenReturn(5000L);
    when(actionReader.getActionProperties(Collections.singletonList(contentId)))
        .thenReturn(
            Result.success(Collections.singletonList(new ActionPropertiesWithId(contentId, data))));
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, ConsistencyToken.getDefaultInstance(), consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));
    verify(actionMutation).remove(action, contentId);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_maxUploadsRemove() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action =
        StreamUploadableAction.newBuilder()
            .setUploadAttempts(2)
            .setFeatureContentId(contentId)
            .build();
    actionSet.add(action);
    RequiredConsumer<Result<ConsistencyToken>> consumer = new RequiredConsumer<>(input -> {});
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(actionReader.getActionProperties(Collections.singletonList(contentId)))
        .thenReturn(
            Result.success(Collections.singletonList(new ActionPropertiesWithId(contentId, data))));
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, ConsistencyToken.getDefaultInstance(), consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));
    verify(actionMutation)
        .remove(
            StreamUploadableAction.newBuilder()
                .setFeatureContentId(contentId)
                .setUploadAttempts(2)
                .build(),
            contentId);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_batchSuccess() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_SIZE_PER_REQUEST, 20)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_ACTIONS_PER_REQUEST, 2)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    String contentId2 = "contentId2";

    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action1 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build();
    StreamUploadableAction action2 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId2).build();
    actionSet.add(action1);
    actionSet.add(action2);
    RequiredConsumer<Result<ConsistencyToken>> consumer =
        new RequiredConsumer<>(
            input -> {
              assertThat(input.isSuccessful()).isTrue();
              assertThat(input.getValue().toByteArray()).isEqualTo(TOKEN_3.toByteArray());
            });
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    OpaqueActionData data2 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId2).build())
            .build();
    when(actionReader.getActionProperties(anyListOf(String.class)))
        .thenReturn(
            Result.success(
                Arrays.asList(
                    new ActionPropertiesWithId(contentId, data),
                    new ActionPropertiesWithId(contentId2, data2))));
    when(protocolAdapter.getWireContentId(contentId2))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, TOKEN_1, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response1 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_2).build())
            .build();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 200, response1));
    Response response2 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_3).build())
            .build();
    verify(networkClient, times(2)).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 200, response2));
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_batchFirstFailure() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_SIZE_PER_REQUEST, 20)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_ACTIONS_PER_REQUEST, 2)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    String contentId2 = "contentId2";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action1 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build();
    StreamUploadableAction action2 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId2).build();
    actionSet.add(action1);
    actionSet.add(action2);
    RequiredConsumer<Result<ConsistencyToken>> consumer =
        new RequiredConsumer<>(
            input -> {
              assertThat(input.isSuccessful()).isFalse();
            });
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    OpaqueActionData data2 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId2).build())
            .build();
    when(actionReader.getActionProperties(anyListOf(String.class)))
        .thenReturn(
            Result.success(
                Arrays.asList(
                    new ActionPropertiesWithId(contentId, data),
                    new ActionPropertiesWithId(contentId2, data2))));
    when(protocolAdapter.getWireContentId(contentId2))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, TOKEN_1, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response1 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_2).build())
            .build();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 500, response1));
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_batchFirstSuccessSecondFailure() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_SIZE_PER_REQUEST, 20)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_ACTIONS_PER_REQUEST, 2)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    String contentId2 = "contentId2";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action1 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build();
    StreamUploadableAction action2 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId2).build();
    actionSet.add(action1);
    actionSet.add(action2);
    RequiredConsumer<Result<ConsistencyToken>> consumer =
        new RequiredConsumer<>(
            input -> {
              assertThat(input.isSuccessful()).isTrue();
              assertThat(input.getValue().toByteArray()).isEqualTo(TOKEN_2.toByteArray());
            });
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    OpaqueActionData data2 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId2).build())
            .build();
    when(actionReader.getActionProperties(anyListOf(String.class)))
        .thenReturn(
            Result.success(
                Arrays.asList(
                    new ActionPropertiesWithId(contentId, data),
                    new ActionPropertiesWithId(contentId2, data2))));
    when(protocolAdapter.getWireContentId(contentId2))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, TOKEN_1, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response1 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_2).build())
            .build();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 200, response1));
    Response response2 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_3).build())
            .build();
    verify(networkClient, times(2)).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 500, response2));

    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_batchFirstReachesMax() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_SIZE_PER_REQUEST, 20)
            .put(ConfigKey.FEED_ACTION_SERVER_MAX_ACTIONS_PER_REQUEST, 1)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    String contentId2 = "contentId2";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action1 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build();
    StreamUploadableAction action2 =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId2).build();
    actionSet.add(action1);
    actionSet.add(action2);
    RequiredConsumer<Result<ConsistencyToken>> consumer =
        new RequiredConsumer<>(
            input -> {
              assertThat(input.isSuccessful()).isTrue();
              assertThat(input.getValue().toByteArray()).isEqualTo(TOKEN_2.toByteArray());
            });
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    OpaqueActionData data2 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId2).build())
            .build();
    when(actionReader.getActionProperties(anyListOf(String.class)))
        .thenReturn(
            Result.success(
                Arrays.asList(
                    new ActionPropertiesWithId(contentId, data),
                    new ActionPropertiesWithId(contentId2, data2))));
    when(protocolAdapter.getWireContentId(contentId2))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, TOKEN_1, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response1 =
        Response.newBuilder()
            .setExtension(
                FeedActionResponse.feedActionResponse,
                FeedActionResponse.newBuilder().setConsistencyToken(TOKEN_2).build())
            .build();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ 200, response1));

    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_getMethod() throws Exception {
    Configuration configuration =
        new Configuration.Builder()
            .put(ConfigKey.FEED_ACTION_SERVER_METHOD, HttpMethod.GET)
            .put(ConfigKey.FEED_ACTION_TTL_SECONDS, 1000L)
            .put(ConfigKey.FEED_ACTION_MAX_UPLOAD_ATTEMPTS, 2)
            .build();
    requestManager =
        new FeedActionUploadRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            getTaskQueue(),
            threadUtils,
            actionReader,
            store,
            clock,
            fakeMainThreadRunner);
    String contentId = "contentId";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    StreamUploadableAction action =
        StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build();
    actionSet.add(action);
    RequiredConsumer<Result<ConsistencyToken>> consumer = new RequiredConsumer<>(input -> {});
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(actionReader.getActionProperties(Collections.singletonList(contentId)))
        .thenReturn(
            Result.success(Collections.singletonList(new ActionPropertiesWithId(contentId, data))));
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, ConsistencyToken.getDefaultInstance(), consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    ActionRequest request = getActionRequestFromHttpRequest(httpRequest);
    UploadableActionsRequestBuilder builder =
        new UploadableActionsRequestBuilder(actionReader, protocolAdapter);
    ActionRequest expectedRequest =
        builder
            .setConsistencyToken(ConsistencyToken.getDefaultInstance())
            .setActions(actionSet)
            .build();
    assertThat(request.toByteArray()).isEqualTo(expectedRequest.toByteArray());

    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));
    verify(actionMutation)
        .upsert(
            StreamUploadableAction.newBuilder()
                .setFeatureContentId(contentId)
                .setUploadAttempts(1)
                .build(),
            contentId);
    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testTriggerUploadActions_defaultMethod() throws Exception {
    String contentId = "contentId";
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    actionSet.add(StreamUploadableAction.newBuilder().setFeatureContentId(contentId).build());
    RequiredConsumer<Result<ConsistencyToken>> consumer = new RequiredConsumer<>(input -> {});
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(contentId).build())
            .build();
    when(actionReader.getActionProperties(Collections.singletonList(contentId)))
        .thenReturn(
            Result.success(Collections.singletonList(new ActionPropertiesWithId(contentId, data))));
    when(protocolAdapter.getWireContentId(contentId))
        .thenReturn(Result.success(ContentId.getDefaultInstance()));
    when(store.editUploadableActions()).thenReturn(actionMutation);
    requestManager.triggerUploadActions(actionSet, ConsistencyToken.getDefaultInstance(), consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();

    ActionRequest request = getActionRequestFromHttpRequestBody(httpRequest);
    UploadableActionsRequestBuilder builder =
        new UploadableActionsRequestBuilder(actionReader, protocolAdapter);
    ActionRequest expectedRequest =
        builder
            .setConsistencyToken(ConsistencyToken.getDefaultInstance())
            .setActions(actionSet)
            .build();
    assertThat(request.toByteArray()).isEqualTo(expectedRequest.toByteArray());

    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));

    assertThat(consumer.isCalled()).isTrue();
  }

  private static void assertHttpRequestFormattedCorrectly(HttpRequest httpRequest) {
    assertThat(httpRequest.getBody()).hasLength(0);
    assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(httpRequest.getUri().getQueryParameter("fmt")).isEqualTo("bin");
    assertThat(httpRequest.getUri().getQueryParameter(RequestHelper.MOTHERSHIP_PARAM_PAYLOAD))
        .isNotNull();
  }

  private static HttpResponse createHttpResponse(int responseCode, Response response)
      throws IOException {
    byte[] rawResponse = response.toByteArray();
    ByteBuffer buffer = ByteBuffer.allocate(rawResponse.length + (Integer.SIZE / 8));
    CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
    codedOutputStream.writeUInt32NoTag(rawResponse.length);
    codedOutputStream.writeRawBytes(rawResponse);
    codedOutputStream.flush();
    return new HttpResponse(responseCode, buffer.array());
  }

  private TaskQueue getTaskQueue() {
    TaskQueue taskQueue =
        new TaskQueue(MoreExecutors.directExecutor(), fakeMainThreadRunner, clock, false);
    taskQueue.initialize(() -> {});
    return taskQueue;
  }

  private ActionRequest getActionRequestFromHttpRequest(HttpRequest httpRequest) throws Exception {
    return ActionRequest.parseFrom(
        Base64.decode(
            httpRequest.getUri().getQueryParameter(RequestHelper.MOTHERSHIP_PARAM_PAYLOAD),
            Base64.URL_SAFE),
        registry);
  }

  private ActionRequest getActionRequestFromHttpRequestBody(HttpRequest httpRequest)
      throws Exception {
    return ActionRequest.parseFrom(httpRequest.getBody(), registry);
  }
}
