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

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelMutator;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults.Status;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests of the {@link SessionImpl} class. */
@RunWith(RobolectricTestRunner.class)
public class SessionImplTest {
  private static final StreamSession HEAD =
      StreamSession.newBuilder().setSessionId("$HEAD").build();
  private static final StreamSession TEST_SESSION =
      StreamSession.newBuilder().setSessionId("TEST$1").build();

  @Mock private Store store;
  @Mock private SessionMutationResults sessionMutationResults;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private ModelProvider modelProvider;
  @Mock private ThreadUtils threadUtils;

  private final ContentIdGenerators contentIdGenerators = new ContentIdGenerators();
  private final InternalProtocolUtils protocolUtils = new InternalProtocolUtils();

  private FakeSessionMutation fakeSessionMutation;
  private FakeModelMutator fakeModelMutator;
  private final TimingUtils timingUtils = new TimingUtils();

  @Before
  public void setUp() {
    initMocks(this);
    when(store.getHeadSession()).thenReturn(HEAD);
    fakeSessionMutation = new FakeSessionMutation();
    when(store.editSession(TEST_SESSION)).thenReturn(fakeSessionMutation);
    when(modelProviderFactory.create()).thenReturn(modelProvider);
    fakeModelMutator = new FakeModelMutator();
    when(modelProvider.edit()).thenReturn(fakeModelMutator);
  }

  @Test
  public void testMinimalSessionManager() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    verify(modelProviderFactory).create();
    assertThat(session.getModelProvider()).isEqualTo(modelProvider);
  }

  @Test
  public void testPopulateModelProvider_notFullyInitialized() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                session.populateModelProvider(new ArrayList<>());
              }
            })
        .throwsAnExceptionOfType(NullPointerException.class);
  }

  @Test
  public void testPopulateModelProvider_features() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // 3 features
    assertThat(streamStructures).hasSize(featureCnt);
    session.populateModelProvider(streamStructures);
    assertThat(fakeModelMutator.addedChildren).hasSize(featureCnt);
    assertThat(fakeModelMutator.commitCalled).isTrue();
    assertThat(session.contentInSession).hasSize(featureCnt);
    assertThat(
            session.contentInSession.contains(
                protocolUtils.getContentIdKey(contentIdGenerators.createFeatureStreamContentId(1))))
        .isTrue();
  }

  @Test
  public void testUpdateSession_notFullyInitialized() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                session.updateSession(new ArrayList<>(), null);
              }
            })
        .throwsAnExceptionOfType(NullPointerException.class);
  }

  @Test
  public void testUpdateSession_features() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // 1 clear, 3 features
    assertThat(streamStructures).hasSize(4);
    session.updateSession(streamStructures, null);
    assertThat(fakeSessionMutation.streamStructures).hasSize(featureCnt);
    assertThat(fakeModelMutator.addedChildren).hasSize(featureCnt);
    assertThat(session.contentInSession).hasSize(featureCnt);
    assertThat(
            session.contentInSession.contains(
                protocolUtils.getContentIdKey(contentIdGenerators.createFeatureStreamContentId(1))))
        .isTrue();
  }

  @Test
  public void testUpdateFromToken() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    StreamToken token =
        StreamToken.newBuilder().setContentId(contentIdGenerators.createTokenContentId(2)).build();
    // The token needs to be in the session
    session.contentInSession.add(protocolUtils.getContentIdKey(token.getContentId()));
    session.updateSession(streamStructures, token);
    assertThat(fakeModelMutator.token).isEqualTo(token);
  }

  @Test
  public void testUpdateFromToken_notInSession() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // The token is not in the session, so we ignore the update
    assertThat(session.contentInSession).hasSize(0);
    StreamToken token =
        StreamToken.newBuilder().setContentId(contentIdGenerators.createTokenContentId(2)).build();
    session.updateSession(streamStructures, token);
    assertThat(session.contentInSession).hasSize(0);
  }

  @Test
  public void testStorageFailure() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    when(sessionMutationResults.getStatus()).thenReturn(Status.FAILURE);
    session.updateSession(streamStructures, null);
    assertThat(fakeModelMutator.commitCalled).isFalse();
  }

  @Test
  public void testRemove() {
    SessionImpl session =
        new SessionImpl(modelProviderFactory, store, timingUtils, protocolUtils, threadUtils);
    session.setStreamSession(TEST_SESSION);
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    int featureCnt = 2;
    addFeatures(protocolBuilder, featureCnt, 1);
    protocolBuilder.removeFeature(
        contentIdGenerators.createFeatureStreamContentId(1),
        InternalProtocolBuilder.ROOT_CONTENT_ID);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();
    session.updateSession(streamStructures, null);
    assertThat(fakeModelMutator.removedChildren).hasSize(1);
    assertThat(session.contentInSession).hasSize(1);
  }

  private void addFeatures(InternalProtocolBuilder protocolBuilder, int featureCnt, int startId) {
    for (int i = 0; i < featureCnt; i++) {
      protocolBuilder.addFeature(
          contentIdGenerators.createFeatureStreamContentId(startId++),
          InternalProtocolBuilder.ROOT_CONTENT_ID);
    }
  }

  private static class FakeModelMutator implements ModelMutator {
    final List<StreamStructure> addedChildren = new ArrayList<>();
    final List<StreamStructure> removedChildren = new ArrayList<>();
    final List<StreamStructure> updateChildren = new ArrayList<>();
    StreamToken token;
    boolean commitCalled = false;

    @Override
    public ModelMutator addChild(StreamStructure streamStructure) {
      addedChildren.add(streamStructure);
      return this;
    }

    @Override
    public ModelMutator removeChild(StreamStructure streamStructure) {
      removedChildren.add(streamStructure);
      return this;
    }

    @Override
    public ModelMutator updateChild(StreamStructure streamStructure) {
      updateChildren.add(streamStructure);
      return this;
    }

    @Override
    public ModelMutator setMutationSourceToken(StreamToken mutationSourceToken) {
      this.token = mutationSourceToken;
      return this;
    }

    @Override
    public void commit() {
      commitCalled = true;
    }
  }

  private class FakeSessionMutation implements SessionMutation {

    private final List<StreamStructure> streamStructures = new ArrayList<>();

    @Override
    public SessionMutation add(StreamStructure dataOperation) {
      streamStructures.add(dataOperation);
      return this;
    }

    @Override
    public SessionMutationResults commit() {
      return sessionMutationResults;
    }
  }
}
