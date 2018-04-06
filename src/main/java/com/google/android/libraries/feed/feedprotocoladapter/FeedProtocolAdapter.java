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

package com.google.android.libraries.feed.feedprotocoladapter;

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.sessionmanager.SessionMutation;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Cluster;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.Stream;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.DataOperationProto.DataOperation;
import com.google.search.now.wire.feed.FeedResponseProto.FeedResponse;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A ProtocolAdapter which converts from the wire protocol to the internal protocol. This is written
 * for the Feed Library implementation.
 *
 * <p>NOTE: This is prototype code. It still uses the FeedStream/FeedCard representation of the wire
 * feed.
 */
public class FeedProtocolAdapter implements ProtocolAdapter, Dumpable {

  private static final String TAG = "FeedProtocolAdapter";

  private final SessionManager sessionManager;
  private final TimingUtils timingUtils;
  private int nextTokenId = 1;

  @VisibleForTesting Map<Integer, StreamToken> streamTokens = new HashMap<>();

  // Operation counts for #dump(Dumpable)
  private int responseHandlingCount = 0;
  private int convertContentIdCount = 0;

  public FeedProtocolAdapter(SessionManager sessionManager, TimingUtils timingUtils) {
    this.sessionManager = sessionManager;
    this.timingUtils = timingUtils;
  }

  /** Create the internal protocol based upon the FeedStream definition. */
  @Override
  public void createModel(Response response, /*@Nullable*/ ByteString pageTokenForMutation) {
    ElapsedTimeTracker totalTimeTracker = timingUtils.getElapsedTimeTracker(TAG);
    responseHandlingCount++;

    SessionMutation sessionMutation = sessionManager.edit();
    if (pageTokenForMutation != null) {
      StreamToken token = streamTokens.remove(pageTokenForMutation.hashCode());
      if (token == null) {
        // TODO: What do we do about this failure?
        Logger.e(TAG, "Didn't find the StreamToken");
      } else {
        sessionMutation.setMutationSourceToken(token);
      }
    }
    List<DataOperation> dataOperations =
        response.getExtension(FeedResponse.feedResponse).getDataOperationList();
    for (DataOperation operation : dataOperations) {
      // The operations defined in stream_data.proto and data_operation.proto have the same
      // integer value
      Operation streamOperation = Operation.forNumber(operation.getOperation().getNumber());
      String contentId;
      if (streamOperation == Operation.CLEAR_ALL) {
        sessionMutation.addOperation(createDataOperation(Operation.CLEAR_ALL, null, null).build());
        continue;
      } else if (streamOperation == Operation.REMOVE) {
        if (operation.getMetadata().hasContentId()) {
          contentId = createContentId(operation.getMetadata().getContentId());
          String parentId = null;
          if (operation.getFeature().hasParentId()) {
            parentId = createContentId(operation.getFeature().getParentId());
          }
          sessionMutation.addOperation(
              createDataOperation(Operation.REMOVE, contentId, parentId).build());

        } else {
          Logger.w(TAG, "REMOVE defined without a ContentId identifying the item to remove");
        }
        continue;
      } else if (operation.getMetadata().hasContentId()) {
        contentId = createContentId(operation.getMetadata().getContentId());
      } else {
        // This is an error state, every card should have a content id
        Logger.e(TAG, "ContentId not defined for DataOperation");
        continue;
      }

      if (operation.hasFeature()) {
        StreamFeature.Builder streamFeature = StreamFeature.newBuilder();
        streamFeature.setContentId(contentId);
        String parentId = null;
        if (operation.getFeature().hasParentId()) {
          parentId = createContentId(operation.getFeature().getParentId());
          streamFeature.setParentId(parentId);
        }
        switch (operation.getFeature().getRenderableUnit()) {
          case STREAM:
            streamFeature.setStream(operation.getFeature().getExtension(Stream.streamExtension));
            break;
          case CARD:
            streamFeature.setCard(operation.getFeature().getExtension(Card.cardExtension));
            break;
          case CONTENT:
            streamFeature.setContent(operation.getFeature().getExtension(Content.contentExtension));
            break;
          case CLUSTER:
            streamFeature.setCluster(operation.getFeature().getExtension(Cluster.clusterExtension));
            break;
          default:
            throw new RuntimeException("Unknown feature payload");
        }
        sessionMutation.addOperation(
            createFeatureDataOperation(streamOperation, contentId, parentId, streamFeature.build())
                .build());
      } else if (operation.hasPietSharedState()) {
        PietSharedStateItem item =
            PietSharedStateItem.newBuilder()
                .setPietSharedState(operation.getPietSharedState())
                .build();
        StreamSharedState state =
            StreamSharedState.newBuilder()
                .setPietSharedStateItem(item)
                .setContentId(contentId)
                .build();
        sessionMutation.addOperation(
            createSharedStateDataOperation(streamOperation, contentId, state).build());
      }
    }

    // if there is a continuation token it should be the last thing added to the root.
    ByteString continuationToken =
        response.getExtension(FeedResponse.feedResponse).getNextPageToken();
    if (continuationToken != null && !continuationToken.isEmpty()) {
      String contentId =
          createContentId(
              ContentId.newBuilder()
                  .setContentDomain("token")
                  .setId(nextTokenId++)
                  .setTable("feature")
                  .build());
      StreamToken streamToken =
          Validators.checkNotNull(createStreamToken(contentId, continuationToken));
      streamTokens.put(continuationToken.hashCode(), streamToken);
      sessionMutation.addOperation(
          createTokenDataOperation(contentId, streamToken.getParentId(), streamToken).build());
    }
    sessionMutation.commit();
    totalTimeTracker.stop("", "convertWireProtocol", "mutations", dataOperations.size());
  }

