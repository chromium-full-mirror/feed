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
import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.ContentMutation.ContentMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.SessionMutation.SessionMutationResults;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedstore.internal.FeedContentMutation;
import com.google.android.libraries.feed.feedstore.internal.FeedSessionMutation;
import com.google.android.libraries.feed.feedstore.internal.SessionState;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.host.storage.InMemoryKeyValuePairStorage;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.StorageMutation.Builder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of the Store. The FeedStore will call the host APIs {@link
 * InMemoryKeyValuePairStorage} and {@link InMemoryJournalStorage} to make persistent changes.
 */
public class FeedStore implements Store, Dumpable {

  private static final String TAG = "FeedStore";
  private static final StreamSession HEAD = new StreamSession("$HEAD");

  private final TimingUtils timingUtils;
  private final FeedExtensionRegistry extensionRegistry;
  private final InMemoryKeyValuePairStorage payloadStorage = new InMemoryKeyValuePairStorage();
  private final InMemoryJournalStorage sessionStorage = new InMemoryJournalStorage();

  // TODO: We need to synchronize access to these states
  private int sessionId = 1;

  // TODO: This should be some type of prefix search against the Content?
  private final Map<String, StreamSharedState> sharedStates = new HashMap<>();

  public FeedStore(
      TimingUtils timingUtils,
      FeedExtensionRegistry extensionRegistry) {
    this.timingUtils = timingUtils;
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public List<PayloadWithId> getPayloads(List<String> contentIds) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<PayloadWithId> payloads = new ArrayList<>();
    for (String contentId : contentIds) {
      try {
        byte[] content = payloadStorage.get(contentId).get();
        if (content != null) {
          StreamPayload streamPayload =
              StreamPayload.parseFrom(content, extensionRegistry.getExtensionRegistry());
          payloads.add(new PayloadWithId(contentId, streamPayload));
        } else {
          Logger.e(TAG, "Didn't find Content for %s", contentId);
        }
      } catch (InvalidProtocolBufferException e) {
        Logger.e(TAG, "Couldn't parse content proto for %s", contentId);
      } catch (ExecutionException | InterruptedException e) {
        Logger.e(TAG, e, "Exception getting payload for %s", contentId);
      }
    }
    tracker.stop("", "getPayloads", "items", contentIds.size());
    return payloads;
  }

  @Override
  public List<StreamSharedState> getSharedStates() {
    return new ArrayList<>(sharedStates.values());
  }

