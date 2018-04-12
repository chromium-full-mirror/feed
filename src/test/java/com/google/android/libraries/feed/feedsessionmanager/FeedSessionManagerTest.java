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

package com.google.android.libraries.feed.feedsessionmanager;

import static com.google.android.libraries.feed.api.sessionmanager.SessionManager.GLOBAL_SHARED_STATE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.Session;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FeedSessionManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedSessionManagerTest {

  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final String rootContentId = idGenerators.createRootContentId(0);
  private static final StreamSession HEAD =
      StreamSession.newBuilder().setStreamToken("$HEAD").build();

  @Mock private UiRunnableHandler uiRunnableHandler;
  @Mock private ThreadUtils threadUtils;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;
  @Captor private ArgumentCaptor<MutationContext> mutationContextCaptor;

  private int cursorCount = 0;
  private final ContentIdGenerators contentIdGenerators = new ContentIdGenerators();
  private final TimingUtils timingUtils = new TimingUtils();
  private final FeedExtensionRegistry extensionRegistry =
      new FeedExtensionRegistry(
          new ProtoExtensionProvider() {
            @Override
            public List<GeneratedExtension<?, ?>> getProtoExtensions() {
              return new ArrayList<>();
            }
          });
  private final Configuration configuration =
      new Configuration.Builder().put(ConfigKey.OPTIMISTIC_SESSION_WRITES, true).build();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testInitialization() {
    Store store = mock(Store.class);
    StreamSharedState sharedState =
        StreamSharedState.newBuilder()
            .setContentId(idGenerators.createFeatureContentId(0))
            .setPietSharedStateItem(PietSharedStateItem.getDefaultInstance())
            .build();
    List<StreamSharedState> sharedStates = new ArrayList<>();
    sharedStates.add(sharedState);
    when(store.getSharedStates()).thenReturn(sharedStates);
    when(store.getHeadSession()).thenReturn(HEAD);

    List<StreamStructure> streamStructures = new ArrayList<>();
    StreamStructure operation =
        StreamStructure.newBuilder()
            .setContentId(idGenerators.createFeatureContentId(0))
            .setOperation(StreamStructure.Operation.UPDATE_OR_APPEND)
            .build();
    streamStructures.add(operation);
    com.google.android.libraries.feed.api.store.SessionMutation sessionMutation =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.getStreamStructures(any(StreamSession.class))).thenReturn(streamStructures);
    when(store.editSession(HEAD)).thenReturn(sessionMutation);
    when(sessionMutation.commit()).thenReturn(true);

    FeedSessionManager sessionManager =
        new FeedSessionManager(
            MoreExecutors.newDirectExecutorService(),
            store,
            timingUtils,
            threadUtils,
            Suppliers.of(protocolAdapter),
            Suppliers.of(requestManager),
            configuration);
    Map<String, StreamSharedState> sharedStateCache = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStateCache).hasSize(sharedStates.size());

    Map<String, Session> sessions = sessionManager.getSessionsMapForTest();
    Session head = sessions.get(HEAD.getStreamToken());
    assertThat(head).isInstanceOf(HeadSessionImpl.class);
    String itemKey = idGenerators.createFeatureContentId(0);
    Set<String> content = ((HeadSessionImpl) head).getContentInSessionForTest();
    assertThat(content).contains(itemKey);
    assertThat(content).hasSize(streamStructures.size());
  }

  @Test
  public void testSessionWithContent() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    int featureCnt = 3;
    populateSession(sessionManager, featureCnt, 1, true, null);

    ModelProviderFactory modelProviderFactory =
        new FeedModelProviderFactory(sessionManager, threadUtils, timingUtils, uiRunnableHandler);
    ModelProvider modelProvider = modelProviderFactory.createNew();
    assertThat(modelProvider).isNotNull();

    ModelCursor cursor = modelProvider.getRootFeature().getCursor();
    cursorCount = 0;
    while (cursor.advanceCursor(incrementChildCountProcessor())) ;
    assertThat(cursorCount).isEqualTo(featureCnt);

    // append a couple of others
    populateSession(sessionManager, featureCnt, featureCnt + 1, false, null);

    cursor = modelProvider.getRootFeature().getCursor();
    cursorCount = 0;
    while (cursor.advanceCursor(incrementChildCountProcessor())) ;
    assertThat(cursorCount).isEqualTo(featureCnt * 2);
  }

  @Test
  public void testReset() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    int featureCnt = 3;
    int fullFeatureCount = populateSession(sessionManager, featureCnt, 1, true, null);
    assertThat(fullFeatureCount).isEqualTo(featureCnt + 1);

    fullFeatureCount = populateSession(sessionManager, featureCnt, 1, true, null);
    assertThat(fullFeatureCount).isEqualTo(featureCnt + 1);
  }

  @Test
  public void testHandleToken() {
    ByteString bytes = ByteString.copyFrom("continuation", Charset.defaultCharset());
    StreamToken streamToken =
        StreamToken.newBuilder().setNextPageToken(bytes).setParentId(rootContentId).build();
    FeedSessionManager sessionManager = getInitializedSessionManager();
    StreamSession streamSession = StreamSession.getDefaultInstance();
    sessionManager.handleToken(streamSession, streamToken);
    verify(requestManager).loadMore(mutationContextCaptor.capture());
    MutationContext mutationContext = mutationContextCaptor.getValue();
    assertThat(mutationContext.getContinuationToken()).isEqualTo(streamToken);
    assertThat(mutationContext.getRequestingSession()).isEqualTo(streamSession);
  }

  @Test
  public void testForceRefresh() {
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken("session:1").build();
    FeedSessionManager sessionManager = getInitializedSessionManager();
    sessionManager.forceRefresh(streamSession);
    verify(requestManager).triggerRefresh(RequestReason.MANUAL_REFRESH);
  }

  @Test
  public void testGetSharedState() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    String sharedStateId = idGenerators.createSharedStateContentId(0);

    populateSession(sessionManager, 3, 1, true, sharedStateId);
    when(protocolAdapter.getStreamContentId(GLOBAL_SHARED_STATE)).thenReturn(sharedStateId);
    assertThat(sessionManager.getSharedState(GLOBAL_SHARED_STATE)).isNotNull();

    // test the null condition
    ContentId undefinedSharedStateId =
        ContentId.newBuilder()
            .setContentDomain("shared-state")
            .setId(5)
            .setTable("shared-states")
            .build();
    String undefinedStreamSharedStateId =
        idGenerators.createSharedStateContentId(undefinedSharedStateId.getId());
    when(protocolAdapter.getStreamContentId(undefinedSharedStateId))
        .thenReturn(undefinedStreamSharedStateId);
    assertThat(sessionManager.getSharedState(undefinedSharedStateId)).isNull();
  }

  @Test
  public void testEdit_semanticProperties() {
    Store store = mock(Store.class);
    when(store.getHeadSession()).thenReturn(HEAD);
    com.google.android.libraries.feed.api.store.SessionMutation session =
        mock(com.google.android.libraries.feed.api.store.SessionMutation.class);
    when(store.editSession(any(StreamSession.class))).thenReturn(session);
    ContentMutation content = mock(ContentMutation.class);
    when(store.editContent()).thenReturn(content);

    SemanticPropertiesMutation semanticPropertiesMutation = mock(SemanticPropertiesMutation.class);
    when(store.editSemanticProperties()).thenReturn(semanticPropertiesMutation);

    FeedSessionManager sessionManager =
        new FeedSessionManager(
            MoreExecutors.newDirectExecutorService(),
            store,
            timingUtils,
            threadUtils,
            Suppliers.of(protocolAdapter),
            Suppliers.of(requestManager),
            configuration);

    ByteString semanticData = ByteString.copyFromUtf8("helloWorld");
    StreamDataOperation streamDataOperation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(
                StreamPayload.newBuilder()
                    .setStreamFeature(
                        StreamFeature.newBuilder()
                            .setSemanticData(semanticData)
                            .setContentId(rootContentId)))
            .setStreamStructure(
                StreamStructure.newBuilder()
                    .setContentId(rootContentId)
                    .setOperation(Operation.UPDATE_OR_APPEND))
            .build();

    SessionMutation sessionMutation = sessionManager.edit();
    sessionMutation.addOperation(streamDataOperation);
    sessionMutation.commit();

    verify(semanticPropertiesMutation).add(rootContentId, semanticData);
    verify(semanticPropertiesMutation).commit();
  }

  private int populateSession(
      FeedSessionManager sessionManager,
      int featureCnt,
      int idStart,
      boolean reset,
      /*@Nullable*/ String sharedStateId) {
    int operationCount = 0;

    InternalProtocolBuilder internalProtocolBuilder = new InternalProtocolBuilder();
    if (reset) {
      internalProtocolBuilder.addClearOperation().addRootFeature();
      operationCount++;
    }
    for (int i = 0; i < featureCnt; i++) {
      internalProtocolBuilder.addFeature(
          contentIdGenerators.createFeatureContentId(idStart++),
          idGenerators.createRootContentId(0));
      operationCount++;
    }
    if (sharedStateId != null) {
      internalProtocolBuilder.addSharedState(sharedStateId);
      operationCount++;
    }
    SessionMutation sessionMutation = sessionManager.edit();
    for (StreamDataOperation operation : internalProtocolBuilder.build()) {
      sessionMutation.addOperation(operation);
    }
    sessionMutation.commit();
    return operationCount;
  }

  private FeedSessionManager getInitializedSessionManager() {
    FeedStore store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            new InMemoryContentStorage(),
            new InMemoryJournalStorage(),
            threadUtils,
            new UiRunnableHandlerImpl());
    FeedSessionManager sessionManager =
        new FeedSessionManager(
            MoreExecutors.newDirectExecutorService(),
            store,
            timingUtils,
            threadUtils,
            Suppliers.of(protocolAdapter),
            Suppliers.of(requestManager),
            configuration);
    return sessionManager;
  }

  private Consumer<ModelChild> incrementChildCountProcessor() {
    return new Consumer<ModelChild>() {
      @Override
      public void accept(ModelChild child) {
        cursorCount++;
      }
    };
  }
}
