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

import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor.ProcessNextChild;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.api.sessionmanager.Session;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults.Status;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadSessionImpl;
import com.google.android.libraries.feed.feedsessionmanager.internal.ManagedSession;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import org.robolectric.RobolectricTestRunner;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link FeedSessionManager} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedSessionManagerTest {

  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final String rootContentId = idGenerators.createRootContentId(0);
  private static final StreamSession HEAD = new StreamSession("$HEAD");

  @Mock private UiRunnableHandler uiRunnableHandler;
  @Mock private ThreadUtils threadUtils;
  @Mock private ProtocolAdapter protocolAdapter;
  @Mock private RequestManager requestManager;

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
  private FeedModelProviderFactory modelProviderFactory;

  @Before
  public void setUp() {
    initMocks(this);
    modelProviderFactory =
        new FeedModelProviderFactory(threadUtils, timingUtils, uiRunnableHandler);
  }

  @Test
  public void testMinimalSessionManager() {
    FeedSessionManager sessionManager = getInitializedSessionManager();
    Session session = sessionManager.getNewSession();
    assertThat(session).isNotNull();
    assertThat(session.getModelProvider()).isNotNull();
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
    SessionMutationResults sessionMutationResult = mock(SessionMutationResults.class);
    when(sessionMutation.commit()).thenReturn(sessionMutationResult);
    when(sessionMutationResult.getStatus()).thenReturn(Status.SUCCESS);

    FeedSessionManager sessionManager =
        new FeedSessionManager(
            MoreExecutors.newDirectExecutorService(),
            store,
            modelProviderFactory,
            timingUtils,
            threadUtils);
    Map<String, StreamSharedState> sharedStateCache = sessionManager.getSharedStateCacheForTest();
    assertThat(sharedStateCache).hasSize(sharedStates.size());

    Map<String, ManagedSession> sessions = sessionManager.getSessionsMapForTest();
    ManagedSession head = sessions.get(HEAD.streamToken);
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

    Session session = sessionManager.getNewSession();
    assertThat(session).isNotNull();

    ModelProvider modelProvider = session.getModelProvider();
    assertThat(modelProvider).isNotNull();
    assertThat(modelProvider.getRootFeature()).isNotNull();
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
    sessionManager.handleToken(streamToken);
    verify(requestManager).loadMore(bytes);
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

  private int populateSession(
      FeedSessionManager sessionManager,
      int featureCnt,
      int idStart,
      boolean reset,
      @Nullable String sharedStateId) {
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
    FeedStore store = new FeedStore(timingUtils, extensionRegistry);
    FeedSessionManager sessionManager =
        new FeedSessionManager(
            MoreExecutors.newDirectExecutorService(),
            store,
            modelProviderFactory,
            timingUtils,
            threadUtils);
    sessionManager.setProtocolAdapter(protocolAdapter);
    sessionManager.setRequestManager(requestManager);
    modelProviderFactory.setSessionManager(sessionManager);
    return sessionManager;
  }

  private ProcessNextChild incrementChildCountProcessor() {
    return new ProcessNextChild() {
      @Override
      public void processNextChild(ModelChild child) {
        cursorCount++;
      }
    };
  }
}
