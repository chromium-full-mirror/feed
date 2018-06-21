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

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.testing.AbstractSessionImplTest;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
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
    session.populateModelProvider(TEST_SESSION, new ArrayList<>(), false);
    int featureCnt = 3;
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder().addClearOperation();
    addFeatures(protocolBuilder, featureCnt, 1);
    List<StreamStructure> streamStructures = protocolBuilder.buildAsStreamStructure();

    // 1 clear, 3 features
    assertThat(streamStructures).hasSize(4);
    session.updateSession(true, streamStructures, null);
    assertThat(fakeSessionMutation.streamStructures).hasSize(featureCnt);
    assertThat(fakeModelMutator.addedChildren).hasSize(featureCnt);
    assertThat(session.contentInSession).hasSize(featureCnt);
    assertThat(session.contentInSession).contains(contentIdGenerators.createFeatureContentId(1));
  }

  @Override
  protected TimeoutSessionImpl getSessionImpl() {
    TaskQueue taskQueue = new TaskQueue(MoreExecutors.newDirectExecutorService(), clock, false);
    taskQueue.initialize(() -> {});
    TimeoutSessionImpl session = new TimeoutSessionImpl(store, taskQueue, timingUtils, threadUtils);
    session.bindModelProvider(modelProvider);
    return session;
  }
}
