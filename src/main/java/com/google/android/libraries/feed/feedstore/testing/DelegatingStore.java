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

package com.google.android.libraries.feed.feedstore.testing;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.LocalActionMutation;
import com.google.android.libraries.feed.api.store.SemanticPropertiesMutation;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.api.store.StoreListener;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.search.now.feed.client.StreamDataProto.StreamLocalAction;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.List;
import java.util.Set;

/** Class which allows spying on a Store implementation */
public class DelegatingStore implements Store {
  private final Store store;

  public DelegatingStore(Store store) {
    this.store = store;
  }

  @Override
  public Result<List<PayloadWithId>> getPayloads(List<String> contentIds) {
    return store.getPayloads(contentIds);
  }

  @Override
  public Result<List<StreamSharedState>> getSharedStates() {
    return store.getSharedStates();
  }

  @Override
  public Result<List<StreamStructure>> getStreamStructures(StreamSession session) {
    return store.getStreamStructures(session);
  }

  @Override
  public Result<List<StreamSession>> getAllSessions() {
    return store.getAllSessions();
  }

  @Override
  public Result<List<SemanticPropertiesWithId>> getSemanticProperties(List<String> contentIds) {
    return store.getSemanticProperties(contentIds);
  }

  @Override
  public Result<List<StreamLocalAction>> getAllDismissLocalActions() {
    return store.getAllDismissLocalActions();
  }

  @Override
  public Result<StreamSession> createNewSession() {
    return store.createNewSession();
  }

  @Override
  public StreamSession getHeadSession() {
    return store.getHeadSession();
  }

  @Override
  public void removeSession(StreamSession session) {
    store.removeSession(session);
  }

  @Override
  public void clearHead() {
    store.clearHead();
  }

  @Override
  public ContentMutation editContent() {
    return store.editContent();
  }

  @Override
  public SessionMutation editSession(StreamSession streamSession) {
    return store.editSession(streamSession);
  }

  @Override
  public SemanticPropertiesMutation editSemanticProperties() {
    return store.editSemanticProperties();
  }

  @Override
  public LocalActionMutation editLocalActions() {
    return store.editLocalActions();
  }

  @Override
  public Runnable triggerContentGc(
      Set<String> reservedContentIds, Supplier<Set<String>> accessibleContent) {
    return store.triggerContentGc(reservedContentIds, accessibleContent);
  }

  @Override
  public Runnable triggerLocalActionGc(
      List<StreamLocalAction> actions, List<String> validContentIds) {
    return store.triggerLocalActionGc(actions, validContentIds);
  }

  @Override
  public void switchToEphemeralMode() {
    store.switchToEphemeralMode();
  }

  @Override
  public boolean isEphemeralMode() {
    return store.isEphemeralMode();
  }

  @Override
  public void registerObserver(StoreListener observer) {
    store.registerObserver(observer);
  }

  @Override
  public void unregisterObserver(StoreListener observer) {
    store.unregisterObserver(observer);
  }
}
