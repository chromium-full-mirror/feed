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

package com.google.android.libraries.feed.api.common.testing;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import java.util.ArrayList;
import java.util.List;

/** This is a builder class for creating internal protocol elements. */
public class InternalProtocolBuilder {
  public static final StreamContentId ROOT_CONTENT_ID =
      StreamContentId.newBuilder()
          .setContentDomain("stream_root")
          .setId(0)
          .setTable("feature")
          .build();

  private List<StreamDataOperation> dataOperations = new ArrayList<>();

  /** This adds a Root Feature into the Stream of data operations */
  public InternalProtocolBuilder addRootFeature() {
    StreamFeature streamFeature = StreamFeature.newBuilder().setContentId(ROOT_CONTENT_ID).build();
    StreamStructure streamStructure =
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(ROOT_CONTENT_ID)
            .build();
    StreamPayload streamPayload =
        StreamPayload.newBuilder().setStreamFeature(streamFeature).build();
    dataOperations.add(
        StreamDataOperation.newBuilder()
            .setStreamStructure(streamStructure)
            .setStreamPayload(streamPayload)
            .build());
    return this;
  }

  public InternalProtocolBuilder addClearOperation() {
    StreamStructure streamStructure =
        StreamStructure.newBuilder().setOperation(Operation.CLEAR_ALL).build();
    dataOperations.add(
        StreamDataOperation.newBuilder().setStreamStructure(streamStructure).build());
    return this;
  }

  public InternalProtocolBuilder addSharedState(StreamContentId contentId) {
    StreamSharedState streamSharedState =
        StreamSharedState.newBuilder().setContentId(contentId).build();
    StreamStructure streamStructure =
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(contentId)
            .build();
    StreamPayload streamPayload =
        StreamPayload.newBuilder().setStreamSharedState(streamSharedState).build();
    dataOperations.add(
        StreamDataOperation.newBuilder()
            .setStreamStructure(streamStructure)
            .setStreamPayload(streamPayload)
            .build());
    return this;
  }

  public InternalProtocolBuilder addToken(StreamContentId contentId) {
    StreamToken streamToken = StreamToken.newBuilder().setContentId(contentId).build();
    StreamStructure streamStructure =
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(contentId)
            .build();
    StreamPayload streamPayload = StreamPayload.newBuilder().setStreamToken(streamToken).build();
    dataOperations.add(
        StreamDataOperation.newBuilder()
            .setStreamStructure(streamStructure)
            .setStreamPayload(streamPayload)
            .build());
    return this;
  }

  public InternalProtocolBuilder addFeature(StreamContentId contentId, StreamContentId parentId) {
    StreamFeature streamFeature =
        StreamFeature.newBuilder().setContentId(contentId).setParentId(parentId).build();
    StreamStructure streamStructure =
        StreamStructure.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setContentId(contentId)
            .setParentContentId(parentId)
            .build();
    StreamPayload streamPayload =
        StreamPayload.newBuilder().setStreamFeature(streamFeature).build();
    dataOperations.add(
        StreamDataOperation.newBuilder()
            .setStreamStructure(streamStructure)
            .setStreamPayload(streamPayload)
            .build());
    return this;
  }

  public InternalProtocolBuilder removeFeature(
      StreamContentId contentId, StreamContentId parentId) {
    StreamStructure streamStructure =
        StreamStructure.newBuilder()
            .setOperation(Operation.REMOVE)
            .setContentId(contentId)
            .setParentContentId(parentId)
            .build();
    dataOperations.add(
        StreamDataOperation.newBuilder().setStreamStructure(streamStructure).build());
    return this;
  }

  public List<StreamDataOperation> build() {
    return new ArrayList<>(dataOperations);
  }

  /**
   * This will return the StreamStructure part only, filtering out any shared state. This is the
   * form structure is represented in the Store.
   */
  public List<StreamStructure> buildAsStreamStructure() {
    List<StreamStructure> streamStructures = new ArrayList<>();
    for (StreamDataOperation operation : dataOperations) {
      // For the structure, ignore the shared state
      if (operation.hasStreamPayload() && operation.getStreamPayload().hasStreamSharedState()) {
        continue;
      }
      streamStructures.add(operation.getStreamStructure());
    }
    return streamStructures;
  }
}
