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

import android.database.Observable;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.ActionMutation;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.search.now.feed.client.StreamDataProto.StreamAction;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.List;
import java.util.Set;

/**
 * {@link Store} implementation that delegates between {@link PersistentFeedStore} and {@link
 * EphemeralFeedStore}.
 */
public class FeedStore extends Observable<FeedStoreListener> implements Store {
  // Permanent reference to the persistent store (used for setting off cleanup)
  private final Store persistentStore;
  // The current store
  private Store delegate;

  // Needed for switching to ephemeral mode
  private final Clock clock;
  private final FeedStoreHelper storeHelper;
  private final TimingUtils timingUtils;

  private boolean isEphemeralMode = false;

  public FeedStore(
      TimingUtils timingUtils,
      FeedExtensionRegistry extensionRegistry,
      ContentStorage contentStorage,
      JournalStorage journalStorage,
      ThreadUtils threadUtils,
      MainThreadRunner mainThreadRunner,
      Clock clock,
      FeedStoreHelper storeHelper) {
    this.clock = clock;
    this.storeHelper = storeHelper;
    this.timingUtils = timingUtils;
    this.persistentStore =
        new PersistentFeedStore(
            this.timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorage,
            threadUtils,
            mainThreadRunner,
            this.clock,
            this.storeHelper);
    delegate = persistentStore;
  }

  @Override
  public Result<List<PayloadWithId>> getPayloads(List<String> contentIds) {
    return delegate.getPayloads(contentIds);
  }

  @Override
  public Result<List<StreamSharedState>> getSharedStates() {
    return delegate.getSharedStates();
  }

  @Override
  public Result<List<StreamStructure>> getStreamStructures(StreamSession session) {
    return delegate.getStreamStructures(session);
  }

  @Override
  public Result<List<StreamSession>> getAllSessions() {
    return delegate.getAllSessions();
  }

  @Override
  public Result<List<SemanticPropertiesWithId>> getSemanticProperties(List<String> contentIds) {
    return delegate.getSemanticProperties(contentIds);
  }

  @Override
  public Result<List<StreamAction>> getAllDismissActions() {
    return delegate.getAllDismissActions();
  }

  @Override
  public Result<StreamSession> createNewSession() {
    return delegate.createNewSession();
  }

  @Override
  public StreamSession getHeadSession() {
    return delegate.getHeadSession();
  }

  @Override
  public void removeSession(StreamSession session) {
    delegate.removeSession(session);
  }

  @Override
  public void clearHead() {
    delegate.clearHead();
  }

  @Override
  public ContentMutation editContent() {
    return delegate.editContent();
  }

  @Override
  public SessionMutation editSession(StreamSession streamSession) {
    return delegate.editSession(streamSession);
  }

  @Override
  public SemanticPropertiesMutation editSemanticProperties() {
    return delegate.editSemanticProperties();
  }

  @Override
  public ActionMutation editActions() {
    return delegate.editActions();
  }

  @Override
  public Runnable triggerContentGc(
      Set<String> reservedContentIds, Supplier<Set<String>> accessibleContent) {
    return delegate.triggerContentGc(reservedContentIds, accessibleContent);
  }

  @Override
  public Runnable triggerActionGc(List<StreamAction> actions, List<String> validContentIds) {
    return delegate.triggerActionGc(actions, validContentIds);
  }

  @Override
  public boolean isEphemeralMode() {
    return isEphemeralMode;
  }

  @Override
  public void switchToEphemeralMode() {
    if (!isEphemeralMode) {
      persistentStore.switchToEphemeralMode();
      delegate = new EphemeralFeedStore(clock, timingUtils, storeHelper);
      isEphemeralMode = true;
      synchronized (mObservers) {
        for (FeedStoreListener listener : mObservers) {
          listener.onSwitchToEphemeralMode();
        }
      }
    }
  }
}
