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

package com.google.android.libraries.feed.feedstore.internal;

import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamPayloadWithId;
import java.util.ArrayList;
import java.util.List;

/** This class will mutate the Content stored in the FeedStore. */
public class FeedContentMutation implements ContentMutation {

  /**
   * Called when the Content Mutation is committed. The {@code FeedStore} will provide the
   * implementation of this callback to udpate the Content.
   */
  public interface CommitCallback {
    ContentMutationResults commitToStore(List<StreamPayloadWithId> mutations);
  }

  private final List<StreamPayloadWithId> mutations = new ArrayList<>();
  private final CommitCallback commitCallback;

  public FeedContentMutation(CommitCallback commitCallback) {
    this.commitCallback = commitCallback;
  }

  @Override
  public ContentMutation add(StreamContentId contentId, StreamPayload payload) {
    mutations.add(
        StreamPayloadWithId.newBuilder().setContentId(contentId).setPayload(payload).build());
    return this;
  }

  @Override
  public ContentMutationResults commit() {
    return commitCallback.commitToStore(mutations);
  }
}