  @Override
  public List<StreamStructure> getStreamStructures(StreamSession session) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    List<StreamStructure> streamStructures = new ArrayList<>();
    try {
      List<byte[]> operations = sessionStorage.read(session.streamToken).get();
      for (byte[] bytes : operations) {
        streamStructures.add(StreamStructure.parseFrom(bytes));
      }
    } catch (InvalidProtocolBufferException | InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Couldn't read stream structures", e);
    }
    tracker.stop("", "getStreamStructures", "items", streamStructures.size());
    return streamStructures;
  }

  @Override
  public StreamSession createNewSession() {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    int newSessionId = sessionId++;
    StreamSession streamSession = new StreamSession("_session:" + newSessionId);
    try {
      SessionState session = new SessionState(streamSession);
      session.addAll(getStreamStructures(HEAD));
      sessionStorage
          .commit(
              new JournalMutation.Builder(HEAD.streamToken).copy(streamSession.streamToken).build())
          .get();
    } catch (InterruptedException | ExecutionException e) {
      Logger.e(TAG, "Unable to write new session %s", newSessionId);
      throw new IllegalStateException("Unable to write new session", e);
    }
    tracker.stop("createNewSession", streamSession.streamToken);
    return streamSession;
  }

  @Override
  public StreamSession getHeadSession() {
    return HEAD;
  }

  @Override
  public void removeSession(StreamSession session) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    if (session.streamToken.equals(HEAD.streamToken)) {
      // TODO: What type of exception should this throw?
      throw new IllegalStateException("Unable to delete the $HEAD session");
    }
    try {
      sessionStorage
          .commit(new JournalMutation.Builder(session.streamToken).delete().build())
          .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Unable to delete session", e);
    }
    tracker.stop("removeSession", session.streamToken);
  }

  @Override
  public void clearHead() {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    try {
      sessionStorage.commit(new JournalMutation.Builder(HEAD.streamToken).delete().build()).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Unable to clear $HEAD");
    }
    tracker.stop("", "clearHead");
  }

  @Override
  public ContentMutation editContent() {
    return new FeedContentMutation(this::commitContentMutation);
  }

  @Override
  public SessionMutation editSession(StreamSession streamSession) {
    return new FeedSessionMutation(
        feedSessionMutation -> commitSessionMutation(streamSession, feedSessionMutation));
  }

  private SessionMutationResults commitSessionMutation(
      StreamSession streamSession, List<StreamStructure> streamStructures) {
    if (streamStructures.isEmpty()) {
      Logger.i(TAG, "No StreamStructures found to add");
      return new SessionResults(CommitResult.SUCCESS);
    }

    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);
    JournalMutation.Builder mutation = new JournalMutation.Builder(streamSession.streamToken);
    for (StreamStructure streamStructure : streamStructures) {
      mutation.append(streamStructure.toByteArray());
    }
    CommitResult result;
    try {
      result = sessionStorage.commit(mutation.build()).get();
    } catch (InterruptedException | ExecutionException e) {
      tracker.stop("", "commitSessionMutationFailure");
      throw new IllegalStateException("Unable to mutate session", e);
    }
    tracker.stop("", "commitSessionMutation", "mutations", streamStructures.size());
    Logger.i(
        TAG,
        "commitSessionMutation - Success %s, Update Session %s, stream structures %s",
        result == CommitResult.SUCCESS,
        streamSession.streamToken,
        streamStructures.size());
    return new SessionResults(result);
  }

  private ContentMutationResults commitContentMutation(List<PayloadWithId> mutations) {
    ElapsedTimeTracker tracker = timingUtils.getElapsedTimeTracker(TAG);

    List<String> contentAdded = new ArrayList<>();
    List<String> contentUpdated = new ArrayList<>();
    List<String> sharedStateAdded = new ArrayList<>();
    for (PayloadWithId mutation : mutations) {
      String payloadId = mutation.contentId;
      StreamPayload payload = mutation.payload;
      if (mutation.payload.hasStreamSharedState()) {
        StreamSharedState prevValue =
            sharedStates.put(mutation.contentId, payload.getStreamSharedState());
        // TODO: Do we need to track SharedState Updates?
        if (prevValue == null) {
          sharedStateAdded.add(mutation.contentId);
        }
      } else {
        // Block waiting for the response from storage, to make this method synchronous.
        try {
          // Check whether the payload is already in storage.
          byte[] payloadBytes = payloadStorage.get(payloadId).get();
          CommitResult commitResult =
              payloadStorage
                  .commit(new Builder().upsert(payloadId, payload.toByteArray()).build())
                  .get();
          if (commitResult == CommitResult.SUCCESS) {
            if (payloadBytes != null) {
              contentUpdated.add(payloadId);
            } else {
              contentAdded.add(payloadId);
            }
          }
        } catch (ExecutionException | InterruptedException e) {
          Logger.e(TAG, e, "Exception committing content mutation");
        }
      }
    }

    StoreResults results = new StoreResults(contentAdded, contentUpdated, sharedStateAdded);
    tracker.stop("", "commitContentMutation", "mutations", mutations.size());
    return results;
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("nextSessionId").value(sessionId);
    dumper.dump(payloadStorage);
    dumper.dump(sessionStorage);
  }

  private static class StoreResults implements ContentMutationResults {
    private final List<String> contentAdded;
    private final List<String> contentUpdated;
    private final List<String> sharedStateAdded;

    StoreResults(
        List<String> contentAdded, List<String> contentUpdated, List<String> sharedStateAdded) {
      this.contentAdded = contentAdded;
      this.contentUpdated = contentUpdated;
      this.sharedStateAdded = sharedStateAdded;
    }

    @Override
    public String toString() {
      return String.format(
          "ContentMutationResults - Added %s, Updated %s, Shared State Added %s",
          contentAdded.size(), contentUpdated.size(), sharedStateAdded.size());
    }

    @Override
    public List<String> getContentAdded() {
      return contentAdded;
    }

    @Override
    public List<String> getContentUpdated() {
      return contentUpdated;
    }

    @Override
    public List<String> getSharedStateAdded() {
      return sharedStateAdded;
    }
  }

  private static class SessionResults implements SessionMutationResults {
    private final Status status;

    SessionResults(CommitResult commitResult) {
      switch (commitResult) {
        case FAILURE:
          status = Status.FAILURE;
          break;
        case SUCCESS:
          status = Status.SUCCESS;
          break;
        default:
          Logger.e(TAG, "Unknown CommitResult %s", commitResult);
          status = Status.FAILURE;
      }
    }

    @Override
    public Status getStatus() {
      return status;
    }
  }
}
