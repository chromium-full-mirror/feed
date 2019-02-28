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

import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.LocalActionMutation;
import com.google.android.libraries.feed.api.store.LocalActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.TaskQueue.TaskType;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import java.util.List;
import java.util.Set;

/** Default implementation of {@link ActionManager} */
public class FeedActionManagerImpl implements ActionManager {

  private final SessionManager sessionManager;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TaskQueue taskQueue;

  public FeedActionManagerImpl(
      SessionManager sessionManager, Store store, ThreadUtils threadUtils, TaskQueue taskQueue) {
    this.sessionManager = sessionManager;
    this.store = store;
    this.threadUtils = threadUtils;
    this.taskQueue = taskQueue;
  }

  @Override
  public void dismissLocal(
      List<String> contentIds,
      List<StreamDataOperation> streamDataOperations,
      /*@Nullable*/ String sessionToken) {
    executeStreamDataOperations(streamDataOperations, sessionToken);
    // Store the dismissLocal actions
    taskQueue.execute(
        "feedActionManagerDismiss",
        TaskType.BACKGROUND,
        () -> {
          LocalActionMutation localActionMutation = store.editLocalActions();
          for (String contentId : contentIds) {
            localActionMutation.add(ActionType.DISMISS, contentId);
          }
          localActionMutation.commit();
        });
  }

  @Override
  public void dismiss(
      List<StreamDataOperation> streamDataOperations,
      /*@Nullable*/ String sessionToken) {
    executeStreamDataOperations(streamDataOperations, sessionToken);
  }

  @Override
  public void uploadActions(Set<StreamUploadableAction> actions) {
    taskQueue.execute(
        "feedActionUpload",
        TaskType.BACKGROUND,
        () -> {
          sessionManager.triggerUploadActions(actions);
        });
  }

  private void executeStreamDataOperations(
      List<StreamDataOperation> streamDataOperations, /*@Nullable*/ String sessionToken) {
    threadUtils.checkMainThread();

    // Update sessions
    StreamSession.Builder streamSessionBuilder = StreamSession.newBuilder();
    if (sessionToken != null) {
      streamSessionBuilder.setStreamToken(sessionToken);
    }
    sessionManager
        .getUpdateConsumer(
            new MutationContext.Builder()
                .setUserInitiated(true)
                .setRequestingSession(streamSessionBuilder.build())
                .build())
        .accept(Result.success(streamDataOperations));
  }
}
