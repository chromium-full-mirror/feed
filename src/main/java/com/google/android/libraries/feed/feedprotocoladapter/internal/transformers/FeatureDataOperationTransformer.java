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

package com.google.android.libraries.feed.feedprotocoladapter.internal.transformers;

import com.google.android.libraries.feed.common.logging.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Cluster;
import com.google.search.now.ui.stream.StreamStructureProto.Stream;
import com.google.search.now.wire.feed.DataOperationProto.DataOperation;
import com.google.search.now.wire.feed.FeedResponseProto.FeedResponseMetadata;

/** {@link DataOperationTransformer} to populate {@link StreamFeature}. */
public class FeatureDataOperationTransformer implements DataOperationTransformer {

  private static final String TAG = "FeatureDataOperationTra";

  @Override
  public StreamDataOperation.Builder transform(
      DataOperation dataOperation,
      StreamDataOperation.Builder streamDataOperation,
      FeedResponseMetadata feedResponseMetadata) {
    StreamFeature.Builder streamFeature =
        streamDataOperation.getStreamPayload().getStreamFeature().toBuilder();
    switch (dataOperation.getFeature().getRenderableUnit()) {
      case STREAM:
        streamFeature.setStream(dataOperation.getFeature().getExtension(Stream.streamExtension));
        break;
      case CARD:
        streamFeature.setCard(dataOperation.getFeature().getExtension(Card.cardExtension));
        break;
      case CLUSTER:
        streamFeature.setCluster(dataOperation.getFeature().getExtension(Cluster.clusterExtension));
        break;
      case CONTENT:
        // Content is handled in ContentDataOperationTransformer
        break;
      case TOKEN:
        // Tokens are handled in FeedProtocolAdapter
        break;
      default:
        // TODO: This should be handled and we should return a result.
        Logger.e(TAG, "Unknown Feature payload %s", dataOperation.getFeature().getRenderableUnit());
        throw new RuntimeException("Unknown feature payload");
    }
    streamDataOperation.setStreamPayload(
        streamDataOperation.getStreamPayload().toBuilder().setStreamFeature(streamFeature));
    return streamDataOperation;
  }
}
