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

import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class implementing the {@link SessionMutation}. Committing the mutation is done through a
 * callback.
 */
public class SessionMutationImpl implements SessionMutation {
  /** Callback interface to commit a {@link SessionMutation}. */
  public interface SessionMutationCommitter {
    /** Method called to commit the session mutation */
    void commit(List<StreamDataOperation> dataOperations, StreamToken mutationSourceToken);
  }

  private final List<StreamDataOperation> dataOperations = new ArrayList<>();
  private final SessionMutationCommitter committer;
  @Nullable private StreamToken mutationSourceToken;

  public SessionMutationImpl(SessionMutationCommitter committer) {
    this.committer = committer;
  }

  @Override
  public void addOperation(StreamDataOperation dataOperation) {
    dataOperations.add(dataOperation);
  }

  @Override
  public void setMutationSourceToken(StreamToken mutationSourceToken) {
    this.mutationSourceToken = mutationSourceToken;
  }

  @Override
  public void commit() {
    committer.commit(dataOperations, mutationSourceToken);
  }
}
