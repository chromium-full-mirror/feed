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
package com.google.android.libraries.feed.feedactionmanager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.LocalActionMutation;
import com.google.android.libraries.feed.api.store.LocalActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FeedActionManagerImpl} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedActionManagerImplTest {

  private static final String CONTENT_ID_STRING = "contentIdString";
  private static final String SESSION_ID = "session";

  @Mock private SessionManager sessionManager;
  @Mock private Store store;
  @Mock private ThreadUtils threadUtils;
  @Mock private Clock clock;
  @Mock private LocalActionMutation localActionMutation;
  @Mock private Consumer<Result<List<StreamDataOperation>>> streamDataOperationsConsumer;
  @Captor private ArgumentCaptor<Integer> actionTypeCaptor;
  @Captor private ArgumentCaptor<String> contentIdStringCaptor;
  @Captor private ArgumentCaptor<Result<List<StreamDataOperation>>> streamDataOperationsCaptor;
  @Captor private ArgumentCaptor<MutationContext> mutationContextCaptor;

  private ActionManager actionManager;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    actionManager = new FeedActionManagerImpl(sessionManager, store, threadUtils, getTaskQueue());
  }

  @Test
  public void dismissLocal() throws Exception {
    setUpDismissMocks();
    StreamDataOperation dataOperation = buildBasicDismissOperation();

    actionManager.dismissLocal(
        Collections.singletonList(CONTENT_ID_STRING),
        Collections.singletonList(dataOperation),
        null);

    verify(localActionMutation).add(actionTypeCaptor.capture(), contentIdStringCaptor.capture());
    assertThat(actionTypeCaptor.getValue()).isEqualTo(ActionType.DISMISS);
    assertThat(contentIdStringCaptor.getValue()).isEqualTo(CONTENT_ID_STRING);

    verify(localActionMutation).commit();

    verify(streamDataOperationsConsumer).accept(streamDataOperationsCaptor.capture());
    Result<List<StreamDataOperation>> result = streamDataOperationsCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    List<StreamDataOperation> streamDataOperations = result.getValue();
    assertThat(streamDataOperations).hasSize(1);
    StreamDataOperation streamDataOperation = streamDataOperations.get(0);
    assertThat(streamDataOperation).isEqualTo(dataOperation);
  }

  @Test
  public void dismissLocal_sessionTokenSet() throws Exception {
    setUpDismissMocks();
    StreamDataOperation dataOperation = buildBasicDismissOperation();

    actionManager.dismissLocal(
        Collections.singletonList(CONTENT_ID_STRING),
        Collections.singletonList(dataOperation),
        SESSION_ID);

    verify(localActionMutation).add(actionTypeCaptor.capture(), contentIdStringCaptor.capture());
    assertThat(actionTypeCaptor.getValue()).isEqualTo(ActionType.DISMISS);
    assertThat(contentIdStringCaptor.getValue()).isEqualTo(CONTENT_ID_STRING);

    verify(localActionMutation).commit();

    verify(sessionManager).getUpdateConsumer(mutationContextCaptor.capture());
    assertThat(mutationContextCaptor.getValue().getRequestingSessionId()).isEqualTo(SESSION_ID);

    verify(streamDataOperationsConsumer).accept(streamDataOperationsCaptor.capture());
    Result<List<StreamDataOperation>> result = streamDataOperationsCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    List<StreamDataOperation> streamDataOperations = result.getValue();
    assertThat(streamDataOperations).hasSize(1);
    StreamDataOperation streamDataOperation = streamDataOperations.get(0);
    assertThat(streamDataOperation).isEqualTo(dataOperation);
  }

  @Test
  public void dismiss() throws Exception {
    setUpDismissMocks();
    StreamDataOperation dataOperation = buildBasicDismissOperation();

    actionManager.dismiss(Collections.singletonList(dataOperation), null);

    verify(streamDataOperationsConsumer).accept(streamDataOperationsCaptor.capture());
    Result<List<StreamDataOperation>> result = streamDataOperationsCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    List<StreamDataOperation> streamDataOperations = result.getValue();
    assertThat(streamDataOperations).hasSize(1);
    StreamDataOperation streamDataOperation = streamDataOperations.get(0);
    assertThat(streamDataOperation).isEqualTo(dataOperation);
  }

  @Test
  public void dismiss_sessionTokenSet() throws Exception {
    setUpDismissMocks();
    StreamDataOperation dataOperation = buildBasicDismissOperation();

    actionManager.dismiss(Collections.singletonList(dataOperation), SESSION_ID);

    verify(sessionManager).getUpdateConsumer(mutationContextCaptor.capture());
    assertThat(mutationContextCaptor.getValue().getRequestingSessionId()).isEqualTo(SESSION_ID);

    verify(streamDataOperationsConsumer).accept(streamDataOperationsCaptor.capture());
    Result<List<StreamDataOperation>> result = streamDataOperationsCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    List<StreamDataOperation> streamDataOperations = result.getValue();
    assertThat(streamDataOperations).hasSize(1);
    StreamDataOperation streamDataOperation = streamDataOperations.get(0);
    assertThat(streamDataOperation).isEqualTo(dataOperation);
  }

  @Test
  public void triggerUploadActions() throws Exception {
    HashSet<StreamUploadableAction> actionSet = new HashSet<>();
    actionSet.add(StreamUploadableAction.getDefaultInstance());
    actionManager.uploadActions(actionSet);
    verify(sessionManager).triggerUploadActions(actionSet);
  }

  private StreamDataOperation buildBasicDismissOperation() {
    return StreamDataOperation.newBuilder()
        .setStreamStructure(
            StreamStructure.newBuilder()
                .setContentId(CONTENT_ID_STRING)
                .setOperation(Operation.REMOVE))
        .build();
  }

  private void setUpDismissMocks() {
    when(sessionManager.getUpdateConsumer(any(MutationContext.class)))
        .thenReturn(streamDataOperationsConsumer);
    when(localActionMutation.add(anyInt(), anyString())).thenReturn(localActionMutation);
    when(store.editLocalActions()).thenReturn(localActionMutation);
  }

  private TaskQueue getTaskQueue() {
    TaskQueue taskQueue =
        new TaskQueue(
            MoreExecutors.directExecutor(),
            FakeMainThreadRunner.runTasksImmediately(),
            clock,
            false);
    taskQueue.initialize(() -> {});
    return taskQueue;
  }
}
