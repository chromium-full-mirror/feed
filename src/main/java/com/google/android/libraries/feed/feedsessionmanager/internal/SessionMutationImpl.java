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
import com.google.android.libraries.feed.common.Committer;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing the {@link SessionMutation}. Committing the mutation is done through a
 * callback.
 */
public class SessionMutationImpl implements SessionMutation {
  /** Structure containing the change to the session manager. */
  public static class Change {
    public final List<StreamDataOperation> dataOperations = new ArrayList<>();
    /*@Nullable*/ public StreamToken mutationSourceToken;
  }

  private final Change change = new Change();
  private final Committer<Void, Change> committer;

  public SessionMutationImpl(Committer<Void, Change> committer) {
    this.committer = committer;
  }

  @Override
  public void addOperation(StreamDataOperation dataOperation) {
    change.dataOperations.add(dataOperation);
  }

  @Override
  public void setMutationContext(StreamToken mutationSourceToken) {
    change.mutationSourceToken = mutationSourceToken;
  }

  @Override
  public void commit() {
    committer.commit(change);
  }
}
