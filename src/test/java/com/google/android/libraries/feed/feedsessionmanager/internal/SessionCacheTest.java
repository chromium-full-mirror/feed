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
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.common.util.concurrent.MoreExecutors;
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
    Map<String, Session> sessions = sessionCache.getSessionsMapForTest();
    assertThat(sessions).isEmpty();
    assertThat(sessionCache.isHeadInitialized()).isFalse();
    assertThat(sessionCache.getHead()).isNull();
    sessionCache.initialize();

    // Initialization adds $HEAD
    sessions = sessionCache.getSessionsMapForTest();
    assertThat(sessionCache.isHeadInitialized()).isTrue();
    assertThat(sessions).hasSize(1);
    assertThat(sessionCache.getHead()).isNotNull();
  }

  @Test
  public void testPutGet() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.put(session.getStreamSession().getStreamToken(), session);

    Session ret = sessionCache.get(session.getStreamSession().getStreamToken());
    assertThat(ret).isEqualTo(session);
  }

  @Test
  public void testPut_persisted() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.put(session.getStreamSession().getStreamToken(), session);

    Session ret = sessionCache.get(session.getStreamSession().getStreamToken());
    assertThat(ret).isEqualTo(session);

    session = populateSession(2, 2);
    sessionCache.put(session.getStreamSession().getStreamToken(), session);

    StreamSessions sessions = sessionCache.getPersistedSessions();
    assertThat(sessions).isNotNull();
    List<StreamSession> streamSessionList = sessions.getStreamSessionsList();
    assertThat(streamSessionList).hasSize(3);
  }

  @Test
  public void testRemove() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session session = populateSession(1, 2);
    sessionCache.put(session.getStreamSession().getStreamToken(), session);

    StreamSessions sessions = sessionCache.getPersistedSessions();
    assertThat(sessions).isNotNull();
    List<StreamSession> streamSessionList = sessions.getStreamSessionsList();
    assertThat(streamSessionList).hasSize(2);

    String token = session.getStreamSession().getStreamToken();
    assertThat(sessionCache.remove(token)).isEqualTo(session);
    assertThat(sessionCache.get(token)).isNull();

    sessions = sessionCache.getPersistedSessions();
    assertThat(sessions).isNotNull();
    streamSessionList = sessions.getStreamSessionsList();
    assertThat(streamSessionList).hasSize(1);
  }

  @Test
  public void testGetSessions() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    Session s1 = populateSession(1, 2);
    sessionCache.put(s1.getStreamSession().getStreamToken(), s1);
    Session s2 = populateSession(2, 2);
    sessionCache.put(s2.getStreamSession().getStreamToken(), s2);

    List<Session> sessions = sessionCache.getSessions();
    assertThat(sessions).hasSize(3);
    assertThat(sessions).contains(sessionCache.get(s1.getStreamSession().getStreamToken()));
    assertThat(sessions).contains(sessionCache.get(s2.getStreamSession().getStreamToken()));
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
      sessionCache.put(session.getStreamSession().getStreamToken(), session);
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
    sessionCache.put(session.getStreamSession().getStreamToken(), session);

    Set<String> accessibleContent = sessionCache.accessibleContentSupplier.get();
    // Add one for the root feature
    assertThat(accessibleContent).hasSize(featureCnt + 1);
  }

  @Test
  public void testIsSessionAlive() {
    StreamSession session1 =
        StreamSession.newBuilder().setStreamToken("stream:1").setLastAccessed(0).build();
    StreamSession session2 =
        StreamSession.newBuilder()
            .setStreamToken("stream:2")
            .setLastAccessed(DEFAULT_LIFETIME_MS - 1)
            .build();
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);
    assertThat(sessionCache.isSessionAlive(session1)).isFalse();
    assertThat(sessionCache.isSessionAlive(session2)).isTrue();
  }

  @Test
  public void testGetPersistedSessions() {
    List<PayloadWithId> persistedSessions = new ArrayList<>();
    StreamSessions streamSessions = StreamSessions.getDefaultInstance();
    StreamPayload payload = StreamPayload.newBuilder().setStreamSessions(streamSessions).build();
    persistedSessions.add(new PayloadWithId(SessionCache.STREAM_SESSION_CONTENT_ID, payload));

    SessionCache sessionCache = getSessionCache();
    doReturn(Result.success(persistedSessions)).when(storeSpy).getPayloads(any());

    StreamSessions sessions = sessionCache.getPersistedSessions();
    assertThat(sessions).isEqualTo(streamSessions);
  }

  @Test
  public void testCleanupJournals() {
    List<StreamSession> storedSessions = new ArrayList<>();
    StreamSession session1 = StreamSession.newBuilder().setStreamToken("stream:1").build();
    StreamSession session2 = StreamSession.newBuilder().setStreamToken("stream:2").build();
    storedSessions.add(session1);
    storedSessions.add(session2);

    SessionCache sessionCache = getSessionCache();
    doReturn(Result.success(storedSessions)).when(storeSpy).getAllSessions();

    Session s2 = mock(Session.class);
    when(s2.getStreamSession()).thenReturn(session2);
    sessionCache.setSessionsForTest(s2);
    sessionCache.cleanupSessionJournals();
    verify(storeSpy).removeSession(session1);
    assertThat(sessionCache.getSessionsMapForTest().get(session2.getStreamToken())).isEqualTo(s2);
  }

  @Test
  public void testInitializePersistedSessions() {
    List<PayloadWithId> persistedSessions = new ArrayList<>();
    StreamSessions.Builder streamSessionsBuilder = StreamSessions.newBuilder();
    StreamSession s1 =
        StreamSession.newBuilder().setStreamToken("stream:1").setLastAccessed(0).build();
    streamSessionsBuilder.addStreamSessions(s1);
    StreamSession s2 =
        StreamSession.newBuilder()
            .setStreamToken("stream:2")
            .setLastAccessed(DEFAULT_LIFETIME_MS - 1)
            .build();
    streamSessionsBuilder.addStreamSessions(s2);
    StreamPayload payload =
        StreamPayload.newBuilder().setStreamSessions(streamSessionsBuilder.build()).build();
    persistedSessions.add(new PayloadWithId(SessionCache.STREAM_SESSION_CONTENT_ID, payload));

    when(clock.currentTimeMillis()).thenReturn(DEFAULT_LIFETIME_MS + 2);

    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();
    doReturn(Result.success(persistedSessions)).when(storeSpy).getPayloads(any());

    Map<String, Session> sessionMap = sessionCache.getSessionsMapForTest();
    // Sessions will include $HEAD
    HeadSessionImpl head = (HeadSessionImpl) sessionMap.get(Store.HEAD.getStreamToken());
    assertThat(head).isNotNull();
    sessionCache.initializePersistedSessions();
    sessionMap = sessionCache.getSessionsMapForTest();
    assertThat(sessionMap).hasSize(2);
    assertThat(sessionMap.get(s2.getStreamToken()).getStreamSession()).isEqualTo(s2);
    assertThat(sessionMap.get(Store.HEAD.getStreamToken())).isNotNull();
  }

  @Test
  public void testUpdatePersistedSessions() {
    SessionCache sessionCache = getSessionCache();
    sessionCache.initialize();

    // persist HEAD into the store
    sessionCache.updatePersistedSessions();
    StreamSessions streamSessions = sessionCache.getPersistedSessions();
    assertThat(streamSessions).isNotNull();
    List<StreamSession> streamSessionList = streamSessions.getStreamSessionsList();
    assertThat(streamSessionList).hasSize(1);

    // add additional sessions
    StreamSession session1 = StreamSession.newBuilder().setStreamToken("stream:1").build();
    StreamSession session2 = StreamSession.newBuilder().setStreamToken("stream:2").build();
    Session s1 = mock(Session.class);
    when(s1.getStreamSession()).thenReturn(session1);
    Session s2 = mock(Session.class);
    when(s2.getStreamSession()).thenReturn(session2);
    sessionCache.setSessionsForTest(s1, s2);

    sessionCache.updatePersistedSessions();
    streamSessions = sessionCache.getPersistedSessions();
    assertThat(streamSessions).isNotNull();
    streamSessionList = streamSessions.getStreamSessionsList();
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
    assertThat(sessionCache.getPersistedSessions()).isNull();
  }

  private SessionCache getSessionCache() {
    TaskQueue taskQueue = new TaskQueue(MoreExecutors.newDirectExecutorService(), clock, false);
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
    sessionFactory =
        new SessionFactory(storeSpy, taskQueue, timingUtils, threadUtils, configuration);
    return new SessionCache(
        storeSpy, taskQueue, sessionFactory, 10, timingUtils, threadUtils, clock);
  }

  private Session populateSession(int id, int featureCnt) {
    InitializableSession session = sessionFactory.getSession();
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken("stream:" + id).build();
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
    session.populateModelProvider(streamSession, head, true, false, null);
    return session;
  }
}
