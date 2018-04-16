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

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.SynchronousConsumerLatch;
import com.google.android.libraries.feed.common.concurrent.SimpleSettableFuture;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
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
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of the Store. The FeedStore will call the host APIs {@link ContentStorage} and
 * {@link JournalStorage} to make persistent changes.
 */
public class FeedStore implements Store, Dumpable {

  private static final String TAG = "FeedStore";
  private static final String SHARED_STATE_PREFIX = "ss::";
  private static final String SESSION_NAME_PREFIX = "_session:";
  private static final String SEMANTIC_PROPERTIES_PREFIX = "sp::";

  private final TimingUtils timingUtils;
  private final FeedExtensionRegistry extensionRegistry;
  private final ContentStorage contentStorage;
  private final JournalStorage journalStorage;
  private final ThreadUtils threadUtils;
  private final UiRunnableHandler uiRunnableHandler;

  public FeedStore(
      TimingUtils timingUtils,
      FeedExtensionRegistry extensionRegistry,
      ContentStorage contentStorage,
      JournalStorage journalStorage,
      ThreadUtils threadUtils,
      UiRunnableHandler uiRunnableHandler) {
    this.timingUtils = timingUtils;
    this.extensionRegistry = extensionRegistry;
    this.contentStorage = contentStorage;
    this.journalStorage = journalStorage;
    this.threadUtils = threadUtils;
    this.uiRunnableHandler = uiRunnableHandler;
  }

  @Override
  public List<PayloadWithId> getPayloads(List<String> contentIds) {
    threadUtils.checkNotMainThread();

    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<PayloadWithId> payloads = new ArrayList<>();
    try {
      SimpleSettableFuture<Map<String, byte[]>> contentFuture = new SimpleSettableFuture<>();

      uiRunnableHandler.post(
          TAG,
          () -> {
            contentStorage.get(
                contentIds,
                result -> {
                  if (result.isSuccessful()) {
                    Map<String, byte[]> input = result.getValue();
                    contentFuture.put(input);
                  } else {
                    // todo(rbonick): handle errors
                  }
                });
          });

      Map<String, byte[]> content = contentFuture.get();
      for (Map.Entry<String, byte[]> entry : content.entrySet()) {
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
    }
    tracker.stop("", "getPayloads", "items", contentIds.size());
    return payloads;
  }

  @Override
  public List<StreamSharedState> getSharedStates() {
    List<StreamSharedState> sharedStates = new ArrayList<>();
    SimpleSettableFuture<List<byte[]>> sharedStatesFuture = new SimpleSettableFuture<>();
    uiRunnableHandler.post(
        TAG,
        () -> {
          contentStorage.getAll(
              SHARED_STATE_PREFIX,
              result -> {
                if (result.isSuccessful()) {
                  Map<String, byte[]> input = result.getValue();
                  sharedStatesFuture.put(new ArrayList<>(input.values()));
                } else {
                  sharedStatesFuture.putException(
                      new RuntimeException("Error fetching shared states"));
                  // TODO: handle errors
                }
              });
        });
    try {
      List<byte[]> bytes = sharedStatesFuture.get();
      for (byte[] byteArray : bytes) {
        sharedStates.add(StreamSharedState.parseFrom(byteArray));
      }
    } catch (InvalidProtocolBufferException | InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error occurred getting shared states");
    }
    return sharedStates;
  }

  @Override
  public List<StreamStructure> getStreamStructures(StreamSession session) {
    threadUtils.checkNotMainThread();

    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<StreamStructure> streamStructures = new ArrayList<>();
    try {
      SimpleSettableFuture<List<byte[]>> operationsFuture = new SimpleSettableFuture<>();
      uiRunnableHandler.post(
          TAG,
          () ->
              journalStorage.read(
                  session.getStreamToken(),
                  result -> {
                    if (result.isSuccessful()) {
                      List<byte[]> value = result.getValue();
                      operationsFuture.put(value);
                    } else {
                      operationsFuture.putException(
                          new RuntimeException("Could not fetch stream structures"));
                      // TODO: handle errors
                    }
                  }));
      List<byte[]> operations = operationsFuture.get();
      for (byte[] bytes : operations) {
        if (bytes.length == 0) {
          continue;
        }
        streamStructures.add(StreamStructure.parseFrom(bytes));
      }
    } catch (InvalidProtocolBufferException | InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Couldn't read stream structures", e);
    }
    tracker.stop("", "getStreamStructures", "items", streamStructures.size());
    return streamStructures;
  }

  @Override
  public List<StreamSession> getAllSessions() {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<StreamSession> sessions = new ArrayList<>();

    try {
      SimpleSettableFuture<List<String>> contentFuture = new SimpleSettableFuture<>();
      uiRunnableHandler.post(
          TAG,
          () -> {
            journalStorage.getAllJournals(
                result -> {
                  if (result.isSuccessful()) {
                    List<String> value = result.getValue();
                    contentFuture.put(value);
                  } else {
                    // todo(rbonick): handle errors
                    contentFuture.putException(
                        new RuntimeException("Could not fetch all journals"));
                  }
                });
          });

      List<String> names = contentFuture.get();
      for (String name : names) {
        if (HEAD.getStreamToken().equals(name)) {
          // Don't add $HEAD to the sessions list
          continue;
        }
        sessions.add(StreamSession.newBuilder().setStreamToken(name).build());
      }
    } catch (ExecutionException | InterruptedException e) {
      Logger.e(TAG, e, "Exception getting stream sessions");
    }
    tracker.stop("", "getAllSessions", "items", sessions.size());
    return sessions;
  }

  @Override
  public List<SemanticPropertiesWithId> getSemanticProperties(List<String> contentIds) {
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<SemanticPropertiesWithId> semanticPropertiesWithIds = new ArrayList<>(contentIds.size());
    List<String> contentIdKeys = new ArrayList<>(contentIds.size());
    for (String contentId : contentIds) {
      contentIdKeys.add(SEMANTIC_PROPERTIES_PREFIX + contentId);
    }
    SimpleSettableFuture<Result<Map<String, byte[]>>> mapResultFuture =
        new SimpleSettableFuture<>();
    uiRunnableHandler.post(
        TAG,
        () -> {
          contentStorage.get(contentIdKeys, mapResultFuture::put);
        });
    Result<Map<String, byte[]>> mapResult;
    try {
      mapResult = mapResultFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, e, "Error getting semantic properties for content ids %s", contentIds);
      tracker.stop("getSemanticProperties failed", contentIds);
      return semanticPropertiesWithIds;
    }

    if (mapResult.isSuccessful()) {
      for (Map.Entry<String, byte[]> entry : mapResult.getValue().entrySet()) {
        String contentId = entry.getKey().replace(SEMANTIC_PROPERTIES_PREFIX, "");
        if (contentIds.contains(contentId)) {
          semanticPropertiesWithIds.add(
              new SemanticPropertiesWithId(contentId, ByteBuffer.wrap(entry.getValue())));
        }
      }
    } else {
      Logger.e(TAG, "Error fetching semantic properties for content ids %s", contentIds);
      tracker.stop("getSemanticProperties failed", contentIds);
    }

    tracker.stop("getSemanticProperties", semanticPropertiesWithIds);
    return semanticPropertiesWithIds;
  }

