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

package com.google.android.libraries.feed.feedstore;

import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.DISMISS_ACTION_JOURNAL;
import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SEMANTIC_PROPERTIES_PREFIX;
import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SHARED_STATE_PREFIX;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.ActionMutation;
import com.google.android.libraries.feed.api.store.ActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.api.store.StoreListener;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.SimpleSettableFuture;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.feedstore.internal.ActionGc;
import com.google.android.libraries.feed.feedstore.internal.ContentGc;
import com.google.android.libraries.feed.feedstore.internal.FeedActionMutation;
import com.google.android.libraries.feed.feedstore.internal.FeedContentMutation;
import com.google.android.libraries.feed.feedstore.internal.FeedSemanticPropertiesMutation;
import com.google.android.libraries.feed.feedstore.internal.FeedSessionMutation;
import com.google.android.libraries.feed.feedstore.internal.SessionState;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation.Builder;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.search.now.feed.client.StreamDataProto.StreamAction;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Store. The PersistentFeedStore will call the host APIs {@link
 * ContentStorage} and {@link JournalStorage} to make persistent changes.
 */
public class PersistentFeedStore implements Store, Dumpable {

  private static final String TAG = "PersistentFeedStore";

  private final TimingUtils timingUtils;
  private final FeedExtensionRegistry extensionRegistry;
  private final ContentStorage contentStorage;
  private final JournalStorage journalStorage;
  private final ThreadUtils threadUtils;
  private final MainThreadRunner mainThreadRunner;
  private final Clock clock;
  private final FeedStoreHelper storeHelper;

  public PersistentFeedStore(
      TimingUtils timingUtils,
      FeedExtensionRegistry extensionRegistry,
      ContentStorage contentStorage,
      JournalStorage journalStorage,
      ThreadUtils threadUtils,
      MainThreadRunner mainThreadRunner,
      Clock clock,
      FeedStoreHelper storeHelper) {
    this.timingUtils = timingUtils;
    this.extensionRegistry = extensionRegistry;
    this.contentStorage = contentStorage;
    this.journalStorage = journalStorage;
    this.threadUtils = threadUtils;
    this.mainThreadRunner = mainThreadRunner;
    this.clock = clock;
    this.storeHelper = storeHelper;
  }

  @Override
  public Result<List<PayloadWithId>> getPayloads(List<String> contentIds) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<PayloadWithId> payloads = new ArrayList<>(contentIds.size());
    try {
      SimpleSettableFuture<Result<Map<String, byte[]>>> contentFuture =
          new SimpleSettableFuture<>();

      mainThreadRunner.execute(TAG, () -> contentStorage.get(contentIds, contentFuture::put));

      Result<Map<String, byte[]>> contentResult = contentFuture.get();
      if (!contentResult.isSuccessful()) {
        Logger.e(TAG, "Unsuccessful fetching payloads for content ids %s", contentIds);
        tracker.stop("getPayloads failed", "items", contentIds);
        return Result.failure();
      }

      for (Map.Entry<String, byte[]> entry : contentResult.getValue().entrySet()) {
        try {
          StreamPayload streamPayload =
              StreamPayload.parseFrom(entry.getValue(), extensionRegistry.getExtensionRegistry());
          payloads.add(new PayloadWithId(entry.getKey(), streamPayload));
        } catch (InvalidProtocolBufferException e) {
          Logger.e(TAG, "Couldn't parse content proto for id %s", entry.getKey());
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      Logger.e(TAG, e, "Exception getting payload for ids: %s", contentIds);
      tracker.stop("getPayloads failed", "items", contentIds);
      return Result.failure();
    }
    tracker.stop("", "getPayloads", "items", contentIds.size());
    return Result.success(payloads);
  }

  @Override
  public Result<List<StreamSharedState>> getSharedStates() {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    SimpleSettableFuture<Result<List<byte[]>>> sharedStatesFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG,
        () ->
            contentStorage.getAll(
                SHARED_STATE_PREFIX,
                result -> {
                  if (result.isSuccessful()) {
                    Map<String, byte[]> input = result.getValue();
                    sharedStatesFuture.put(Result.success(new ArrayList<>(input.values())));
                  } else {
                    sharedStatesFuture.put(Result.failure());
                  }
                }));
    try {
      Result<List<byte[]>> bytesResult = sharedStatesFuture.get();
      if (!bytesResult.isSuccessful()) {
        Logger.e(TAG, "Error fetching shared states");
        tracker.stop("getSharedStates", "failed");
        return Result.failure();
      }
      List<byte[]> result = bytesResult.getValue();
      List<StreamSharedState> sharedStates = new ArrayList<>(result.size());
      for (byte[] byteArray : result) {
        try {
          sharedStates.add(StreamSharedState.parseFrom(byteArray));
        } catch (InvalidProtocolBufferException e) {
          tracker.stop("getSharedStates", "failed");
          Logger.e(TAG, e, "Error parsing protocol buffer from bytes %s", byteArray);
        }
      }
      tracker.stop("", "getSharedStates", "items", sharedStates.size());
      return Result.success(sharedStates);
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error occurred getting shared states");
      tracker.stop("getSharedStates", "failed");
      return Result.failure();
    }
  }