  /*@Nullable*/
  private StreamToken createStreamToken(String tokenId, ByteString continuationToken) {
    if (continuationToken.isEmpty()) {
      return null;
    }
    StreamToken.Builder tokenBuilder = StreamToken.newBuilder();
    String rootContentId =
        createContentId(
            ContentId.newBuilder()
                .setContentDomain("stream_root")
                .setId(0)
                .setTable("feature")
                .build());
    tokenBuilder.setParentId(rootContentId);
    tokenBuilder.setContentId(tokenId);
    tokenBuilder.setNextPageToken(continuationToken);
    return tokenBuilder.build();
  }

  @Override
  public String getStreamContentId(ContentId contentId) {
    convertContentIdCount++;
    return createContentId(contentId);
  }

  private StreamDataOperation.Builder createFeatureDataOperation(
      Operation operation,
      /*@Nullable*/ String contentId,
      /*@Nullable*/ String parentId,
      StreamFeature streamFeature) {
    StreamDataOperation.Builder dataOperation = createDataOperation(operation, contentId, parentId);
    dataOperation.setStreamPayload(StreamPayload.newBuilder().setStreamFeature(streamFeature));
    return dataOperation;
  }

  private StreamDataOperation.Builder createSharedStateDataOperation(
      Operation operation, /*@Nullable*/ String contentId, StreamSharedState sharedState) {
    StreamDataOperation.Builder dataOperation = createDataOperation(operation, contentId, null);
    dataOperation.setStreamPayload(StreamPayload.newBuilder().setStreamSharedState(sharedState));
    return dataOperation;
  }

  private StreamDataOperation.Builder createTokenDataOperation(
      /*@Nullable*/ String contentId, /*@Nullable*/ String parentId, StreamToken streamToken) {
    StreamDataOperation.Builder dataOperation =
        createDataOperation(Operation.UPDATE_OR_APPEND, contentId, parentId);
    dataOperation.setStreamPayload(StreamPayload.newBuilder().setStreamToken(streamToken));
    return dataOperation;
  }

  private StreamDataOperation.Builder createDataOperation(
      Operation operation, /*@Nullable*/ String contentId, /*@Nullable*/ String parentId) {
    StreamDataOperation.Builder streamDataOperation = StreamDataOperation.newBuilder();
    StreamStructure.Builder streamStructure = StreamStructure.newBuilder();
    streamStructure.setOperation(operation);
    if (contentId != null) {
      streamStructure.setContentId(contentId);
    }
    if (parentId != null) {
      streamStructure.setParentContentId(parentId);
    }
    streamDataOperation.setStreamStructure(streamStructure);
    return streamDataOperation;
  }

  private String createContentId(ContentId contentId) {
    // Using String concat for performance reasons.  This is called a lot for large feed responses.
    return contentId.getTable() + "::" + contentId.getContentDomain() + "::" + contentId.getId();
  }

  @Override
  public void dump(Dumper dumper) {
    dumper.title(TAG);
    dumper.forKey("responseHandlingCount").value(responseHandlingCount);
    dumper.forKey("convertContentIdCount").value(convertContentIdCount).compactPrevious();
  }
}
