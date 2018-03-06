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

package com.google.android.libraries.feed.feedmodelprovider.internal;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.common.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import javax.annotation.Nullable;

/** */
public class UpdatableModelChild implements ModelChild {
  private static final String TAG = "UpdatableModelChild";

  private final StreamContentId contentId;
  @Nullable private final StreamContentId parentContentId;
  private final InternalProtocolUtils protocolUtils;

  private ModelChild.Type type = Type.UNBOUND;
  private UpdatableModelFeature modelFeature;
  private UpdatableModelToken modelToken;

  UpdatableModelChild(
      StreamContentId contentId,
      @Nullable StreamContentId parentContentId,
      InternalProtocolUtils protocolUtils) {
    this.contentId = contentId;
    this.parentContentId = parentContentId;
    this.protocolUtils = protocolUtils;
  }

  void bindFeature(UpdatableModelFeature modelFeature) {
    validateType(Type.UNBOUND);
    this.modelFeature = modelFeature;
    type = Type.FEATURE;
  }

  void bindToken(UpdatableModelToken modelToken) {
    validateType(Type.UNBOUND);
    this.modelToken = modelToken;
    type = Type.TOKEN;
  }

  @Override
  public ModelFeature getModelFeature() {
    validateType(Type.FEATURE);
    // TODO: We want to prune these once they are accessed by the cursor.  Add support
    // for this.
    return modelFeature;
  }

  @Override
  public ModelToken getModelToken() {
    validateType(Type.TOKEN);
    return modelToken;
  }

  public UpdatableModelToken getUpdatableModelToken() {
    validateType(Type.TOKEN);
    return modelToken;
  }

  public String getContentIdAsKey() {
    return protocolUtils.getContentIdKey(contentId);
  }

  public void updateFeature(StreamPayload payload) {
    switch (type) {
      case FEATURE:
        if (payload.hasStreamFeature()) {
          modelFeature.setFeatureValue(payload.getStreamFeature());
        } else {
          Logger.e(TAG, "Attempting to update a ModelFeature without providing a feature");
        }
        break;
      case TOKEN:
        Logger.e(TAG, "Updated called for TOKEN is unsupported");
        break;
      default:
        Logger.e(TAG, "Updated called for unsupported type: %s", type);
    }
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public StreamContentId getContentId() {
    return contentId;
  }

  @Override
  public StreamContentId getParentId() {
    return parentContentId;
  }

  @Override
  public boolean hasParentId() {
    return parentContentId != null;
  }

  private void validateType(ModelChild.Type type) {
    if (this.type != type) {
      throw new IllegalStateException(
          String.format("ModelChild type error - Type %s, expected %s", this.type, type));
    }
  }
}
