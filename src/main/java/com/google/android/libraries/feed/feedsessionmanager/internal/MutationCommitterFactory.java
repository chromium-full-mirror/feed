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

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.logging.StringFormattingUtils;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedsessionmanager.internal.TaskQueue.TaskType;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.search.now.feed.client.StreamDataProto;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class which implements factory to create the task which commits a mutation to the SessionManager
 * and Sessions. This is created and used by the {@link
 * com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager} to commit a mutation
 * defined as a List of {@link StreamDataOperation}.
 */
public class MutationCommitterFactory {
  private static final String TAG = "MutationCommitter";

  private final Consumer</*@Nullable*/ StreamSession> resetHeadConsumer;
  private final Consumer<Session> notifySessionErrorConsumer;
  private final Store store;
  private final SessionCache sessionCache;
  private final ContentCache contentCache;
  private final Map<String, StreamSharedState> sharedStateCache;
  private final TaskQueue taskQueue;
  private final MainThreadRunner mainThreadRunner;
  private final SchedulerApi schedulerApi;
  private final ThreadUtils threadUtils;
  private final TimingUtils timingUtils;
  private final Clock clock;

  public MutationCommitterFactory(
      Consumer</*@Nullable*/ StreamSession> resetHeadConsumer,
      Consumer<Session> notifySessionErrorConsumer,
      Store store,
      SessionCache sessionCache,
      ContentCache contentCache,
      Map<String, StreamSharedState> sharedStateCache,
      TaskQueue taskQueue,
      MainThreadRunner mainThreadRunner,
      SchedulerApi schedulerApi,
      ThreadUtils threadUtils,
      TimingUtils timingUtils,
      Clock clock) {
    this.resetHeadConsumer = resetHeadConsumer;
    this.notifySessionErrorConsumer = notifySessionErrorConsumer;
    this.store = store;
    this.sessionCache = sessionCache;
    this.contentCache = contentCache;
    this.sharedStateCache = sharedStateCache;
    this.taskQueue = taskQueue;
    this.mainThreadRunner = mainThreadRunner;
    this.schedulerApi = schedulerApi;
    this.threadUtils = threadUtils;
    this.timingUtils = timingUtils;
    this.clock = clock;
  }

  /**
   * Return a Consumer of StreamDataOperations which will update the {@link
   * com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager}.
   */
  public Consumer<Result<List<StreamDataOperation>>> create(
      String task, MutationContext mutationContext) {
    return new MutationCommitter(task, mutationContext);
  }

  @VisibleForTesting
  class MutationCommitter implements Consumer<Result<List<StreamDataOperation>>> {

    private final String task;
    private final MutationContext mutationContext;
    private final List<StreamStructure> streamStructures = new ArrayList<>();

    @VisibleForTesting boolean clearedHead = false;
    private List<StreamDataOperation> dataOperations;

    private MutationCommitter(String task, MutationContext mutationContext) {
      this.task = task;
      this.mutationContext = mutationContext;
    }

    @Override
    public void accept(Result<List<StreamDataOperation>> updateResults) {
      if (!updateResults.isSuccessful()) {
        if (mutationContext.getContinuationToken() != null) {
          Logger.e(
              TAG, "Error found with a token request %s", mutationContext.getContinuationToken());
          Session session = null;
          StreamSession streamSession = mutationContext.getRequestingSession();
          String sessionToken = (streamSession != null) ? streamSession.getStreamToken() : null;
          if (sessionToken != null) {
            session = sessionCache.get(sessionToken);
          }
          if (session != null) {
            notifySessionErrorConsumer.accept(session);
          }
        } else {
          Logger.e(TAG, "Unexpected Update error found, the update is being ignored");
        }
        return;
      }
      dataOperations = updateResults.getValue();
      for (StreamDataOperation operation : dataOperations) {
        if (operation.getStreamStructure().getOperation() == Operation.CLEAR_ALL) {
          clearedHead = true;
          break;
        }
      }
      taskQueue.execute(
          "MutationCommitter:" + task + ":clearHead:" + clearedHead,
          clearedHead ? TaskType.HEAD_RESET : TaskType.USER_FACING,
          this::commitTask);
    }

    private void commitTask() {
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
      commitContent();
      commitSessionUpdates();
      timeTracker.stop(
          "task", "sessionMutation.commitTask:" + task, "mutations", streamStructures.size());
    }

