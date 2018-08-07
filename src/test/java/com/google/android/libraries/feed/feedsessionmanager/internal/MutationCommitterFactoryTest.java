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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.util.Pair;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelError.ErrorType;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.MutationCommitterFactory.MutationCommitter;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests of the {@link MutationCommitterFactory} and the actual committer {@link
 * MutationCommitterFactory.MutationCommitter}.
 */
@RunWith(RobolectricTestRunner.class)
public class MutationCommitterFactoryTest {
  @Mock private ContentCache contentCache;
  @Mock private ThreadUtils threadUtils;
  @Mock private SchedulerApi schedulerApi;
  @Mock private Clock clock;

  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorageDirect contentStorage = new InMemoryContentStorage();
  private final TimingUtils timingUtils = new TimingUtils();
  private final Configuration configuration = new Configuration.Builder().build();
  private final Map<String, StreamSharedState> sharedStateCache = new HashMap<>();
  private final ContentIdGenerators idGenerators = new ContentIdGenerators();

  private final String rootContentId = idGenerators.createRootContentId(0);

  private Store storeSpy;
  private TaskQueue taskQueue;
  private SessionCache sessionCache;
  private Session notifySession;
  private ModelError notifyError;
  private StreamSession resetSession;

  @Before
  public void setUp() {
    initMocks(this);
    storeSpy = null;
    sharedStateCache.clear();
    notifySession = null;
    resetSession = null;
    taskQueue = new TaskQueue(MoreExecutors.newDirectExecutorService(), clock, false);
    taskQueue.initialize(() -> {});
    Store store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            new InMemoryJournalStorage(),
            threadUtils,
            taskQueue,
            clock);
    storeSpy = spy(store);
    SessionFactory sessionFactory =
        new SessionFactory(storeSpy, taskQueue, timingUtils, threadUtils, configuration);
    sessionCache =
        new SessionCache(storeSpy, taskQueue, sessionFactory, 10L, timingUtils, threadUtils, clock);
    sessionCache.initialize();
  }

  @Test
  public void testResultError() {
    String streamToken = "session:1";
    Session session = getSession(streamToken);

    MutationContext mutationContext =
        new MutationContext.Builder()
            .setContinuationToken(StreamToken.getDefaultInstance())
            .setRequestingSession(session.getStreamSession())
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
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    dataOperations.add(
        getStreamDataOperation(
            StreamStructure.newBuilder().setOperation(Operation.CLEAR_ALL).build(), null));
    String streamToken = "session:1";
    Session session = getSession(streamToken);

    MutationCommitter mutationCommitter =
        getMutationCommitter(
            new MutationContext.Builder().setRequestingSession(session.getStreamSession()).build());
    mutationCommitter.accept(Result.success(dataOperations));
    assertThat(mutationCommitter.clearedHead).isTrue();
    assertThat(resetSession).isEqualTo(session.getStreamSession());
  }

  @Test
  public void testCompletion() {
    @SuppressWarnings("unchecked")
    Consumer<Boolean> completion = mock(Consumer.class);
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

    MutationCommitter mutationCommitter =
        getMutationCommitter(
            new MutationContext.Builder().setMutationCompleteConsumer(completion).build());
    mutationCommitter.accept(Result.success(dataOperations));
    verify(completion).accept(true);
  }

  @Test
  public void testUpdateContent() {
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
  public void testValidDataOperation() {
    MutationCommitter mutationCommitter = getMutationCommitter(EMPTY_CONTEXT);

    StreamDataOperation operation = StreamDataOperation.getDefaultInstance();
    assertThat(mutationCommitter.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .build();
    assertThat(mutationCommitter.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamStructure(StreamStructure.getDefaultInstance())
            .build();
    assertThat(mutationCommitter.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .setStreamStructure(StreamStructure.getDefaultInstance())
            .build();
    assertThat(mutationCommitter.validDataOperation(operation)).isFalse();

    operation =
        StreamDataOperation.newBuilder()
            .setStreamPayload(StreamPayload.getDefaultInstance())
            .setStreamStructure(StreamStructure.newBuilder().setContentId("content").build())
            .build();
    assertThat(mutationCommitter.validDataOperation(operation)).isTrue();
  }

  private MutationCommitter getMutationCommitter(MutationContext mutationContext) {
    MutationCommitterFactory committerFactory =
        new MutationCommitterFactory(
            storeSpy,
            sessionCache,
            contentCache,
            taskQueue,
            schedulerApi,
            threadUtils,
            timingUtils,
            clock);
    return (MutationCommitter)
        committerFactory.create(
            "task", mutationContext, this::resetHead, this::notifySessionError, sharedStateCache);
  }

  private List<String> getContentIds(int count) {
    List<String> contentIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      contentIds.add(idGenerators.createFeatureContentId(i));
    }
    return contentIds;
  }

  private Session getSession(String token) {
    Session session = mock(Session.class);
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken(token).build();
    when(session.getStreamSession()).thenReturn(streamSession);
    sessionCache.put(token, session);
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

  private void resetHead(StreamSession streamSession) {
    this.resetSession = streamSession;
  }

  private void notifySessionError(Session session, ModelError error) {
    notifySession = session;
    notifyError = error;
  }
}
