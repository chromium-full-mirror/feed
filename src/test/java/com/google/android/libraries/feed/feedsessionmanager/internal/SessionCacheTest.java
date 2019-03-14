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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.feedstore.testing.DelegatingStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.SessionMetadata;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSessions;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link SessionCache} class. */
@RunWith(RobolectricTestRunner.class)
public class SessionCacheTest {
  private static final long DEFAULT_LIFETIME_MS = 10;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private ThreadUtils threadUtils;
  @Mock private Clock clock;

  private SessionFactory sessionFactory;
  private Store storeSpy;

  private final TimingUtils timingUtils = new TimingUtils();
  private final Configuration configuration = new Configuration.Builder().build();
  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorageDirect contentStorage = new InMemoryContentStorage();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testInitialization() {
    SessionCache sessionCache = getSessionCache();
    populateHead();
    Map<String, Session> sessions = sessionCache.getSessionsMapForTest();
    assertThat(sessions).isEmpty();
    assertThat(sessionCache.isHeadInitialized()).isFalse();
    assertThat(sessionCache.getHead()).isNotNull();
    assertThat(sessionCache.getHead().isHeadEmpty()).isTrue();
    sessionCache.initialize();

    // Initialization adds $HEAD
    sessions = sessionCache.getSessionsMapForTest();
    assertThat(sessionCache.isHeadInitialized()).isTrue();
    assertThat(sessions).hasSize(1);
    assertThat(sessionCache.getHead()).isNotNull();
    assertThat(sessionCache.getHead().isHeadEmpty()).isFalse();
  }

  @Test
  public void testPutGet() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.putAndRetainMetadata(session.getSessionId(), session);

