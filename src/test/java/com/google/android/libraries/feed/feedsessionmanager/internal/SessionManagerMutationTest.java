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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import static com.google.android.libraries.feed.api.common.MutationContext.EMPTY_CONTEXT;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.util.Pair;
import com.google.android.libraries.feed.api.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.knowncontent.KnownContentApi.KnownContentListener;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelError.ErrorType;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.testing.FakeTaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeThreadUtils;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.feedsessionmanager.internal.SessionManagerMutation.MutationCommitter;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.feedstore.testing.DelegatingStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.testing.host.logging.FakeBasicLoggingApi;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.OpaqueActionDataForTestProto.OpaqueActionDataForTest;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests of the {@link SessionManagerMutation} and the actual committer {@link
 * SessionManagerMutation.MutationCommitter}.
 */
@RunWith(RobolectricTestRunner.class)
public class SessionManagerMutationTest {
  private final Configuration configuration = new Configuration.Builder().build();
  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final ContentStorageDirect contentStorage = new InMemoryContentStorage();
  private final FakeBasicLoggingApi fakeBasicLoggingApi = new FakeBasicLoggingApi();
  private final FakeClock fakeClock = new FakeClock();
  private final FakeThreadUtils fakeThreadUtils = new FakeThreadUtils();
  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final String rootContentId = idGenerators.createRootContentId(0);
  private final TimingUtils timingUtils = new TimingUtils();

  @Mock private KnownContentListener knownContentListener;
  @Mock private SchedulerApi schedulerApi;
  private ContentCache contentCache;
  private FakeTaskQueue fakeTaskQueue;
  private ModelError notifyError;
  private Session notifySession;
  private SessionCache sessionCache;
  private Store storeSpy;