  @Override
  public Result<List<StreamStructure>> getStreamStructures(StreamSession session) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<StreamStructure> streamStructures;
    try {
      SimpleSettableFuture<Result<List<byte[]>>> operationsFuture = new SimpleSettableFuture<>();
      mainThreadRunner.execute(
          TAG, () -> journalStorage.read(session.getStreamToken(), operationsFuture::put));
      Result<List<byte[]>> operationsResult = operationsFuture.get();
      if (!operationsResult.isSuccessful()) {
        Logger.e(TAG, "Error fetching stream structures for stream session %s", session);
        tracker.stop("getStreamStructures failed", "session", session);
        return Result.failure();
      }
      List<byte[]> results = operationsResult.getValue();
      streamStructures = new ArrayList<>(results.size());
      for (byte[] bytes : results) {
        if (bytes.length == 0) {
          continue;
        }
        try {
          streamStructures.add(StreamStructure.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
          Logger.e(TAG, e, "Error parsing stream structure.");
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Couldn't read stream structures");
      tracker.stop("getStreamStructures failed", "session", session);
      return Result.failure();
    }
    tracker.stop("", "getStreamStructures", "items", streamStructures.size());
    return Result.success(streamStructures);
  }

  @Override
  public Result<List<StreamSession>> getAllSessions() {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<StreamSession> sessions;

    try {
      SimpleSettableFuture<Result<List<String>>> contentFuture = new SimpleSettableFuture<>();
      mainThreadRunner.execute(TAG, () -> journalStorage.getAllJournals(contentFuture::put));

      Result<List<String>> namesResult = contentFuture.get();
      if (!namesResult.isSuccessful()) {
        Logger.e(TAG, "Error fetching all journals");
        tracker.stop("getAllSessions failed");
        return Result.failure();
      }

      List<String> result = namesResult.getValue();
      sessions = new ArrayList<>(result.size());
      for (String name : result) {
        if (HEAD.getStreamToken().equals(name)) {
          // Don't add $HEAD to the sessions list
          continue;
        }
        if (DISMISS_ACTION_JOURNAL.equals(name)) {
          // Don't add Dismiss Actions journal to sessions list
          continue;
        }
        sessions.add(StreamSession.newBuilder().setStreamToken(name).build());
      }
    } catch (ExecutionException | InterruptedException e) {
      Logger.e(TAG, e, "Exception getting stream sessions");
      tracker.stop("getAllSessions failed");
      return Result.failure();
    }
    tracker.stop("", "getAllSessions", "items", sessions.size());
    return Result.success(sessions);
  }

  @Override
  public Result<List<SemanticPropertiesWithId>> getSemanticProperties(List<String> contentIds) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<SemanticPropertiesWithId> semanticPropertiesWithIds = new ArrayList<>(contentIds.size());
    List<String> contentIdKeys = new ArrayList<>(contentIds.size());
    for (String contentId : contentIds) {
      contentIdKeys.add(SEMANTIC_PROPERTIES_PREFIX + contentId);
    }
    SimpleSettableFuture<Result<Map<String, byte[]>>> mapResultFuture =
        new SimpleSettableFuture<>();
    mainThreadRunner.execute(TAG, () -> contentStorage.get(contentIdKeys, mapResultFuture::put));
    Result<Map<String, byte[]>> mapResult;
    try {
      mapResult = mapResultFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error getting semantic properties for content ids %s", contentIds);
      tracker.stop("getSemanticProperties failed", contentIds);
      return Result.failure();
    }

    if (mapResult.isSuccessful()) {
      for (Map.Entry<String, byte[]> entry : mapResult.getValue().entrySet()) {
        String contentId = entry.getKey().replace(SEMANTIC_PROPERTIES_PREFIX, "");
        if (contentIds.contains(contentId)) {
          semanticPropertiesWithIds.add(new SemanticPropertiesWithId(contentId, entry.getValue()));
        }
      }
    } else {
      Logger.e(TAG, "Error fetching semantic properties for content ids %s", contentIds);
      tracker.stop("getSemanticProperties failed", contentIds);
    }

    tracker.stop("getSemanticProperties", semanticPropertiesWithIds);
    return Result.success(semanticPropertiesWithIds);
  }

  @Override
  public Result<List<StreamAction>> getAllDismissActions() {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);

    SimpleSettableFuture<Result<List<byte[]>>> actionsJournalFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG, () -> journalStorage.read(DISMISS_ACTION_JOURNAL, actionsJournalFuture::put));
    Result<List<byte[]>> listResult;
    try {
      listResult = actionsJournalFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error fetching dismiss journal");
      tracker.stop("getAllDismissActions failed");
      return Result.failure();
    }
    if (!listResult.isSuccessful()) {
      Logger.e(TAG, "Error retrieving dismiss journal");
      tracker.stop("getAllDismissActions failed");
      return Result.failure();
    } else {
      List<byte[]> actionByteArrays = listResult.getValue();
      List<StreamAction> actions = new ArrayList<>(actionByteArrays.size());
      for (byte[] bytes : actionByteArrays) {
        StreamAction action;
        try {
          action = StreamAction.parseFrom(bytes);
          actions.add(action);
        } catch (InvalidProtocolBufferException e) {
          Logger.e(TAG, e, "Error parsing StreamAction for bytes: %s", bytes);
        }
      }
      tracker.stop("getAllDismissActions");
      return Result.success(actions);
    }
  }

  @Override
  public Result<StreamSession> createNewSession() {
    threadUtils.checkNotMainThread();

    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    StreamSession streamSession = storeHelper.getNewStreamSession();
    SessionState session = new SessionState();
    Result<List<StreamStructure>> streamStructuresResult = getStreamStructures(HEAD);
    if (!streamStructuresResult.isSuccessful()) {
      Logger.e(TAG, "Could not retrieve stream structures");
      tracker.stop("createNewSession failed");
      return Result.failure();
    }

    session.addAll(streamStructuresResult.getValue());

    // While journal storage is async, we want PersistentFeedStore calls to be synchronous (for now)
    CountDownLatch latch = new CountDownLatch(1);
    mainThreadRunner.execute(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(HEAD.getStreamToken())
                    .copy(streamSession.getStreamToken())
                    .build(),
                ignored -> latch.countDown()));
    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.e(TAG, "Error creating new session due to interrupt", e);
    }
    tracker.stop("createNewSession", streamSession.getStreamToken());
    return Result.success(streamSession);
  }

  @Override
  public StreamSession getHeadSession() {
    return HEAD;
  }

  @Override
  public void removeSession(StreamSession session) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    if (session.getStreamToken().equals(HEAD.getStreamToken())) {
      // TODO: What type of exception should this throw?
      throw new IllegalStateException("Unable to delete the $HEAD session");
    }
    // While journal storage is async, we want PersistentFeedStore calls to be synchronous (for now)
    CountDownLatch latch = new CountDownLatch(1);
    mainThreadRunner.execute(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(session.getStreamToken()).delete().build(),
                ignored -> latch.countDown()));
    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.e(TAG, "Error removing session due to interrupt", e);
    }
    tracker.stop("removeSession", session.getStreamToken());
  }

  @Override
  public void clearHead() {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);

    // While journal storage is async, we want PersistentFeedStore calls to be synchronous (for now)
    CountDownLatch latch = new CountDownLatch(1);
    mainThreadRunner.execute(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(HEAD.getStreamToken())
                    .delete()
                    .append(new byte[0])
                    .build(),
                ignored -> latch.countDown()));
    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.e(TAG, "Error clearing head due to interrupt", e);
    }
    tracker.stop("", "clearHead");
  }

  @Override
  public ContentMutation editContent() {
    threadUtils.checkNotMainThread();
    return new FeedContentMutation(this::commitContentMutation);
  }

  @Override
  public SessionMutation editSession(StreamSession streamSession) {
    threadUtils.checkNotMainThread();
    return new FeedSessionMutation(
        feedSessionMutation -> commitSessionMutation(streamSession, feedSessionMutation));
  }

  @Override
  public SemanticPropertiesMutation editSemanticProperties() {
    threadUtils.checkNotMainThread();
    return new FeedSemanticPropertiesMutation(this::commitSemanticPropertiesMutation);
  }

  @Override
  public ActionMutation editActions() {
    threadUtils.checkNotMainThread();
    return new FeedActionMutation(this::commitActionMutation);
  }

  @Override
  public Runnable triggerContentGc(
      Set<String> reservedContentIds, Supplier<Set<String>> accessibleContent) {
    Supplier<Set<StreamAction>> dismissActionSupplier =
        () -> {
          Result<List<StreamAction>> dismissActionsResult = getAllDismissActions();
          if (!dismissActionsResult.isSuccessful()) {
            // TODO: clean up error condition
            Logger.e(TAG, "Error retrieving dismiss actions for content garbage collection");
            return Collections.emptySet();
          } else {
            return new HashSet<>(dismissActionsResult.getValue());
          }
        };

    return new ContentGc(
            accessibleContent,
            reservedContentIds,
            dismissActionSupplier,
            contentStorage,
            mainThreadRunner,
            timingUtils)
        ::gc;
  }

  @Override
  public Runnable triggerActionGc(List<StreamAction> actions, List<String> validContentIds) {
    return new ActionGc(
            actions,
            validContentIds,
            journalStorage,
            mainThreadRunner,
            timingUtils,
            DISMISS_ACTION_JOURNAL)
        ::gc;
  }

  @Override
  public void switchToEphemeralMode() {
    // TODO: implement cleanup
  }

  @Override
  public boolean isEphemeralMode() {
    return false;
  }

  @Override
  public void registerObserver(StoreListener observer) {
    throw new UnsupportedOperationException(
        "PersistentFeedStore does not support observer directly");
  }

  @Override
  public void unregisterObserver(StoreListener observer) {
    throw new UnsupportedOperationException(
        "PersistentFeedStore does not support observer directly");
  }

  private CommitResult commitSemanticPropertiesMutation(
      Map<String, ByteString> semanticPropertiesMap) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    Builder mutationBuilder = new Builder();
    for (Map.Entry<String, ByteString> entry : semanticPropertiesMap.entrySet()) {
      mutationBuilder.upsert(
          SEMANTIC_PROPERTIES_PREFIX + entry.getKey(), entry.getValue().toByteArray());
    }
    SimpleSettableFuture<CommitResult> resultFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG,
        () -> {
          // TODO: handle errors
          contentStorage.commit(mutationBuilder.build(), resultFuture::put);
        });
    CommitResult commitResult;
    try {
      commitResult = resultFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      tracker.stop("", "commitSemanticPropertiesMutation failed");
      Logger.e(TAG, e, "Error committing semantic properties");
      return CommitResult.FAILURE;
    }
    tracker.stop("", "commitSemanticPropertiesMutation", "mutations", semanticPropertiesMap.size());
    return commitResult;
  }

  private Boolean commitSessionMutation(
      StreamSession streamSession, List<StreamStructure> streamStructures) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    JournalMutation.Builder mutation = new JournalMutation.Builder(streamSession.getStreamToken());
    if (streamStructures.isEmpty()) {
      // allow an empty journal to be created
      mutation.append(new byte[0]);
    }
    for (StreamStructure streamStructure : streamStructures) {
      mutation.append(streamStructure.toByteArray());
    }
    SimpleSettableFuture<CommitResult> resultFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG,
        () -> {
          // TOOD(rbonick): handle errors
          journalStorage.commit(mutation.build(), resultFuture::put);
        });
    boolean result;
    try {
      result = CommitResult.SUCCESS.equals(resultFuture.get());
    } catch (InterruptedException | ExecutionException e) {
      tracker.stop("", "commitSessionMutationFailure");
      throw new IllegalStateException("Unable to mutate session", e);
    }
    tracker.stop("", "commitSessionMutation", "mutations", streamStructures.size());
    Logger.i(
        TAG,
        "commitSessionMutation - Success %s, Update Session %s, stream structures %s",
        result,
        streamSession.getStreamToken(),
        streamStructures.size());
    return result;
  }

  private CommitResult commitContentMutation(List<PayloadWithId> mutations) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);

    CommitResult commitResult = CommitResult.FAILURE;
    Builder contentMutationBuilder = new Builder();
    for (PayloadWithId mutation : mutations) {
      String payloadId = mutation.contentId;
      StreamPayload payload = mutation.payload;
      if (mutation.payload.hasStreamSharedState()) {
        StreamSharedState streamSharedState = mutation.payload.getStreamSharedState();
        contentMutationBuilder.upsert(
            SHARED_STATE_PREFIX + streamSharedState.getContentId(),
            streamSharedState.toByteArray());
      } else {
        contentMutationBuilder.upsert(payloadId, payload.toByteArray());
      }
    }

    // Block waiting for the response from storage, to make this method synchronous.
    SimpleSettableFuture<CommitResult> commitResultFuture = new SimpleSettableFuture<>();
    // TODO: handle errors
    mainThreadRunner.execute(
        TAG, () -> contentStorage.commit(contentMutationBuilder.build(), commitResultFuture::put));
    try {
      commitResult = commitResultFuture.get();
    } catch (ExecutionException | InterruptedException e) {
      Logger.e(TAG, e, "Exception committing content mutation");
    }

    tracker.stop("task", "commitContentMutation", "mutations", mutations.size());
    return commitResult;
  }

  private CommitResult commitActionMutation(Map<Integer, List<String>> actions) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);

    CommitResult commitResult = CommitResult.SUCCESS;
    for (Map.Entry<Integer, List<String>> entry : actions.entrySet()) {
      Integer actionType = entry.getKey();
      String journalName;
      if (ActionType.DISMISS == entry.getKey()) {
        journalName = DISMISS_ACTION_JOURNAL;
      } else {
        Logger.e(TAG, "Unknown journal name for action type %s", actionType);
        continue;
      }
      JournalMutation.Builder builder = new JournalMutation.Builder(journalName);
      for (String contentId : entry.getValue()) {
        StreamAction action =
            StreamAction.newBuilder()
                .setAction(actionType)
                .setFeatureContentId(contentId)
                .setTimestampSeconds(TimeUnit.MILLISECONDS.toSeconds(clock.currentTimeMillis()))
                .build();
        builder.append(action.toByteArray());
      }
      SimpleSettableFuture<CommitResult> commitResultFuture = new SimpleSettableFuture<>();
      mainThreadRunner.execute(
          TAG, () -> journalStorage.commit(builder.build(), commitResultFuture::put));
      try {
        commitResult = commitResultFuture.get();
        if (commitResult == CommitResult.FAILURE) {
          Logger.e(TAG, "Error committing action for type %s", actionType);
          break;
        }
      } catch (InterruptedException | ExecutionException e) {
        Logger.e(TAG, e, "Error committing action");
        tracker.stop("commitActionMutation failed");
        return CommitResult.FAILURE;
      }
    }

    tracker.stop("task", "commitActionMutation", "actions", actions.size());
    return commitResult;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    if (contentStorage instanceof Dumpable) {
      dumper.dump((Dumpable) contentStorage);
    } else {
      dumper.forKey("contentStorage").value("not dumpable");
    }
    if (journalStorage instanceof Dumpable) {
      dumper.dump((Dumpable) journalStorage);
    } else {
      dumper.forKey("journalStorage").value("not dumpable");
    }
  }

  /**
   * Clears contents and journals relating to the stream. Leaves actions and semantic properties
   * intact.
   *
   * @return {@code true} when the clear succeeded
   */
  boolean clearStorage() {
    try {
      if (!clearContentStorage()) {
        return false;
      }
      if (!clearJournalStorage()) {
        return false;
      }
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error clearing all contents.");
      return false;
    }
    return true;
  }

  /**
   * Cleans out journal storage (excluding dismiss actions journal)
   *
   * @return whether the clear succeeded
   */
  private boolean clearJournalStorage() throws InterruptedException, ExecutionException {
    SimpleSettableFuture<Result<List<String>>> journalFuture = new SimpleSettableFuture<>();
    journalStorage.getAllJournals(journalFuture::put);
    Result<List<String>> allJournalsResult = journalFuture.get();
    if (!allJournalsResult.isSuccessful()) {
      Logger.e(TAG, "Error clearing all contents. Could not fetch all journals.");
      return false;
    }

    for (String journal : allJournalsResult.getValue()) {
      if (DISMISS_ACTION_JOURNAL.equals(journal)) {
        continue;
      }
      SimpleSettableFuture<CommitResult> commitResultFuture = new SimpleSettableFuture<>();
      journalStorage.commit(
          new JournalMutation.Builder(journal).delete().build(), commitResultFuture::put);
      if (commitResultFuture.get() != CommitResult.SUCCESS) {
        Logger.e(TAG, "Error clearing all contents. Could not delete journal %s", journal);
        return false;
      }
    }
    return true;
  }

  /**
   * Cleans out content storage (excluding semantic properties)
   *
   * @return whether the clear succeeded
   */
  private boolean clearContentStorage() throws InterruptedException, ExecutionException {
    SimpleSettableFuture<Result<Map<String, byte[]>>> contentFuture = new SimpleSettableFuture<>();
    // TODO: replace with getAllKeys call
    contentStorage.getAll("", contentFuture::put);
    Result<Map<String, byte[]>> allContentMapResult = contentFuture.get();
    if (!allContentMapResult.isSuccessful()) {
      Logger.e(TAG, "Error clearing all contents. Could not fetch all content.");
      return false;
    }
    Builder contentMutationBuilder = new Builder();
    for (String key : allContentMapResult.getValue().keySet()) {
      if (key.contains(SEMANTIC_PROPERTIES_PREFIX)) {
        continue;
      }
      contentMutationBuilder.delete(key);
    }
    SimpleSettableFuture<CommitResult> commitResultFuture = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG, () -> contentStorage.commit(contentMutationBuilder.build(), commitResultFuture::put));
    if (commitResultFuture.get() != CommitResult.SUCCESS) {
      Logger.e(TAG, "Error clearing all contents. Could not commit content deletions.");
      return false;
    }
    return true;
  }

  /** Wipes all content and journals */
  boolean clearAll() {
    boolean success = true;
    // Run clear on both content and journal
    SimpleSettableFuture<CommitResult> contentCommitResult = new SimpleSettableFuture<>();
    mainThreadRunner.execute(
        TAG,
        () -> contentStorage.commit(new Builder().deleteAll().build(), contentCommitResult::put));
    SimpleSettableFuture<CommitResult> journalCommitResult = new SimpleSettableFuture<>();
    mainThreadRunner.execute(TAG, () -> journalStorage.deleteAll(journalCommitResult::put));

    // Confirm results were successful
    try {
      if (contentCommitResult.get() != CommitResult.SUCCESS) {
        Logger.e(TAG, "Error clearing all. Could not delete all content from content storage.");
        success = false;
      }
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error clearing all. Could not delete all content from content storage.");
      success = false;
    }
    try {
      if (contentCommitResult.get() != CommitResult.SUCCESS) {
        Logger.e(TAG, "Error clearing all. Could not delete all journals from journal storage.");
        success = false;
      }
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error clearing all. Could not delete all content from content storage.");
      success = false;
    }
    return success;
  }
}