    private void commitContent() {
      threadUtils.checkNotMainThread();
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      contentCache.startMutation();
      ContentMutation contentMutation = store.editContent();
      SemanticPropertiesMutation semanticPropertiesMutation = store.editSemanticProperties();
      for (StreamDataOperation dataOperation : dataOperations) {
        Operation operation = dataOperation.getStreamStructure().getOperation();
        if (operation == Operation.CLEAR_ALL) {
          streamStructures.add(dataOperation.getStreamStructure());
          resetHeadConsumer.accept(
              (mutationContext != null) ? mutationContext.getRequestingSession() : null);
          continue;
        }

        String contentId = dataOperation.getStreamStructure().getContentId();
        if (operation == Operation.UPDATE_OR_APPEND) {
          if (!validDataOperation(dataOperation)) {
            continue;
          }
          StreamPayload payload = dataOperation.getStreamPayload();
          contentCache.put(contentId, payload);
          if (payload.hasStreamSharedState()) {
            // don't add StreamSharedState to the metadata list stored for sessions
            contentMutation.add(contentId, payload);
            sharedStateCache.put(
                dataOperation.getStreamStructure().getContentId(), payload.getStreamSharedState());
          } else if (payload.hasStreamFeature() || payload.hasStreamToken()) {
            contentMutation.add(contentId, payload);
            streamStructures.add(dataOperation.getStreamStructure());
          } else if (dataOperation.getStreamPayload().hasSemanticData()) {
            semanticPropertiesMutation.add(
                contentId, dataOperation.getStreamPayload().getSemanticData());
            continue;
          } else {
            Logger.e(TAG, "Unsupported UPDATE_OR_APPEND payload");
          }
          continue;
        } else if (operation == Operation.REMOVE) {
          // We don't update the content for REMOVED items, content will be garbage collected.
          streamStructures.add(dataOperation.getStreamStructure());
          continue;
        }

        Logger.e(
            TAG, "Unsupported Mutation: %s", dataOperation.getStreamStructure().getOperation());
      }
      contentCache.finishMutation();
      taskQueue.execute("contentMutation", TaskType.BACKGROUND, contentMutation::commit);
      // TODO: currently there is a small window in which an action could occur and the
      // request be sent to the server before this is written, fix
      taskQueue.execute(
          "semanticPropertiesMutation", TaskType.BACKGROUND, semanticPropertiesMutation::commit);
      timeTracker.stop("", "contentUpdate", "items", dataOperations.size());
    }

    private void commitSessionUpdates() {
      threadUtils.checkNotMainThread();
      ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

      StreamDataProto.StreamToken mutationSourceToken =
          (mutationContext != null) ? mutationContext.getContinuationToken() : null;
      // For sessions we want to add the remove operation if the mutation source was a
      // continuation token.
      if (mutationSourceToken != null) {
        StreamStructure removeOperation = addTokenRemoveOperation(mutationSourceToken);
        if (removeOperation != null) {
          streamStructures.add(0, removeOperation);
        }
      }
      Collection<Session> updates = sessionCache.getSessions();
      HeadSessionImpl head = Validators.checkNotNull(sessionCache.getHead());
      for (Session session : updates) {
        if (clearedHead && session == head) {
          session.updateSession(clearedHead, streamStructures, mutationSourceToken);
          long updateTime = clock.currentTimeMillis();
          Validators.checkNotNull(head).updateAccessTime(updateTime);
          mainThreadRunner.execute(TAG, () -> schedulerApi.onReceiveNewContent(updateTime));
          Logger.i(
              TAG,
              "Cleared Head, new creation time %s",
              StringFormattingUtils.formatLogDate(updateTime));
          continue;
        }
        Logger.i(TAG, "Update Session %s", session.getStreamSession().getStreamToken());
        session.updateSession(clearedHead, streamStructures, mutationSourceToken);
      }
      timeTracker.stop("", "sessionUpdate", "sessions", updates.size());
    }

    @VisibleForTesting
    boolean validDataOperation(StreamDataOperation dataOperation) {
      if (!dataOperation.hasStreamPayload() || !dataOperation.hasStreamStructure()) {
        Logger.e(TAG, "Invalid StreamDataOperation - payload or streamStructure not defined");
        return false;
      }
      String contentId = dataOperation.getStreamStructure().getContentId();
      if (TextUtils.isEmpty(contentId)) {
        Logger.e(TAG, "Invalid StreamDataOperation - No ID Found");
        return false;
      }
      return true;
    }

    /*@Nullable*/
    private StreamStructure addTokenRemoveOperation(StreamToken token) {
      return StreamStructure.newBuilder()
          .setContentId(token.getContentId())
          .setParentContentId(token.getParentId())
          .setOperation(Operation.REMOVE)
          .build();
    }
  }
}
