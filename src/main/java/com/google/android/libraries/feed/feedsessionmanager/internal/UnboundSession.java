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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.common.logging.Dumpable;
import com.google.android.libraries.feed.common.logging.Dumper;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This is a session which is known by the FeedSessionManager but is not currently associated with a
 * ModelProvider.
 *
 * <p>TODO: We need to support getContentInSession and also update the journal when
 * updateSession is called.
 */
public class UnboundSession implements Session, Dumpable {
  private static final String TAG = "UnboundSession";

  private StreamSession streamSession;

  public UnboundSession(StreamSession streamSession) {
    this.streamSession = streamSession;
  }

  @Override
  public void updateSession(
      List<StreamStructure> streamStructures, /*@Nullable*/ StreamToken mutationSourceToken) {
    // TODO: Implement this
    Logger.i(TAG, "Attempt to update unbound session is ignored");
  }

  @Override
  public StreamSession getStreamSession() {
    return streamSession;
  }

  @Override
  public /*@Nullable*/ ModelProvider getModelProvider() {
    Logger.w(TAG, "UnboundSession doesn't have a ModelProvider, unexpected call");
    return null;
  }

  @Override
  public void updateAccessTime(long time) {
    streamSession = streamSession.toBuilder().setLastAccessed(time).build();
  }

  @Override
  public Collection<String> getContentInSession() {
    // TODO: Implement this
    return new ArrayList<>();
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("sessionName").value(streamSession.getStreamToken());
    dumper.forKey("accessTime").value(new Date(streamSession.getLastAccessed())).compactPrevious();
  }
}
