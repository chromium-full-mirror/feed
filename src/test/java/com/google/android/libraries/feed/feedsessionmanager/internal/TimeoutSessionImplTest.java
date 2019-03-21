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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.testing.AbstractSessionImplTest;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.feed.client.StreamDataProto.UiContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link TimeoutSessionImpl} class. */
@RunWith(RobolectricTestRunner.class)
public class TimeoutSessionImplTest extends AbstractSessionImplTest {
  private final TimingUtils timingUtils = new TimingUtils();

  @Mock private Clock clock;
  @Mock private ThreadUtils threadUtils;
  @Mock private ViewDepthProvider viewDepthProvider;

  @Before
  @Override
  public void setUp() {
    super.setUp();
  }

  @Test
  public void testInvalidateOnResetHead() {
    TimeoutSessionImpl session = getSessionImpl();
    assertThat(session.invalidateOnResetHead()).isFalse();
  }

  @Test
  public void testClearHead() {
    SessionImpl session = getSessionImpl();
    session.setSessionId(TEST_SESSION_ID);
    session.populateModelProvider(new ArrayList<>(), false, true);
    int featureCnt = 3;
    int featureId = 5;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, featureId);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // 1 clear, 3 features
    assertThat(streamStructures).hasSize(4);
    session.updateSession(true, streamStructures, null);
    assertThat(fakeSessionMutation.streamStructures).hasSize(featureCnt);
    assertThat(fakeModelMutator.addedChildren).hasSize(featureCnt);
    assertThat(session.getContentInSession()).hasSize(featureCnt);
    assertThat(session.getContentInSession())
        .contains(contentIdGenerators.createFeatureContentId(featureId));
  }

  @Test
  public void testClearHead_onPaginationRequest() {
    SessionImpl session = getSessionImpl();
    session.setSessionId(TEST_SESSION_ID);
    session.populateModelProvider(new ArrayList<>(), false, true);
    int featureCnt = 3;
    int featureId = 5;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, featureId);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    session.bindModelProvider(modelProvider, viewDepthProvider);
    session.populateModelProvider(new ArrayList<>(), false, false);
    session.updateSession(
        true,
        streamStructures,
        new MutationContext.Builder()
            .setRequestingSessionId(TEST_SESSION_ID)
            .setContinuationToken(StreamToken.newBuilder().setContentId("token").build())
            .build());
    verify(modelProvider).invalidate(UiContext.getDefaultInstance());
  }

  @Test
  public void testPopulateModelProviderState() {
    TimeoutSessionImpl session = getSessionImpl();
    assertThat(session.legacyHeadContent).isFalse();
    assertThat(session.viewDepthProvider).isNull();

    session.setSessionId(TEST_SESSION_ID);
    session.bindModelProvider(modelProvider, viewDepthProvider);
    session.populateModelProvider(new ArrayList<>(), false, true);
    assertThat(session.legacyHeadContent).isTrue();
    assertThat(session.viewDepthProvider).isEqualTo(viewDepthProvider);
  }

  @Test
  public void testCreateRemoveFeature() {
    TimeoutSessionImpl session = getSessionImpl();
    String contentId = "contentId";
    String parentContentId = "parentId";

    StreamStructure streamStructure = session.createRemoveFeature(contentId, parentContentId);
    assertThat(streamStructure.getContentId()).isEqualTo(contentId);
    assertThat(streamStructure.getParentContentId()).isEqualTo(parentContentId);
  }

  @Test
  public void testCreateRemoveFeature_nullParent() {
    TimeoutSessionImpl session = getSessionImpl();
    String contentId = "contentId";

    StreamStructure streamStructure = session.createRemoveFeature(contentId, null);
    assertThat(streamStructure.getContentId()).isEqualTo(contentId);
    assertThat(streamStructure.getParentContentId()).isEmpty();
  }

  @Test
  public void testCaptureRootContent() {
    TimeoutSessionImpl session = getSessionImpl();
    setupModelProviderRoot();

    List<ModelChild> children = session.captureRootContent();
    assertThat(children).isNotNull();
    assertThat(children).hasSize(1);
  }

  @Test
  public void testCaptureRootContent_nullRoot() {
    TimeoutSessionImpl session = getSessionImpl();

    List<ModelChild> children = session.captureRootContent();
    assertThat(children).isEmpty();
  }

  @Test
  public void testRemoveItems() {
    TimeoutSessionImpl session = getSessionImpl();
    List<ModelChild> modelChildren = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      modelChildren.add(createModelChild(i, ModelChild.Type.FEATURE));
    }
    setupForViewDepthProvider(
        session, contentIdGenerators.createFeatureContentId(6), modelChildren);

    // Items below the lowest child should be removed.
    session.updateSession(/* clearHead= */ true, new ArrayList<>(), /* mutationContext= */ null);
    assertThat(fakeModelMutator.removedChildren).hasSize(3);
    assertThat(fakeModelMutator.removedChildren.get(0).getContentId())
        .isEqualTo(contentIdGenerators.createFeatureContentId(7));
    assertThat(fakeModelMutator.removedChildren.get(1).getContentId())
        .isEqualTo(contentIdGenerators.createFeatureContentId(8));
    assertThat(fakeModelMutator.removedChildren.get(2).getContentId())
        .isEqualTo(contentIdGenerators.createFeatureContentId(9));
  }

  @Test
  public void testRemoveItems_visibleTokenIsRemoved() {
    TimeoutSessionImpl session = getSessionImpl();
    List<ModelChild> modelChildren = new ArrayList<>();
    modelChildren.add(createModelChild(0, ModelChild.Type.TOKEN));
    String tokenContentId = contentIdGenerators.createFeatureContentId(0);
    setupForViewDepthProvider(session, tokenContentId, modelChildren);

    // Because the lowest child is a token it should also be removed.
    session.updateSession(/* clearHead= */ true, new ArrayList<>(), /* mutationContext= */ null);
    assertThat(fakeModelMutator.removedChildren).hasSize(1);
    assertThat(fakeModelMutator.removedChildren.get(0).getContentId()).isEqualTo(tokenContentId);
  }

  @Test
  public void testRemoveItems_nullLowestChildShouldRemoveToken() {
    TimeoutSessionImpl session = getSessionImpl();
    List<ModelChild> modelChildren = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      modelChildren.add(createModelChild(i, ModelChild.Type.FEATURE));
    }
    modelChildren.add(createModelChild(11, ModelChild.Type.TOKEN));
    setupForViewDepthProvider(session, /* lowestChild= */ null, modelChildren);

    // The token should be removed even when there is a null lowest child.
    session.updateSession(/* clearHead= */ true, new ArrayList<>(), /* mutationContext= */ null);
    assertThat(fakeModelMutator.removedChildren).hasSize(1);
    assertThat(fakeModelMutator.removedChildren.get(0).getContentId())
        .isEqualTo(contentIdGenerators.createFeatureContentId(11));
  }

  private ModelChild createModelChild(long id, @ModelChild.Type int type) {
    ModelChild child = mock(ModelChild.class);
    when(child.getContentId()).thenReturn(contentIdGenerators.createFeatureContentId(id));
    when(child.getType()).thenReturn(type);
    return child;
  }

  private void setupModelProviderRoot() {
    List<ModelChild> cursorChildren = new ArrayList<>();
    cursorChildren.add(mock(ModelChild.class));
    when(modelProvider.getAllRootChildren()).thenReturn(cursorChildren);
  }

  @Override
  protected TimeoutSessionImpl getSessionImpl() {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    TimeoutSessionImpl session =
        new TimeoutSessionImpl(store, false, taskQueue, timingUtils, threadUtils);
    session.bindModelProvider(modelProvider, /* viewDepthProvider= */ null);
    return session;
  }

  private void setupForViewDepthProvider(
      TimeoutSessionImpl session, String lowestChild, List<ModelChild> modelChildren) {
    when(viewDepthProvider.getChildViewDepth()).thenReturn(lowestChild);
    when(modelProvider.getAllRootChildren()).thenReturn(modelChildren);
    session.setSessionId(TEST_SESSION_ID);
    session.bindModelProvider(modelProvider, viewDepthProvider);
    session.populateModelProvider(
        new ArrayList<>(), /* cachedBindings= */ false, /* legacyHeadContent= */ true);
  }
}
