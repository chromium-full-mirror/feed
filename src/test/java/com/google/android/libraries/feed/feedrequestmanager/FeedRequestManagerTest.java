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
import static com.google.common.truth.extensions.proto.LiteProtoTruth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Base64;
import com.google.android.libraries.feed.api.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.common.DismissActionWithSemanticProperties;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.RequiredConsumer;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.host.config.ApplicationInfo;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ActionTypeProto.ActionType;
import com.google.search.now.wire.feed.CapabilityProto.Capability;
import com.google.search.now.wire.feed.ClientInfoProto.ClientInfo;
import com.google.search.now.wire.feed.ClientInfoProto.ClientInfo.AppType;
import com.google.search.now.wire.feed.ClientInfoProto.ClientInfo.PlatformType;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedActionQueryDataProto.Action;
import com.google.search.now.wire.feed.FeedActionQueryDataProto.FeedActionQueryData;
import com.google.search.now.wire.feed.FeedActionQueryDataProto.FeedActionQueryDataItem;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.RequestProto.Request.RequestVersion;
import com.google.search.now.wire.feed.ResponseProto.Response;
import com.google.search.now.wire.feed.SemanticPropertiesProto.SemanticProperties;
import com.google.search.now.wire.feed.VersionProto.Version;
import com.google.search.now.wire.feed.VersionProto.Version.Architecture;
import com.google.search.now.wire.feed.VersionProto.Version.BuildType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/** Test of the {@link FeedRequestManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedRequestManagerTest {

  private static final int NOT_FOUND = 404;
  private static final String TABLE = "table";
  private static final String TABLE_2 = "table2";
  private static final String CONTENT_DOMAIN = "contentDomain";
  private static final String CONTENT_DOMAIN_2 = "contentDomain2";
  public static final long ID = 42;
  private static final long ID_2 = 2;
  private static final String APP_VERSION_STRING = "5.7";

  @Mock private NetworkClient networkClient;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private SchedulerApi scheduler;
  @Mock private ThreadUtils threadUtils;
  @Mock private ActionReader actionReader;
  @Mock private Consumer<Result<List<StreamDataOperation>>> consumer;
  @Mock private Clock clock;
  @Mock private ApplicationInfo applicationInfo;
  private Context context;
  @Captor private ArgumentCaptor<Consumer<HttpResponse>> responseConsumerCaptor;
  @Captor private ArgumentCaptor<HttpRequest> requestCaptor;
  @Captor private ArgumentCaptor<Response> responseCaptor;
  @Captor private ArgumentCaptor<Result<List<StreamDataOperation>>> consumerCapture;
  private ExtensionRegistryLite registry;
  private FeedRequestManager requestManager;
  private final TimingUtils timingUtils = new TimingUtils();

  private final Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    context.getResources().getConfiguration().locale = Locale.US;
    FeedExtensionRegistry feedExtensionRegistry = new FeedExtensionRegistry(ArrayList::new);
    registry = ExtensionRegistryLite.newInstance();
    registry.add(FeedRequest.feedRequest);
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", VERSION_CODES.KITKAT);
    ReflectionHelpers.setStaticField(Build.VERSION.class, "RELEASE", "4.4.3");
    ReflectionHelpers.setStaticField(Build.class, "CPU_ABI", "armeabi");
    ReflectionHelpers.setStaticField(Build.class, "TAGS", "dev-keys");
    when(applicationInfo.getAppType()).thenReturn(ApplicationInfo.AppType.CHROME);
    when(applicationInfo.getArchitecture()).thenReturn(ApplicationInfo.Architecture.ARM);
    when(applicationInfo.getBuildType()).thenReturn(ApplicationInfo.BuildType.DEV);
    when(applicationInfo.getVersionString()).thenReturn(APP_VERSION_STRING);
    requestManager =
        new FeedRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            feedExtensionRegistry,
            scheduler,
            getTaskQueue(),
            timingUtils,
            threadUtils,
            actionReader,
            context,
            applicationInfo);

    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Collections.emptyList()));
  }

  @Test
  public void testTriggerRefresh() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);
    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testTriggerRefresh_FeedUiCapabilityAddedWhenFlagIsOn() throws Exception {
    testCapabilityAdded(ConfigKey.FEED_UI_ENABLED, Capability.FEED_UI);
  }

  @Test
  public void testTriggerRefresh_interesteHeaderCapabilityAddedWhenFlagIsOn() throws Exception {
    testCapabilityAdded(ConfigKey.INTEREST_HEADER_UI_ENABLED, Capability.INTEREST_HEADER);
  }

  @Test
  public void testTriggerRefresh_undoableActionCapabilityAddedWhenFlagIsOn() throws Exception {
    testCapabilityAdded(ConfigKey.UNDOABLE_ACTIONS_ENABLED, Capability.UNDOABLE_ACTIONS);
  }

  @Test
  public void testTriggerRefresh_manageInterestsCapabilityAddedWhenFlagIsOn() throws Exception {
    testCapabilityAdded(ConfigKey.MANAGE_INTERESTS_ENABLED, Capability.MANAGE_INTERESTS);
  }

  @Test
  public void testTriggerRefresh_tooltipCapabilityAddedWhenFlagIsOn() throws Exception {
    testCapabilityAdded(ConfigKey.CARD_MENU_TOOLTIP_ELIGIBLE, Capability.CARD_MENU_TOOLTIP);
  }

  @Test
  public void testTriggerRefresh_useSecondaryPageRequestAdded() throws Exception {
    testCapabilityAdded(
        ConfigKey.USE_SECONDARY_PAGE_REQUEST, Capability.USE_SECONDARY_PAGE_REQUEST);
  }

  @Test
  public void testTriggerRefresh_defaultConsumer() throws Exception {
    RequiredConsumer<Result<List<StreamDataOperation>>> consumer =
        new RequiredConsumer<>(input -> {});
    requestManager.setDefaultTriggerRefreshConsumerSupplier(() -> consumer);
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    when(protocolAdapter.createModel(any(Response.class)))
        .thenReturn(Result.success(new ArrayList<>()));

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);
    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);

    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));

    assertThat(consumer.isCalled()).isTrue();
  }

  @Test
  public void testActionData_simpleDismiss() throws Exception {
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, null);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Collections.singletonList(dismissAction)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Collections.singletonList(ID);
    List<String> expectedContentDomains = Collections.singletonList(CONTENT_DOMAIN);
    List<String> expectedTables = Collections.singletonList(TABLE);
    List<SemanticProperties> expectedSemanticProperties = Collections.emptyList();
    List<FeedActionQueryDataItem> expectedDataItems =
        Collections.singletonList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_uniqueDismisses() throws Exception {
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, null);
    DismissActionWithSemanticProperties dismissAction2 =
        buildDismissAction(ID_2, CONTENT_DOMAIN_2, TABLE_2, null);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Arrays.asList(dismissAction, dismissAction2)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Arrays.asList(ID, ID_2);
    List<String> expectedContentDomains = Arrays.asList(CONTENT_DOMAIN, CONTENT_DOMAIN_2);
    List<String> expectedTables = Arrays.asList(TABLE, TABLE_2);
    List<SemanticProperties> expectedSemanticProperties = Collections.emptyList();
    List<FeedActionQueryDataItem> expectedDataItems =
        Arrays.asList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .build(),
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(1)
                .setContentDomainIndex(1)
                .setTableIndex(1)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_overlappingDismisses() throws Exception {
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, null);
    DismissActionWithSemanticProperties dismissAction2 =
        buildDismissAction(ID_2, CONTENT_DOMAIN, TABLE, null);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Arrays.asList(dismissAction, dismissAction2)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Arrays.asList(ID, ID_2);
    List<String> expectedContentDomains = Collections.singletonList(CONTENT_DOMAIN);
    List<String> expectedTables = Collections.singletonList(TABLE);
    List<SemanticProperties> expectedSemanticProperties = Collections.emptyList();
    List<FeedActionQueryDataItem> expectedDataItems =
        Arrays.asList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .build(),
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(1)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_simpleDismissWithSemanticProperties() throws Exception {
    byte[] semanticPropertiesBytes = {42, 17, 88};
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, semanticPropertiesBytes);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Collections.singletonList(dismissAction)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Collections.singletonList(ID);
    List<String> expectedContentDomains = Collections.singletonList(CONTENT_DOMAIN);
    List<String> expectedTables = Collections.singletonList(TABLE);
    List<SemanticProperties> expectedSemanticProperties =
        Collections.singletonList(
            SemanticProperties.newBuilder()
                .setSemanticPropertiesData(ByteString.copyFrom(semanticPropertiesBytes))
                .build());
    List<FeedActionQueryDataItem> expectedDataItems =
        Collections.singletonList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .setSemanticPropertiesIndex(0)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_uniqueDismissesWithSemanticProperties() throws Exception {
    byte[] semanticPropertiesBytes = {42, 17, 88};
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, semanticPropertiesBytes);
    byte[] semanticPropertiesBytes2 = {7, 43, 91};
    DismissActionWithSemanticProperties dismissAction2 =
        buildDismissAction(ID_2, CONTENT_DOMAIN_2, TABLE_2, semanticPropertiesBytes2);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Arrays.asList(dismissAction, dismissAction2)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Arrays.asList(ID, ID_2);
    List<String> expectedContentDomains = Arrays.asList(CONTENT_DOMAIN, CONTENT_DOMAIN_2);
    List<String> expectedTables = Arrays.asList(TABLE, TABLE_2);
    List<SemanticProperties> expectedSemanticProperties =
        Arrays.asList(
            SemanticProperties.newBuilder()
                .setSemanticPropertiesData(ByteString.copyFrom(semanticPropertiesBytes))
                .build(),
            SemanticProperties.newBuilder()
                .setSemanticPropertiesData(ByteString.copyFrom(semanticPropertiesBytes2))
                .build());
    List<FeedActionQueryDataItem> expectedDataItems =
        Arrays.asList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .setSemanticPropertiesIndex(0)
                .build(),
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(1)
                .setContentDomainIndex(1)
                .setTableIndex(1)
                .setSemanticPropertiesIndex(1)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_overlappingDismissesWithSemanticProperties() throws Exception {
    byte[] semanticPropertiesBytes = {42, 17, 88};
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, semanticPropertiesBytes);
    DismissActionWithSemanticProperties dismissAction2 =
        buildDismissAction(ID_2, CONTENT_DOMAIN, TABLE_2, semanticPropertiesBytes);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Arrays.asList(dismissAction, dismissAction2)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Arrays.asList(ID, ID_2);
    List<String> expectedContentDomains = Collections.singletonList(CONTENT_DOMAIN);
    List<String> expectedTables = Arrays.asList(TABLE, TABLE_2);
    List<SemanticProperties> expectedSemanticProperties =
        Collections.singletonList(
            SemanticProperties.newBuilder()
                .setSemanticPropertiesData(ByteString.copyFrom(semanticPropertiesBytes))
                .build());
    List<FeedActionQueryDataItem> expectedDataItems =
        Arrays.asList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .setSemanticPropertiesIndex(0)
                .build(),
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(1)
                .setContentDomainIndex(0)
                .setTableIndex(1)
                .setSemanticPropertiesIndex(0)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testActionData_someDismissesWithSemanticProperties() throws Exception {
    DismissActionWithSemanticProperties dismissAction =
        buildDismissAction(ID, CONTENT_DOMAIN, TABLE, null);
    byte[] semanticPropertiesBytes = {42, 17, 88};
    DismissActionWithSemanticProperties dismissAction2 =
        buildDismissAction(ID_2, CONTENT_DOMAIN_2, TABLE_2, semanticPropertiesBytes);
    when(actionReader.getDismissActionsWithSemanticProperties())
        .thenReturn(Result.success(Arrays.asList(dismissAction, dismissAction2)));
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);

    List<Long> expectedIds = Arrays.asList(ID, ID_2);
    List<String> expectedContentDomains = Arrays.asList(CONTENT_DOMAIN, CONTENT_DOMAIN_2);
    List<String> expectedTables = Arrays.asList(TABLE, TABLE_2);
    List<SemanticProperties> expectedSemanticProperties =
        Collections.singletonList(
            SemanticProperties.newBuilder()
                .setSemanticPropertiesData(ByteString.copyFrom(semanticPropertiesBytes))
                .build());
    List<FeedActionQueryDataItem> expectedDataItems =
        Arrays.asList(
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(0)
                .setContentDomainIndex(0)
                .setTableIndex(0)
                .build(),
            FeedActionQueryDataItem.newBuilder()
                .setIdIndex(1)
                .setContentDomainIndex(1)
                .setTableIndex(1)
                .setSemanticPropertiesIndex(0)
                .build());

    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addFeedActionQueryData(
                        FeedActionQueryData.newBuilder()
                            .setAction(Action.newBuilder().setActionType(ActionType.DISMISS))
                            .addAllUniqueId(expectedIds)
                            .addAllUniqueContentDomain(expectedContentDomains)
                            .addAllUniqueTable(expectedTables)
                            .addAllUniqueSemanticProperties(expectedSemanticProperties)
                            .addAllFeedActionQueryDataItem(expectedDataItems))
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  @Test
  public void testHandleResponse() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, consumer);
    when(protocolAdapter.createModel(any(Response.class)))
        .thenReturn(Result.success(new ArrayList<>()));

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response = Response.getDefaultInstance();
    responseConsumerCaptor.getValue().accept(createHttpResponse(/* responseCode= */ 200, response));

    verify(protocolAdapter).createModel(responseCaptor.capture());
    assertThat(responseCaptor.getValue()).isEqualTo(response);

    verify(consumer).accept(consumerCapture.capture());
    List<Result<List<StreamDataOperation>>> consumerCaptures = consumerCapture.getAllValues();
    assertThat(consumerCaptures).hasSize(1);
    Result<List<StreamDataOperation>> result = consumerCaptures.get(0);
    assertThat(result.isSuccessful()).isTrue();
  }

  @Test
  public void testHandleResponse_notFound() throws Exception {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response = Response.getDefaultInstance();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ NOT_FOUND, response));

    verify(scheduler).onRequestError(NOT_FOUND);
    verify(consumer).accept(consumerCapture.capture());
    List<Result<List<StreamDataOperation>>> consumerCaptures = consumerCapture.getAllValues();
    assertThat(consumerCaptures).hasSize(1);
    Result<List<StreamDataOperation>> result = consumerCaptures.get(0);
    assertThat(result.isSuccessful()).isFalse();
  }

  @Test
  public void testHandleResponse_pageNotFound() throws Exception {
    StreamToken token =
        StreamToken.newBuilder()
            .setNextPageToken(ByteString.copyFrom("abc", Charset.defaultCharset()))
            .build();
    requestManager.loadMore(token, consumer);

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    Response response = Response.getDefaultInstance();
    responseConsumerCaptor
        .getValue()
        .accept(createHttpResponse(/* responseCode= */ NOT_FOUND, response));

    verify(scheduler, never()).onRequestError(NOT_FOUND);
    verify(consumer).accept(consumerCapture.capture());
    List<Result<List<StreamDataOperation>>> consumerCaptures = consumerCapture.getAllValues();
    assertThat(consumerCaptures).hasSize(1);
    Result<List<StreamDataOperation>> result = consumerCaptures.get(0);
    assertThat(result.isSuccessful()).isFalse();
  }

  @Test
  public void testHandleResponse_missingLengthPrefixNotSupported() {
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());
    responseConsumerCaptor
        .getValue()
        .accept(
            new HttpResponse(/* responseCode= */ 200, Response.getDefaultInstance().toByteArray()));

    verify(protocolAdapter, never()).createModel(any());
  }

  @Test
  @Config(sdk = VERSION_CODES.LOLLIPOP)
  public void testClientInfo_postLollipop() throws Exception {
    context.getResources().getConfiguration().locale = Locale.UK;
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", VERSION_CODES.LOLLIPOP);
    ReflectionHelpers.setStaticField(Build.VERSION.class, "RELEASE", "7.1.2b4.1");
    ReflectionHelpers.setStaticField(Build.class, "SUPPORTED_ABIS", new String[] {"arm64-v8a"});
    ReflectionHelpers.setStaticField(Build.class, "CPU_ABI", "armeabi");
    ReflectionHelpers.setStaticField(Build.class, "TAGS", "release-keys");
    when(applicationInfo.getAppType()).thenReturn(ApplicationInfo.AppType.SEARCH_APP);
    when(applicationInfo.getArchitecture()).thenReturn(ApplicationInfo.Architecture.ARM64);
    when(applicationInfo.getBuildType()).thenReturn(ApplicationInfo.BuildType.RELEASE);
    when(applicationInfo.getVersionString()).thenReturn("1.2.3.4");

    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    when(protocolAdapter.createModel(any(Response.class)))
        .thenReturn(Result.success(new ArrayList<>()));

    HttpRequest httpRequest = requestCaptor.getValue();

    Request request = getRequestFromHttpRequest(httpRequest);
    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .setClientInfo(
                        ClientInfo.newBuilder()
                            .setPlatformType(PlatformType.ANDROID)
                            .setPlatformVersion(
                                Version.newBuilder()
                                    .setMajor(7)
                                    .setMinor(1)
                                    .setBuild(2)
                                    .setRevision(1)
                                    .setArchitecture(Architecture.ARM64)
                                    .setBuildType(BuildType.RELEASE)
                                    .setApiVersion(VERSION_CODES.LOLLIPOP)
                                    .build())
                            .setLocale(Locale.UK.toLanguageTag())
                            .setAppType(AppType.GSA)
                            .setAppVersion(
                                Version.newBuilder()
                                    .setMajor(1)
                                    .setMinor(2)
                                    .setBuild(3)
                                    .setRevision(4)
                                    .setArchitecture(Architecture.ARM64)
                                    .setBuildType(BuildType.RELEASE)
                                    .build())
                            .build())
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }

  private static void assertHttpRequestFormattedCorrectly(HttpRequest httpRequest) {
    assertThat(httpRequest.getBody()).hasLength(0);
    assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(httpRequest.getUri().getQueryParameter("fmt")).isEqualTo("bin");
    assertThat(httpRequest.getUri().getQueryParameter(FeedRequestManager.MOTHERSHIP_PARAM_PAYLOAD))
        .isNotNull();
    assertThat(httpRequest.getUri().getQueryParameter(FeedRequestManager.LOCALE_PARAM))
        .isEqualTo("en_US");
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

  private static DismissActionWithSemanticProperties buildDismissAction(
      long id, String contentDomain, String table, byte /*@Nullable*/ [] semanticProperties) {
    ContentId contentId =
        ContentId.newBuilder().setTable(table).setContentDomain(contentDomain).setId(id).build();
    return new DismissActionWithSemanticProperties(contentId, semanticProperties);
  }

  private TaskQueue getTaskQueue() {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    return taskQueue;
  }

  private Request getRequestFromHttpRequest(HttpRequest httpRequest) throws Exception {
    return Request.parseFrom(
        Base64.decode(
            httpRequest.getUri().getQueryParameter(FeedRequestManager.MOTHERSHIP_PARAM_PAYLOAD),
            Base64.URL_SAFE),
        registry);
  }

  private static FeedRequest.Builder getTestFeedRequestBuilder() {
    return FeedRequest.newBuilder()
        .setClientInfo(
            ClientInfo.newBuilder()
                .setPlatformType(PlatformType.ANDROID)
                .setPlatformVersion(
                    Version.newBuilder()
                        .setMajor(4)
                        .setMinor(4)
                        .setBuild(3)
                        .setArchitecture(Architecture.ARM)
                        .setBuildType(BuildType.DEV)
                        .setApiVersion(VERSION_CODES.KITKAT)
                        .build())
                .setLocale(Locale.US.toLanguageTag())
                .setAppType(AppType.CHROME)
                .setAppVersion(
                    Version.newBuilder()
                        .setMajor(5)
                        .setMinor(7)
                        .setArchitecture(Architecture.ARM)
                        .setBuildType(BuildType.DEV)
                        .build())
                .build());
  }

  private void testCapabilityAdded(String configKey, Capability capability) throws Exception {
    Configuration configuration = new Configuration.Builder().put(configKey, true).build();
    requestManager =
        new FeedRequestManager(
            configuration,
            networkClient,
            protocolAdapter,
            new FeedExtensionRegistry(ArrayList::new),
            scheduler,
            getTaskQueue(),
            timingUtils,
            threadUtils,
            actionReader,
            context,
            applicationInfo);
    requestManager.triggerRefresh(RequestReason.MANUAL_REFRESH, input -> {});

    verify(networkClient).send(requestCaptor.capture(), responseConsumerCaptor.capture());

    HttpRequest httpRequest = requestCaptor.getValue();
    assertHttpRequestFormattedCorrectly(httpRequest);

    Request request = getRequestFromHttpRequest(httpRequest);
    Request expectedRequest =
        Request.newBuilder()
            .setRequestVersion(RequestVersion.FEED_QUERY)
            .setExtension(
                FeedRequest.feedRequest,
                getTestFeedRequestBuilder()
                    .setFeedQuery(FeedQuery.getDefaultInstance())
                    .addClientCapability(capability)
                    .addClientCapability(Capability.BASE_UI)
                    .build())
            .build();
    assertThat(request).isEqualTo(expectedRequest);
  }
}
