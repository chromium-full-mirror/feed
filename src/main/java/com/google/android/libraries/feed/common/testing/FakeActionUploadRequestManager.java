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

package com.google.android.libraries.feed.common.testing;

import com.google.android.libraries.feed.api.requestmanager.ActionUploadRequestManager;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

/** Fake implementation of a {@link ActionUploadRequestManager}. */
public class FakeActionUploadRequestManager implements ActionUploadRequestManager {
  private final long requestDelayMs;
  private final Queue<Response> responses = new ArrayDeque<>();

  public FakeActionUploadRequestManager(long requestDelayMs) {

    this.requestDelayMs = requestDelayMs;
  }

  // TODO: queue responses for action uploads
  /** Adds a Response to the Queue. */
  public void queueResponse(Response response) {
    responses.add(response);
  }

  @Override
  public void triggerUploadActions(
      Set<StreamUploadableAction> actions,
      ConsistencyToken token,
      Consumer<Result<ConsistencyToken>> consumer) {
    if (requestDelayMs > 0) {
      try {
        // slightly delay the results.
        Thread.sleep(requestDelayMs);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
    consumer.accept(Result.success(ConsistencyToken.getDefaultInstance()));
  }
}
