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

package com.google.android.libraries.feed.testing.modelprovider;

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelMutation;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.common.collect.ImmutableList;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.UiContext;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fake for tests using {@link ModelProvider}. Functionality should be added to this class as
 * needed.
 */
public class FakeModelProvider implements ModelProvider {

  private final Set<ModelProviderObserver> observers = new HashSet<>();
  private ModelFeature rootFeature;
  private boolean wasRefreshTriggered;
  private boolean tokensEnabled = true;
  /*@Nullable*/ private String sessionId = null;
  @RequestReason private int lastRequestReason = RequestReason.UNKNOWN;
  /*@Nullable*/ private ModelFeature immediateSessionStartModel;
  /*@Nullable*/ private UiContext unusedTriggerRefreshUiContext;

  @Override
  public ModelMutation edit() {
    throw new UnsupportedOperationException("Edit is currently unsupported.");
  }

  @Override
  public void invalidate() {}

  @Override
  public void invalidate(UiContext uiContext) {}

  @Override
  public void detachModelProvider() {}

  @Override
  public void raiseError(ModelError error) {}

  @Override
  /*@Nullable*/
  public ModelFeature getRootFeature() {
    return rootFeature;
  }

  @Override
  /*@Nullable*/
  public ModelChild getModelChild(String contentId) {
    return null;
  }

  @Override
  /*@Nullable*/
  public StreamSharedState getSharedState(ContentId contentId) {
    return null;
  }

  @Override
  public boolean handleToken(ModelToken modelToken) {
    return tokensEnabled;
  }

  @Override
  public void triggerRefresh(@RequestReason int requestReason) {
    wasRefreshTriggered = true;
    lastRequestReason = requestReason;
  }

  @Override
  public void triggerRefresh(@RequestReason int requestReason, UiContext uiContext) {
    wasRefreshTriggered = true;
    lastRequestReason = requestReason;
    unusedTriggerRefreshUiContext = uiContext;
  }

  @Override
  public int getCurrentState() {
    return State.INITIALIZING;
  }

  @Override
  /*@Nullable*/
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public List<ModelChild> getAllRootChildren() {
    ImmutableList.Builder<ModelChild> listBuilder = ImmutableList.builder();

    ModelCursor cursor = rootFeature.getCursor();
    ModelChild child;
    while ((child = cursor.getNextItem()) != null) {
      listBuilder.add(child);
    }
    return listBuilder.build();
  }

  @Override
  public void enableRemoveTracking(RemoveTrackingFactory<?> removeTrackingFactory) {}

  @Override
  public void registerObserver(ModelProviderObserver observer) {
    if (immediateSessionStartModel != null) {
      this.rootFeature = immediateSessionStartModel;
      observer.onSessionStart();
    }

    observers.add(observer);
  }

  @Override
  public void unregisterObserver(ModelProviderObserver observer) {
    observers.remove(observer);
  }

  public void triggerOnSessionStart(ModelFeature rootFeature) {
    this.rootFeature = rootFeature;
    for (ModelProviderObserver observer : observers) {
      observer.onSessionStart();
    }
  }

  public void triggerOnSessionFinished() {
    triggerOnSessionFinished(UiContext.getDefaultInstance());
  }

  public void triggerOnSessionFinished(UiContext uiContext) {
    for (ModelProviderObserver observer : observers) {
      observer.onSessionFinished(uiContext);
    }
  }

  public void triggerOnError(ModelError modelError) {
    for (ModelProviderObserver observer : observers) {
      observer.onError(modelError);
    }
  }

  public boolean wasRefreshTriggered() {
    return wasRefreshTriggered;
  }

  public Set<ModelProviderObserver> getObservers() {
    return observers;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @RequestReason
  public int getLastRequestReason() {
    return lastRequestReason;
  }

  public void triggerOnSessionStartImmediately(ModelFeature modelFeature) {
    immediateSessionStartModel = modelFeature;
  }

  public void setTokensEnabled(boolean value) {
    tokensEnabled = value;
  }
}
