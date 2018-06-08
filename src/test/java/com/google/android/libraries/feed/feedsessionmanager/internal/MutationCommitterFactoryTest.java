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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.MutationCommitterFactory.MutationCommitter;
import com.google.android.libraries.feed.feedstore.EphemeralFeedStore;
import com.google.android.libraries.feed.feedstore.FeedStoreHelper;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
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

  private final MainThreadRunner mainThreadRunner = new MainThreadRunner();
  private final TimingUtils timingUtils = new TimingUtils();
  private final Configuration configuration = new Configuration.Builder().build();
  private final Map<String, StreamSharedState> sharedStateCache = new HashMap<>();

  private Store storeSpy;
  private SessionCache sessionCache;
  private Session notifySession;
  private StreamSession resetSession;

  @Before
  public void setUp() {
    initMocks(this);
    storeSpy = null;
    sharedStateCache.clear();
    notifySession = null;
    resetSession = null;
  }

  @Test
  public void testResultError() {
    MutationCommitterFactory committerFactory = getMutationCommitterFactory();

    String streamToken = "session:1";
    Session session = getSession(streamToken);

    MutationContext mutationContext =
        new MutationContext.Builder()
            .setContinuationToken(StreamToken.getDefaultInstance())
            .setRequestingSession(session.getStreamSession())
            .build();
    MutationCommitter mutationCommitter =
        (MutationCommitter) committerFactory.create("task", mutationContext);
    mutationCommitter.accept(Result.failure());
    assertThat(notifySession).isEqualTo(session);
  }

  @Test
  public void testResetHead() {
    MutationCommitterFactory committerFactory = getMutationCommitterFactory();
    List<StreamDataOperation> dataOperations = new ArrayList<>();
    dataOperations.add(
        getStreamDataOperation(
            StreamStructure.newBuilder().setOperation(Operation.CLEAR_ALL).build()));
    String streamToken = "session:1";
    Session session = getSession(streamToken);

    MutationCommitter mutationCommitter =
        (MutationCommitter)
            committerFactory.create(
                "task",
                new MutationContext.Builder()
                    .setRequestingSession(session.getStreamSession())
                    .build());
    mutationCommitter.accept(Result.success(dataOperations));
    assertThat(mutationCommitter.clearedHead).isTrue();
    assertThat(resetSession).isEqualTo(session.getStreamSession());
  }

  @Test
  public void testValidDataOperation() {
    MutationCommitterFactory committerFactory = getMutationCommitterFactory();
    MutationCommitter mutationCommitter =
        (MutationCommitter) committerFactory.create("task", MutationContext.EMPTY_CONTEXT);

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

  private Session getSession(String token) {
    Session session = mock(Session.class);
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken(token).build();
    when(session.getStreamSession()).thenReturn(streamSession);
    sessionCache.put(token, session);
    return session;
  }

  private StreamDataOperation getStreamDataOperation(StreamStructure streamStructure) {
    return StreamDataOperation.newBuilder().setStreamStructure(streamStructure).build();
  }

  private MutationCommitterFactory getMutationCommitterFactory() {
    EphemeralFeedStore store = new EphemeralFeedStore(clock, timingUtils, new FeedStoreHelper());
    storeSpy = spy(store);
    TaskQueue taskQueue = new TaskQueue(MoreExecutors.newDirectExecutorService());
    SessionFactory sessionFactory =
        new SessionFactory(storeSpy, taskQueue, timingUtils, threadUtils, configuration);
    sessionCache =
        new SessionCache(storeSpy, taskQueue, sessionFactory, 10L, timingUtils, threadUtils, clock);
    sessionCache.initialize();
    return new MutationCommitterFactory(
        this::resetHead,
        this::notifySessionError,
        storeSpy,
        sessionCache,
        contentCache,
        sharedStateCache,
        taskQueue,
        mainThreadRunner,
        schedulerApi,
        threadUtils,
        timingUtils,
        clock);
  }

  private void resetHead(StreamSession streamSession) {
    this.resetSession = streamSession;
  }

  private void notifySessionError(Session session) {
    notifySession = session;
  }
}
