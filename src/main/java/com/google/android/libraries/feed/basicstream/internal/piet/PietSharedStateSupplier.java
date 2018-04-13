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

package com.google.android.libraries.feed.basicstream.internal.piet;

import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Supplier;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.ui.piet.PietProto.PietSharedState;

/** Provides Piet shared states based on the a specified {@link ModelProvider}. */
public class PietSharedStateSupplier implements Supplier<PietSharedState> {

  private static final String TAG = "PietSharedStateSupplier";

  /*@Nullable*/ private ModelProvider modelProvider;

  @Override
  public PietSharedState get() {
    if (modelProvider == null) {
      Logger.w(TAG, "Attempt to get PietSharedState without a current ModelProvider defined.");
      return PietSharedState.getDefaultInstance();
    }
    StreamSharedState sharedState =
        modelProvider.getSharedState(SessionManager.GLOBAL_SHARED_STATE);
    if (sharedState == null) {
      Logger.w(TAG, "The Global Shared State was not defined");
      return PietSharedState.getDefaultInstance();
    }
    return sharedState.getPietSharedStateItem().getPietSharedState();
  }

  public void setModelProvider(ModelProvider modelProvider) {
    this.modelProvider = modelProvider;
  }
}
