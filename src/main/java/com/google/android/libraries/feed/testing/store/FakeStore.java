// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.testing.store;

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.testing.FakeThreadUtils;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.internalapi.common.PayloadWithId;
import com.google.android.libraries.feed.internalapi.store.ContentMutation;
import com.google.android.libraries.feed.internalapi.store.SessionMutation;
import com.google.android.libraries.feed.internalapi.store.UploadableActionMutation;
import com.google.android.libraries.feed.testing.host.logging.FakeBasicLoggingApi;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fake implementation of {@link com.google.android.libraries.feed.internalapi.store.Store}. */
public final class FakeStore extends FeedStore {
  private final FakeThreadUtils fakeThreadUtils;
  private boolean allowCreateNewSession = true;
  private boolean allowGetPayloads = true;
  private boolean allowGetStreamStructures = true;
  private boolean allowGetSharedStates = true;

  public FakeStore(FakeThreadUtils fakeThreadUtils, TaskQueue taskQueue, Clock clock) {
    super(
        new TimingUtils(),
        new FeedExtensionRegistry(ArrayList::new),
        new InMemoryContentStorage(),
        new InMemoryJournalStorage(),
        fakeThreadUtils,
        taskQueue,
        clock,
        new FakeBasicLoggingApi(),
        FakeMainThreadRunner.runTasksImmediately());
    this.fakeThreadUtils = fakeThreadUtils;
  }

  @Override
  public Result<List<PayloadWithId>> getPayloads(List<String> contentIds) {
    if (!allowGetPayloads) {
      return Result.failure();
    }

    return super.getPayloads(contentIds);
  }

  @Override
  public Result<List<StreamStructure>> getStreamStructures(String sessionId) {
    if (!allowGetStreamStructures) {
      return Result.failure();
    }

    return super.getStreamStructures(sessionId);
  }

  @Override
  public Result<String> createNewSession() {
    if (!allowCreateNewSession) {
      return Result.failure();
    }

    return super.createNewSession();
  }

  @Override
  public Result<List<StreamSharedState>> getSharedStates() {
    if (!allowGetSharedStates) {
      return Result.failure();
    }

    return super.getSharedStates();
  }

  /** Sets whether to fail on calls to {@link getStreamStructures(String)}. */
  public FakeStore setAllowGetStreamStructures(boolean value) {
    allowGetStreamStructures = value;
    return this;
  }

  /** Sets whether to fail on calls to {@link createNewSession()}. */
  public FakeStore setAllowCreateNewSession(boolean value) {
    allowCreateNewSession = value;
    return this;
  }

  /** Sets whether to fail on calls to {@link getPayloads(List)}. */
  public FakeStore setAllowGetPayloads(boolean value) {
    allowGetPayloads = value;
    return this;
  }

  /** Sets whether to fail on calls to {@link getSharedStates()}. */
  public FakeStore setAllowGetSharedStates(boolean value) {
    allowGetSharedStates = value;
    return this;
  }

  /** Adds the {@code payload} to the store. */
  public FakeStore setContent(String contentId, StreamPayload payload) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    editContent().add(contentId, payload).commit();
    fakeThreadUtils.enforceMainThread(policy);
    return this;
  }

  /** Adds the {@code payloads} to the store. */
  public FakeStore setContent(List<PayloadWithId> payloads) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    ContentMutation mutation = editContent();
    for (PayloadWithId payload : payloads) {
      mutation.add(payload.contentId, payload.payload);
    }
    mutation.commit();
    fakeThreadUtils.enforceMainThread(policy);
    return this;
  }

  /** Adds the {@code sharedStates} to the store. */
  public FakeStore setSharedStates(StreamSharedState... sharedStates) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    ContentMutation mutation = editContent();
    for (StreamSharedState sharedState : sharedStates) {
      mutation.add(
          sharedState.getContentId(),
          StreamPayload.newBuilder().setStreamSharedState(sharedState).build());
    }
    mutation.commit();
    fakeThreadUtils.enforceMainThread(policy);
    return this;
  }

  /** Adds the {@code structures} to the store for the specified {@code sessionId}. */
  public FakeStore setStreamStructures(String sessionId, StreamStructure... structures) {
    return setStreamStructures(sessionId, Arrays.asList(structures));
  }

  /** Adds the {@code structures} to the store for the specified {@code sessionId}. */
  public FakeStore setStreamStructures(String sessionId, List<StreamStructure> structures) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    SessionMutation mutation = editSession(sessionId);
    for (StreamStructure structure : structures) {
      mutation.add(structure);
    }
    mutation.commit();
    fakeThreadUtils.enforceMainThread(policy);
    return this;
  }

  /** Adds the {@code actions} to the store. */
  public FakeStore setStreamUploadableActions(StreamUploadableAction... actions) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    UploadableActionMutation mutation = editUploadableActions();
    for (StreamUploadableAction action : actions) {
      mutation.upsert(action, action.getFeatureContentId());
    }
    mutation.commit();
    fakeThreadUtils.enforceMainThread(policy);
    return this;
  }

  /** Gets all content associated with the {@code contentId}. */
  public List<Object> getContentById(String contentId) {
    boolean policy = fakeThreadUtils.enforceMainThread(false);
    ArrayList<String> contentIds = new ArrayList<>(1);
    contentIds.add(contentId);
    ArrayList<Object> result = new ArrayList<>();
    result.addAll(getPayloads(contentIds).getValue());
    result.addAll(getActionProperties(contentIds).getValue());
    result.addAll(getSemanticProperties(contentIds).getValue());
    for (StreamUploadableAction action : getAllUploadableActions().getValue()) {
      if (action.getFeatureContentId().equals(contentId)) {
        result.add(action);
      }
    }
    fakeThreadUtils.enforceMainThread(policy);
    return result;
  }
}