    Session ret = sessionCache.get(session.getSessionId());
    assertThat(ret).isEqualTo(session);
  }

  @Test
  public void testPut_persisted() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.put(session.getSessionId(), /* creationTimeMillis= */ 0L, session);

    Session ret = sessionCache.get(session.getSessionId());
    assertThat(ret).isEqualTo(session);

    session = populateSession(2, 2);
    sessionCache.put(session.getSessionId(), /* creationTimeMillis= */ 0L, session);

    List<StreamSession> streamSessionList = sessionCache.getPersistedSessions();
    assertThat(streamSessionList).hasSize(3);
  }

  @Test
  public void testRemove() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.put(session.getSessionId(), /* creationTimeMillis= */ 0L, session);

    List<StreamSession> streamSessionList = sessionCache.getPersistedSessions();
    assertThat(streamSessionList).hasSize(2);

    String token = session.getSessionId();
    assertThat(sessionCache.remove(token)).isEqualTo(session);
    assertThat(sessionCache.get(token)).isNull();

    streamSessionList = sessionCache.getPersistedSessions();
    assertThat(streamSessionList).hasSize(1);
  }

  @Test
  public void testDetach() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session s1 = populateSession(1, 2);
    String s1Token = s1.getSessionId();
    sessionCache.putAndRetainMetadata(s1Token, s1);

    List<Session> sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(2);
    assertThat(sessions).contains(s1);
    assertThat(s1.getModelProvider()).isNotNull();

    sessionCache.detachModelProvider(s1Token);
    sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(2);
    assertThat(sessions).contains(s1);
    assertThat(s1.getModelProvider()).isNull();
  }

  @Test
  public void testGetSessions() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session s1 = populateSession(1, 2);
    sessionCache.putAndRetainMetadata(s1.getSessionId(), s1);
    Session s2 = populateSession(2, 2);
    sessionCache.putAndRetainMetadata(s2.getSessionId(), s2);

    List<Session> sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(3);
    assertThat(sessions).contains(sessionCache.get(s1.getSessionId()));
    assertThat(sessions).contains(sessionCache.get(s2.getSessionId()));
  }

  @Test
  public void testReset_headOnly() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    List<Session> sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(1);

    sessionCache.reset();
    sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(1);
  }

  @Test
  public void testReset_sessions() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    int sessionCount = 2;
    for (int i = 0; i < sessionCount; i++) {
      Session session = populateSession(i, 2);
      sessionCache.putAndRetainMetadata(session.getSessionId(), session);
    }
    List<Session> sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(sessionCount + 1);

    sessionCache.reset();
    sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(1);
  }

  @Test
  public void testAccessibleContentSupplier() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();

    int featureCnt = 3;
    Session session = populateSession(1, featureCnt);
    sessionCache.putAndRetainMetadata(session.getSessionId(), session);

    Set<String> accessibleContent = sessionCache.accessibleContentSupplier.get();
    // Add one for the root feature
    assertThat(accessibleContent).hasSize(featureCnt + 1);
  }

  @Test
  public void testIsSessionAlive() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);
    assertThat(sessionCache.isSessionAlive("stream:1", SessionMetadata.getDefaultInstance()))
        .isFalse();
    assertThat(
            sessionCache.isSessionAlive(
                Store.HEAD_SESSION_ID, SessionMetadata.getDefaultInstance()))
        .isTrue();
    assertThat(
            sessionCache.isSessionAlive(
                "stream:2",
                SessionMetadata.newBuilder()
                    .setCreationTimeMillis(DEFAULT_LIFETIME_MS - 1)
                    .build()))
        .isTrue();
  }

  @Test
  public void testGetPersistedSessions() {
    List<PayloadWithId> persistedSessions = new ArrayList<>();
    StreamSession streamSession = StreamSession.getDefaultInstance();
    StreamSessions streamSessions =
        StreamSessions.newBuilder().addStreamSession(streamSession).build();
    StreamPayload payload = StreamPayload.newBuilder().setStreamSessions(streamSessions).build();
    persistedSessions.add(new PayloadWithId(SessionCache.STREAM_SESSION_CONTENT_ID, payload));

    SessionCache sessionCache = getSessionCache();
    doReturn(Result.success(persistedSessions)).when(storeSpy).getPayloads(any());

    List<StreamSession> sessionList = sessionCache.getPersistedSessions();
    assertThat(sessionList).containsExactly(streamSession);
  }

  @Test
  public void testCleanupJournals() {
    List<String> storedSessions = new ArrayList<>();
    String sessionId1 = "stream:1";
    String sessionId2 = "stream:2";
    storedSessions.add(sessionId1);
    storedSessions.add(sessionId2);

    SessionCache sessionCache = getSessionCache();
    doReturn(Result.success(storedSessions)).when(storeSpy).getAllSessions();

    Session s2 = mock(Session.class);
    when(s2.getSessionId()).thenReturn(sessionId2);
    sessionCache.setSessionsForTest(s2);
    sessionCache.cleanupSessionJournals();
    verify(storeSpy).removeSession(sessionId1);
    assertThat(sessionCache.getSessionsMapForTest().get(sessionId2)).isEqualTo(s2);
  }

  @Test
  public void testInitializePersistedSessions_emptyStreamSessions() {
    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);

    SessionCache sessionCache = getSessionCache();
    mockStreamSessions(StreamSessions.getDefaultInstance());
    sessionCache.initialize();

    assertThat(sessionCache.get(Store.HEAD_SESSION_ID)).isNotNull();
  }

  @Test
  public void testInitializePersistedSessions_legacyStreamSession() {
    StreamSessions streamSessions =
        StreamSessions.newBuilder()
            .addStreamSession(
                StreamSession.newBuilder().setSessionId("stream:1").setLegacyTimeMillis(0))
            .addStreamSession(
                StreamSession.newBuilder()
                    .setSessionId("stream:2")
                    .setLegacyTimeMillis(DEFAULT_LIFETIME_MS - 1))
            .addStreamSession(
                StreamSession.newBuilder()
                    .setSessionId(Store.HEAD_SESSION_ID)
                    .setLegacyTimeMillis(1))
            .build();

    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);

    SessionCache sessionCache = getSessionCache();
    mockStreamSessions(streamSessions);
    sessionCache.initialize();

    assertThat(sessionCache.get(Store.HEAD_SESSION_ID)).isNotNull();
    assertThat(sessionCache.get("stream:1")).isNull();
    assertThat(sessionCache.get("stream:2")).isNotNull();
    assertThat(sessionCache.getCreationTimeMillis("stream:2")).isEqualTo(DEFAULT_LIFETIME_MS - 1);
    assertThat(sessionCache.getHeadLastAddedTimeMillis()).isEqualTo(1);
  }

  @Test
  public void testInitializePersistedSessions_sessionMetadata() {
    StreamSessions streamSessions =
        StreamSessions.newBuilder()
            .addStreamSession(
                StreamSession.newBuilder()
                    .setSessionId("stream:1")
                    .setSessionMetadata(SessionMetadata.getDefaultInstance()))
            .addStreamSession(
                StreamSession.newBuilder()
                    .setSessionId("stream:2")
                    .setSessionMetadata(
                        SessionMetadata.newBuilder()
                            .setCreationTimeMillis(DEFAULT_LIFETIME_MS - 1)))
            .build();

    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);

    SessionCache sessionCache = getSessionCache();
    mockStreamSessions(streamSessions);
    sessionCache.initialize();

    assertThat(sessionCache.get(Store.HEAD_SESSION_ID)).isNotNull();
    assertThat(sessionCache.get("stream:1")).isNull();
    assertThat(sessionCache.get("stream:2")).isNotNull();
    assertThat(sessionCache.getCreationTimeMillis("stream:2")).isEqualTo(DEFAULT_LIFETIME_MS - 1);
  }

  @Test
  public void testUpdatePersistedSessions() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();

    // persist HEAD into the store
    sessionCache.updatePersistedSessions();
    List<StreamSession> streamSessionList = sessionCache.getPersistedSessions();
    assertThat(streamSessionList).hasSize(1);

    // add additional sessions
    StreamSession session1 =
        StreamSession.newBuilder()
            .setSessionId("stream:1")
            .setSessionMetadata(SessionMetadata.newBuilder().setCreationTimeMillis(0L))
            .build();
    StreamSession session2 =
        StreamSession.newBuilder()
            .setSessionId("stream:2")
            .setSessionMetadata(SessionMetadata.newBuilder().setCreationTimeMillis(0L))
            .build();
    Session s1 = mock(Session.class);
    when(s1.getSessionId()).thenReturn(session1.getSessionId());
    Session s2 = mock(Session.class);
    when(s2.getSessionId()).thenReturn(session2.getSessionId());
    sessionCache.setSessionsForTest(s1, s2);

    sessionCache.updatePersistedSessions();
    streamSessionList = sessionCache.getPersistedSessions();
    assertThat(streamSessionList).hasSize(3);
    assertThat(streamSessionList).contains(session1);
    assertThat(streamSessionList).contains(session2);
  }

  @Test
  public void testErrors_persistedSession() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    sessionCache.updatePersistedSessions();

    doReturn(Result.failure()).when(storeSpy).getPayloads(any());
    assertThat(sessionCache.getPersistedSessions()).isEmpty();
  }

  private SessionCache getSessionCache() {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    Store store =
        new DelegatingStore(
            new FeedStore(
                timingUtils,
                extensionRegistry,
                contentStorage,
                new InMemoryJournalStorage(),
                threadUtils,
                taskQueue,
                clock,
                basicLoggingApi,
                FakeMainThreadRunner.runTasksImmediately()));
    storeSpy = spy(store);
    sessionFactory =
        new SessionFactory(storeSpy, taskQueue, timingUtils, threadUtils, configuration);
    return new SessionCache(
        storeSpy, taskQueue, sessionFactory, 10, timingUtils, threadUtils, clock);
  }

  private void mockStreamSessions(StreamSessions streamSessions) {
    List<PayloadWithId> persistedSessions = new ArrayList<>();
    persistedSessions.add(
        new PayloadWithId(
            SessionCache.STREAM_SESSION_CONTENT_ID,
            StreamPayload.newBuilder().setStreamSessions(streamSessions).build()));

    doReturn(Result.success(persistedSessions)).when(storeSpy).getPayloads(any());
  }

  private Session populateSession(int id, int featureCnt) {
    InitializableSession session = sessionFactory.getSession();
    String rootId = idGenerators.createRootContentId(1);
    List<StreamStructure> head = new ArrayList<>();
    head.add(
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(rootId)
            .build());
    for (int i = 0; i < featureCnt; i++) {
      head.add(
          StreamStructure.newBuilder()
              .setOperation(Operation.UPDATE_OR_APPEND)
              .setContentId(idGenerators.createFeatureContentId(i))
              .setParentContentId(rootId)
              .build());
    }
    session.setSessionId("stream:" + id);
    session.bindModelProvider(mock(ModelProvider.class), mock(ViewDepthProvider.class));
    session.populateModelProvider(head, true, false);
    return session;
  }

  private void populateHead() {
    SessionMutation sessionMutation = storeSpy.editSession(Store.HEAD_SESSION_ID);
    String rootId = idGenerators.createRootContentId(1);
    sessionMutation.add(
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(rootId)
            .build());
    sessionMutation.commit();
  }
}
