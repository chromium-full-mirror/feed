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
  @Mock private ThreadUtils threadUtils;
  @Mock private Clock clock;

  private final TimingUtils timingUtils = new TimingUtils();

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
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, true);
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
    assertThat(session.contentInSession).hasSize(featureCnt);
    assertThat(session.contentInSession)
        .contains(contentIdGenerators.createFeatureContentId(featureId));
  }

  @Test
  public void testClearHead_onPaginationRequest() {
    SessionImpl session = getSessionImpl();
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, true);
    int featureCnt = 3;
    int featureId = 5;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, featureId);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    ViewDepthProvider mockDepthProvider = mock(ViewDepthProvider.class);
    session.bindModelProvider(modelProvider, mockDepthProvider);
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, false);
    session.updateSession(
        true,
        streamStructures,
        new MutationContext.Builder()
            .setRequestingSessionId(TEST_SESSION.getSessionId())
            .setContinuationToken(StreamToken.newBuilder().setContentId("token").build())
            .build());
    verify(modelProvider).invalidate(UiContext.getDefaultInstance());
  }

  @Test
  public void testPopulateModelProviderState() {
    TimeoutSessionImpl session = getSessionImpl();
    assertThat(session.legacyHeadContent).isFalse();
    assertThat(session.viewDepthProvider).isNull();

    ViewDepthProvider mockDepthProvider = mock(ViewDepthProvider.class);
    session.bindModelProvider(modelProvider, mockDepthProvider);
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, true);
    assertThat(session.legacyHeadContent).isTrue();
    assertThat(session.viewDepthProvider).isEqualTo(mockDepthProvider);
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
    int childCount = 10;
    List<ModelChild> modelChildren = new ArrayList<>();
    for (int i = 0; i < childCount; i++) {
      modelChildren.add(createModelChild(i));
    }
    String lowestChild = contentIdGenerators.createFeatureContentId(6);
    List<StreamStructure> removeOperations = session.removeItems(lowestChild, modelChildren);
    assertThat(removeOperations).hasSize(3);
  }

  private ModelChild createModelChild(long id) {
    ModelChild child = mock(ModelChild.class);
    when(child.getContentId()).thenReturn(contentIdGenerators.createFeatureContentId(id));
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
    session.bindModelProvider(modelProvider, null);
    return session;
  }
}
