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

import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.DataOperationProto.DataOperation;
import com.google.search.now.wire.feed.DataOperationProto.DataOperation.Operation;
import com.google.search.now.wire.feed.FeatureProto.Feature;
import com.google.search.now.wire.feed.FeatureProto.Feature.RenderableUnit;
import com.google.search.now.wire.feed.FeedResponseProto.FeedResponse;
import com.google.search.now.wire.feed.PayloadMetadataProto.PayloadMetadata;
import com.google.search.now.wire.feed.ResponseProto.Response;

/** Builder making creation of wire protocol Response object easier. */
public class WireProtocolResponseBuilder {
  public static final ContentId ROOT_CONTENT_ID =
      ContentId.newBuilder().setContentDomain("root-feature").setId(1).setTable("feature").build();

  private final WireProtocolInfo wireProtocolInfo = new WireProtocolInfo();
  private final FeedResponse.Builder feedResponseBuilder = FeedResponse.newBuilder();

  /** Add a CLEAR_ALL data operation to the response */
  public WireProtocolResponseBuilder addClearOperation() {
    wireProtocolInfo.hasClearOperation = true;
    feedResponseBuilder.addDataOperation(
        DataOperation.newBuilder().setOperation(Operation.CLEAR_ALL).build());
    return this;
  }

  /** Add a data operation for a feature that represents the Root of the Stream. */
  public WireProtocolResponseBuilder addRootFeature() {
    wireProtocolInfo.featureCount++;
    PayloadMetadata payloadMetadata =
        PayloadMetadata.newBuilder().setContentId(ROOT_CONTENT_ID).build();
    Feature feature = Feature.newBuilder().setRenderableUnit(RenderableUnit.STREAM).build();

    feedResponseBuilder.addDataOperation(
        DataOperation.newBuilder()
            .setOperation(Operation.UPDATE_OR_APPEND)
            .setFeature(feature)
            .setMetadata(payloadMetadata)
            .build());
    return this;
  }

  public Response build() {
    return Response.newBuilder()
        .setExtension(FeedResponse.feedResponse, feedResponseBuilder.build())
        .build();
  }

  public WireProtocolInfo getWireProtocolInfo() {
    return wireProtocolInfo;
  }

  /** Captures information about the wire protocol that was created. */
  public static class WireProtocolInfo {
    public boolean hasClearOperation = false;
    public int featureCount = 0;
  }
}
