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

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.testing.AbstractSessionImplTest;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link SessionImpl} class. */
@RunWith(RobolectricTestRunner.class)
public class SessionImplTest extends AbstractSessionImplTest {
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
    SessionImpl session = getSessionImpl();
    assertThat(session.invalidateOnResetHead()).isTrue();
  }

  @Test
  public void testClearHead() {
    SessionImpl session = getSessionImpl();
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, false);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // clear head will be ignored
    assertThat(streamStructures).hasSize(4);
    session.updateSession(true, streamStructures, null);
    assertThat(fakeSessionMutation.streamStructures).hasSize(0);
    assertThat(fakeModelMutator.addedChildren).hasSize(0);
    assertThat(session.contentInSession).hasSize(0);
  }

  @Test
  public void testClearHead_paginationRequest() {
    SessionImpl session = getSessionImpl();
    ViewDepthProvider mockDepthProvider = mock(ViewDepthProvider.class);
    session.bindModelProvider(modelProvider, mockDepthProvider);
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false, false);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();
    session.updateSession(
        true,
        streamStructures,
        new MutationContext.Builder()
            .setContinuationToken(StreamToken.newBuilder().setContentId("token").build())
            .setRequestingSession(TEST_SESSION)
            .build());

    verify(modelProvider).invalidate();
  }

  @Test
  public void testModelProviderBinding_withDetach() {
    SessionImpl session = getSessionImpl();
    ViewDepthProvider mockDepthProvider = mock(ViewDepthProvider.class);
    session.bindModelProvider(modelProvider, mockDepthProvider);
    assertThat(session.modelProvider).isEqualTo(modelProvider);
    assertThat(session.viewDepthProvider).isEqualTo(mockDepthProvider);

    session.bindModelProvider(null, null);
    assertThat(session.viewDepthProvider).isNull();
    assertThat(session.modelProvider).isNull();
  }

  @Override
  protected SessionImpl getSessionImpl() {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    SessionImpl session = new SessionImpl(store, false, taskQueue, timingUtils, threadUtils);
    session.bindModelProvider(modelProvider, null);
    return session;
  }
}