  @Before
  public void setUp() {
    initMocks(this);
    storeSpy = null;
    notifySession = null;
    fakeTaskQueue = new FakeTaskQueue(MoreExecutors.directExecutor(), fakeClock, fakeThreadUtils);
    fakeTaskQueue.initialize(() -> {});
    fakeThreadUtils.enforceMainThread(false);
    Store store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            new InMemoryJournalStorage(),
            fakeThreadUtils,
            fakeTaskQueue,
            fakeClock,
            fakeBasicLoggingApi,
            FakeMainThreadRunner.runTasksImmediately());
    storeSpy = spy(new DelegatingStore(store));
    SessionFactory sessionFactory =
        new SessionFactory(storeSpy, fakeTaskQueue, timingUtils, fakeThreadUtils, configuration);
    sessionCache =
        new SessionCache(
            storeSpy, fakeTaskQueue, sessionFactory, 10L, timingUtils, fakeThreadUtils, fakeClock);
    sessionCache.initialize();
    contentCache = new ContentCache();
  }

  @Test
  public void testResultError() {
    String sessionId = "session:1";
    Session session = getSession(sessionId);

    MutationContext mutationContext =
        new MutationContext.Builder()
            .setContinuationToken(StreamToken.getDefaultInstance())
            .setRequestingSessionId(sessionId)
            .build();
    MutationCommitter mutationCommitter = getMutationCommitter(mutationContext);
    mutationCommitter.accept(Result.failure());
    assertThat(notifySession).isEqualTo(session);
    assertThat(notifyError.getErrorType()).isEqualTo(ErrorType.PAGINATION_ERROR);
  }

  @Test
  public void testResultError_noSession() {
    MutationCommitter mutationCommitter = getMutationCommitter(MutationContext.EMPTY_CONTEXT);
    mutationCommitter.accept(Result.failure());
    assertThat(notifySession).isEqualTo(null);
    assertThat(notifyError.getErrorType()).isEqualTo(ErrorType.NO_CARDS_ERROR);
  }

  @Test
  public void testResetHead() {
    fakeClock.set(5L);
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    dataOperations.add(
        getStreamDataOperation(
            StreamStructure.newBuilder().setOperation(Operation.CLEAR_ALL).build(), null));
    String sessionId = "session:1";

    MutationCommitter mutationCommitter =
        getMutationCommitter(
            new MutationContext.Builder().setRequestingSessionId(sessionId).build());
    mutationCommitter.accept(Result.success(dataOperations));
    assertThat(mutationCommitter.clearedHead).isTrue();
    verify(schedulerApi).onReceiveNewContent(5L);
    verify(knownContentListener).onNewContentReceived(true, 5L);
  }

  @Test
  public void testUpdateContent() {
    fakeClock.set(8L);
    List<String> contentIds = getContentIds(3);
    List<Pair<StreamStructure, StreamPayload>> features =
        getFeatures(
            contentIds,
            () ->
                StreamPayload.newBuilder()
                    .setStreamFeature(StreamFeature.getDefaultInstance())
                    .build());
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    for (Pair<StreamStructure, StreamPayload> feature : features) {
      dataOperations.add(getStreamDataOperation(feature.first, feature.second));
    }
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    mutationCommitter.accept(Result.success(dataOperations));

    Result<List<PayloadWithId>> result = storeSpy.getPayloads(contentIds);
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getValue()).hasSize(contentIds.size());
    for (PayloadWithId payload : result.getValue()) {
      assertThat(contentIds).contains(payload.contentId);
    }
    assertThat(contentCache.size()).isEqualTo(0);
    verify(schedulerApi, never()).onReceiveNewContent(anyLong());
    verify(knownContentListener).onNewContentReceived(false, 8L);
  }

  @Test
  public void testSemanticData() {
    List<String> contentIds = getContentIds(2);
    List<Pair<StreamStructure, StreamPayload>> features =
        getFeatures(
            contentIds,
            () ->
                StreamPayload.newBuilder()
                    .setSemanticData(ByteString.copyFrom("foo", Charset.defaultCharset()))
                    .build());
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    for (Pair<StreamStructure, StreamPayload> feature : features) {
      dataOperations.add(getStreamDataOperation(feature.first, feature.second));
    }
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    mutationCommitter.accept(Result.success(dataOperations));

    Result<List<SemanticPropertiesWithId>> result = storeSpy.getSemanticProperties(contentIds);
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getValue()).hasSize(contentIds.size());
    for (SemanticPropertiesWithId payload : result.getValue()) {
      assertThat(contentIds).contains(payload.contentId);
    }
  }

  @Test
  public void testActionData() {
    List<String> contentIds = getContentIds(2);
    OpaqueActionData data =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId("id").build())
            .build();
    List<Pair<StreamStructure, StreamPayload>> features =
        getFeatures(contentIds, () -> StreamPayload.newBuilder().setActionData(data).build());
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    for (Pair<StreamStructure, StreamPayload> feature : features) {
      dataOperations.add(getStreamDataOperation(feature.first, feature.second));
    }
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    mutationCommitter.accept(Result.success(dataOperations));

    Result<List<ActionPropertiesWithId>> result = storeSpy.getActionProperties(contentIds);
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getValue()).hasSize(contentIds.size());
    for (ActionPropertiesWithId payload : result.getValue()) {
      assertThat(contentIds).contains(payload.contentId);
    }
  }

  @Test
  public void testValidDataOperation() {
    StreamDataOperation operation = StreamDataOperation.getDefaultInstance();
    assertThat(SessionManagerMutation.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .build();
    assertThat(SessionManagerMutation.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamStructure(StreamStructure.getDefaultInstance())
            .build();
    assertThat(SessionManagerMutation.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .setStreamStructure(StreamStructure.getDefaultInstance())
            .build();
    assertThat(SessionManagerMutation.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .setStreamStructure(StreamStructure.newBuilder().setContentId("content").build())
            .build();
    assertThat(SessionManagerMutation.validDataOperation(operation)).isTrue();
  }

  @Test
  public void testInvalidateHead() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    mutationCommitter.resetHead(null);
    verify(storeSpy).clearHead();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderInitializing() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.INITIALIZING);
    assertThat(mutationCommitter.shouldInvalidateSession(null, modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderInvalidated() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.INVALIDATED);
    assertThat(mutationCommitter.shouldInvalidateSession(null, modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_modelProviderReady() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    assertThat(mutationCommitter.shouldInvalidateSession(null, modelProvider)).isTrue();
  }

  @Test
  public void testShouldInvalidateSession_noModelProviderSession() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    assertThat(mutationCommitter.shouldInvalidateSession("session:2", modelProvider)).isTrue();
  }

  @Test
  public void testShouldInvalidateSession_differentSession() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    String sessionId = "session:1";
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    when(modelProvider.getSessionId()).thenReturn(sessionId);
    assertThat(mutationCommitter.shouldInvalidateSession("session:2", modelProvider)).isFalse();
  }

  @Test
  public void testShouldInvalidateSession_sameSession() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);
    String sessionId = "session:1";
    ModelProvider modelProvider = mock(ModelProvider.class);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    when(modelProvider.getSessionId()).thenReturn(sessionId);
    assertThat(mutationCommitter.shouldInvalidateSession(sessionId, modelProvider)).isTrue();
  }

  private MutationCommitter getMutationCommitter(MutationContext mutationContext) {
    SessionManagerMutation mutation =
        new SessionManagerMutation(
            storeSpy,
            sessionCache,
            contentCache,
            fakeTaskQueue,
            schedulerApi,
            fakeThreadUtils,
            timingUtils,
            fakeClock);
    return (MutationCommitter)
        mutation.createCommitter(
            "task", mutationContext, this::notifySessionError, knownContentListener);
  }

  private List<String> getContentIds(int count) {
    List<String> contentIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      contentIds.add(idGenerators.createFeatureContentId(i));
    }
    return contentIds;
  }

  private Session getSession(String sessionId) {
    Session session = mock(Session.class);
    when(session.getSessionId()).thenReturn(sessionId);
    sessionCache.putAttachedAndRetainMetadata(sessionId, session);
    return session;
  }

  private List<Pair<StreamStructure, StreamPayload>> getFeatures(
      List<String> contentIds, Supplier<StreamPayload> payloadConsumer) {
    List<Pair<StreamStructure, StreamPayload>> values = new ArrayList<>();
    for (String contentId : contentIds) {
      StreamStructure streamStructure =
          StreamStructure.newBuilder()
              .setOperation(Operation.UPDATE_OR_APPEND)
              .setParentContentId(rootContentId)
              .setContentId(contentId)
              .build();
      StreamPayload streamPayload = payloadConsumer.get();
      values.add(new Pair<>(streamStructure, streamPayload));
    }
    return values;
  }

  private StreamDataOperation getStreamDataOperation(
      StreamStructure streamStructure, StreamPayload payload) {
    StreamDataOperation.Builder builder =
        StreamDataOperation.newBuilder().setStreamStructure(streamStructure);
    if (payload != null) {
      builder.setStreamPayload(payload);
    }
    return builder.build();
  }

  private void notifySessionError(Session session, ModelError error) {
    notifySession = session;
    notifyError = error;
  }
}