  @Override
  public StreamSession createNewSession() {
    threadUtils.checkNotMainThread();

    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    String sessionName = SESSION_NAME_PREFIX + UUID.randomUUID();
    StreamSession streamSession = StreamSession.newBuilder().setStreamToken(sessionName).build();
    SessionState session = new SessionState();
    session.addAll(getStreamStructures(HEAD));

    // While journal storage is async, we want FeedStore calls to be synchronous (for now)
    SynchronousConsumerLatch<CommitResult> latch = new SynchronousConsumerLatch<>();
    uiRunnableHandler.post(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(HEAD.getStreamToken())
                    .copy(streamSession.getStreamToken())
                    .build(),
                latch));
    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.e(TAG, "Error creating new session due to interrupt", e);
    }
    tracker.stop("createNewSession", streamSession.getStreamToken());
    return streamSession;
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
    // While journal storage is async, we want FeedStore calls to be synchronous (for now)
    SynchronousConsumerLatch<CommitResult> latch = new SynchronousConsumerLatch<>();
    uiRunnableHandler.post(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(session.getStreamToken()).delete().build(), latch));
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

    // While journal storage is async, we want FeedStore calls to be synchronous (for now)
    SynchronousConsumerLatch<CommitResult> latch = new SynchronousConsumerLatch<>();
    uiRunnableHandler.post(
        TAG,
        () ->
            journalStorage.commit(
                new JournalMutation.Builder(HEAD.getStreamToken())
                    .delete()
                    .append(new byte[0])
                    .build(),
                latch));
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
    return new FeedSessionMutation(
        feedSessionMutation -> commitSessionMutation(streamSession, feedSessionMutation));
  }

  @Override
  public SemanticPropertiesMutation editSemanticProperties() {
    return new FeedSemanticPropertiesMutation(this::commitSemanticPropertiesMutation);
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
    uiRunnableHandler.post(
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
    uiRunnableHandler.post(
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
    for (PayloadWithId mutation : mutations) {
      String payloadId = mutation.contentId;
      StreamPayload payload = mutation.payload;
      try {
        Builder contentMutationBuilder = new Builder();
        if (mutation.payload.hasStreamSharedState()) {
          StreamSharedState streamSharedState = mutation.payload.getStreamSharedState();
          contentMutationBuilder =
              contentMutationBuilder.upsert(
                  SHARED_STATE_PREFIX + streamSharedState.getContentId(),
                  streamSharedState.toByteArray());
        } else {
          contentMutationBuilder.upsert(payloadId, payload.toByteArray());
        }

        // Block waiting for the response from storage, to make this method synchronous.
        SimpleSettableFuture<CommitResult> commitResultFuture = new SimpleSettableFuture<>();
        Builder finalContentMutationBuilder = contentMutationBuilder;
        // TODO: handle errors
        uiRunnableHandler.post(
            TAG,
            () ->
                contentStorage.commit(
                    finalContentMutationBuilder.build(), commitResultFuture::put));
        commitResult = commitResultFuture.get();
      } catch (ExecutionException | InterruptedException e) {
        Logger.e(TAG, e, "Exception committing content mutation");
      }
    }

    tracker.stop("task", "commitContentMutation", "mutations", mutations.size());
    return commitResult;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    if (contentStorage instanceof Dumpable) {
      dumper.dump((Dumpable) contentStorage);
    }
    if (journalStorage instanceof Dumpable) {
      dumper.dump((Dumpable) journalStorage);
    }
  }
}

