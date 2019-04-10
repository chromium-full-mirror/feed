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

import android.net.Uri;
import android.util.Base64;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.store.LocalActionMutation;
import com.google.android.libraries.feed.api.store.LocalActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.TaskQueue.TaskType;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.internalapi.actionmanager.ActionManager;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.wire.feed.ActionPayloadProto.ActionPayload;
import java.util.HashSet;
import java.util.List;

/** Default implementation of {@link ActionManager} */
public class FeedActionManagerImpl implements ActionManager {

  private final SessionManager sessionManager;
  private final Store store;
  private final ThreadUtils threadUtils;
  private final TaskQueue taskQueue;
  private final MainThreadRunner mainThreadRunner;

  public FeedActionManagerImpl(
      SessionManager sessionManager,
      Store store,
      ThreadUtils threadUtils,
      TaskQueue taskQueue,
      MainThreadRunner mainThreadRunner) {
    this.sessionManager = sessionManager;
    this.store = store;
    this.threadUtils = threadUtils;
    this.taskQueue = taskQueue;
    this.mainThreadRunner = mainThreadRunner;
  }

  @Override
  public void dismissLocal(
      List<String> contentIds,
      List<StreamDataOperation> streamDataOperations,
      /*@Nullable*/ String sessionId) {
    executeStreamDataOperations(streamDataOperations, sessionId);
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
  public void dismiss(List<StreamDataOperation> streamDataOperations, /*@Nullable*/ String sessionId) {
    executeStreamDataOperations(streamDataOperations, sessionId);
  }

  @Override
  public void createAndUploadAction(String contentId, ActionPayload payload) {
    taskQueue.execute(
        "feedActionUpload",
        TaskType.BACKGROUND,
        () -> {
          HashSet<StreamUploadableAction> actionSet = new HashSet<>();
          actionSet.add(
              StreamUploadableAction.newBuilder()
                  .setFeatureContentId(contentId)
                  .setPayload(payload)
                  .build());
          sessionManager.triggerUploadActions(actionSet);
        });
  }

  @Override
  public void uploadAllActionsAndUpdateUrl(
      String url, String consistencyTokenQueryParamName, Consumer<String> consumer) {
    taskQueue.execute(
        "feedActionUpload",
        TaskType.BACKGROUND,
        () -> {
          // TODO: figure out spinner and/or timeout conditions
          sessionManager.fetchActionsAndUpload(
              result -> {
                mainThreadRunner.execute(
                    "Open url",
                    () -> {
                      if (result.isSuccessful()) {
                        consumer.accept(
                            updateParam(
                                url,
                                consistencyTokenQueryParamName,
                                result.getValue().toByteArray()));
                      } else {
                        consumer.accept(url);
                      }
                    });
              });
        });
  }

  static String updateParam(String url, String consistencyTokenQueryParamName, byte[] value) {
    Uri.Builder uriBuilder = Uri.parse(url).buildUpon();
    uriBuilder.appendQueryParameter(
        consistencyTokenQueryParamName,
        Base64.encodeToString(value, Base64.URL_SAFE | Base64.NO_WRAP));
    return uriBuilder.build().toString();
  }

  private void executeStreamDataOperations(
      List<StreamDataOperation> streamDataOperations, /*@Nullable*/ String sessionId) {
    threadUtils.checkMainThread();

    MutationContext.Builder mutationContextBuilder =
        new MutationContext.Builder().setUserInitiated(true);
    if (sessionId != null) {
      mutationContextBuilder.setRequestingSessionId(sessionId);
    }
    sessionManager
        .getUpdateConsumer(mutationContextBuilder.build())
        .accept(Result.success(streamDataOperations));
  }
}
