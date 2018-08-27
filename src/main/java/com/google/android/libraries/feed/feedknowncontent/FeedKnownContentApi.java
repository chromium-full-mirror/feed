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

package com.google.android.libraries.feed.feedknowncontent;

import com.google.android.libraries.feed.api.knowncontent.ContentMetadata;
import com.google.android.libraries.feed.api.knowncontent.ContentRemoval;
import com.google.android.libraries.feed.api.knowncontent.KnownContentApi;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.functional.Consumer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Default implementation of the {@link KnownContentApi}. */
public class FeedKnownContentApi implements KnownContentApi {

  private static final String TAG = "FeedKnownContentApi";
  private final SessionManager sessionManager;
  private final Set<KnownContentListener> listeners = new HashSet<>();
  private final KnownContentListener listener;

  public FeedKnownContentApi(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    listener =
        new KnownContentListener() {
          @Override
          public void onContentRemoved(List<ContentRemoval> contentRemoved) {
            for (KnownContentListener knownContentListener : listeners) {
              knownContentListener.onContentRemoved(contentRemoved);
            }
          }

          @Override
          public void onNewContentReceived(boolean isNewRefresh, long contentCreationDateTimeMs) {
            for (KnownContentListener knownContentListener : listeners) {
              knownContentListener.onNewContentReceived(isNewRefresh, contentCreationDateTimeMs);
            }
          }
        };
    sessionManager.setKnownContentListener(listener);
  }

  @Override
  public void getKnownContent(Consumer<List<ContentMetadata>> knownContentConsumer) {
    // TODO: Implement to provide host with knowledge of known content.
  }

  @Override
  public void addListener(KnownContentListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(KnownContentListener listener) {
    listeners.remove(listener);
  }

  @Override
  public KnownContentListener getKnownContentHostNotifier() {
    return listener;
  }
}
