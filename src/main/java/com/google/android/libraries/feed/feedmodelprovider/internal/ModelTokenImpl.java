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

package com.google.android.libraries.feed.feedmodelprovider.internal;

import android.database.Observable;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenChangeObserver;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;

/** Implementation of the {@link ModelToken}. */
class ModelTokenImpl extends Observable<TokenChangeObserver> implements ModelToken {
  private final StreamToken token;

  ModelTokenImpl(StreamToken token) {
    this.token = token;
  }

  @Override
  public StreamToken getStreamToken() {
    return token;
  }

  @Override
  public List<TokenChangeObserver> getObserversToNotify() {
    synchronized (mObservers) {
      return new ArrayList<>(mObservers);
    }
  }
}
