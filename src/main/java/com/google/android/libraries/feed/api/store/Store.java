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

package com.google.android.libraries.feed.api.store;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.List;

/**
 * Interface defining the Store API used by the feed library to persist the internal proto objects
 * into a an underlying store. The store is divided into content and structure portions. Content is
 * retrieved using String representing the {@code ContentId}. The structure is defined through a set
 * of {@link StreamDataOperation} which define the structure of the stream. $HEAD defines the full
 * set of {@code StreamDataOperation}s defining the full stream. A {@link StreamSession} represents
 * an instance of the stream defined by a possible subset of $HEAD.
 */
public interface Store {
  /**
   * Returns an {@code List} of the {@link PayloadWithId}s for the ContentId Strings passed in
   * through {@code contentIds}.
   */
  List<PayloadWithId> getPayloads(List<String> contentIds);

  /** Get the {@link StreamSharedState}s stored as content. */
  List<StreamSharedState> getSharedStates();

  /**
   * Returns a list of all {@link StreamStructure}s for the session. The Stream Structure does not
   * contain the content. This represents the full structure of the stream for a particular session.
   */
  List<StreamStructure> getStreamStructures(StreamSession session);

  /**
   * Create a new session. The session is defined by all the {@link StreamDataOperation}s which are
   * currently stored in $HEAD.
   */
  StreamSession createNewSession();

  /** Returns the {@link StreamSession} for $HEAD. */
  StreamSession getHeadSession();

  /** Remove a specific session. It is illegal to attempt to remove $HEAD. */
  void removeSession(StreamSession session);

  /**
   * Clears out all data operations defining $HEAD. Only $HEAD supports reset, all other session may
   * not be reset. Instead they should be invalidated and removed, then a new session is created
   * from $HEAD.
   */
  void clearHead();

  /** Returns a mutation used to modify the content in the persistent store. */
  ContentMutation editContent();

  /** Returns a session mutation applied to a Session. */
  SessionMutation editSession(StreamSession streamSession);
}
