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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.android.libraries.feed.common.Validators.checkState;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.PietContentModel;
import com.google.android.libraries.feed.basicstream.internal.util.TokenHandler;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import java.util.ArrayList;
import java.util.List;

/** Generates a list of {@link ContentModel} instances for an entire stream. */
public class StreamDriver {

  private static final String TAG = "StreamDriver";
  private final ModelFeature streamFeature;
  private final ThreadUtils threadUtils;
  private final TokenHandler tokenHandler;

  public StreamDriver(
      ModelFeature streamFeature, ThreadUtils threadUtils, TokenHandler tokenHandler) {
    this.streamFeature = streamFeature;
    this.threadUtils = threadUtils;
    this.tokenHandler = tokenHandler;
  }

  public List<ContentModel> getContentModels() {
    return getContentModelsFromModelFeatures(processCursor(streamFeature.getCursor()));
  }

  private List<ContentModel> getContentModelsFromModelFeatures(List<ModelFeature> modelFeatures) {
    List<ContentModel> contentModels = new ArrayList<>();
    for (ModelFeature modelFeature : modelFeatures) {
      if (!modelFeature.getStreamFeature().hasCard()) {
        throw new RuntimeException("Expected card feature as a StreamFeature");
      }

      ModelCursor cardCursor = modelFeature.getCursor();
      while (cardCursor.advanceCursor(
          child -> {
            StreamFeature streamFeature = child.getModelFeature().getStreamFeature();
            checkState(streamFeature.hasContent(), "Expected content for feature");

            Content content = streamFeature.getContent();
            checkState(
                content.getType() == StreamStructureProto.Content.Type.PIET,
                "Expected Piet type for feature");

            checkState(
                content.hasExtension(PietContent.pietContentExtension),
                "Expected Piet content for feature");
            contentModels.add(
                new PietContentModel(content.getExtension(PietContent.pietContentExtension)));
          })) {}
    }

    return contentModels;
  }

  private List<ModelFeature> processCursor(ModelCursor modelCursor) {
    List<ModelFeature> cardFeatures = new ArrayList<>();
    while (modelCursor.advanceCursor(
        (ModelChild child) -> {
          Logger.i(TAG, "Advance Cursor");
          if (child.getType() == Type.FEATURE) {
            ModelFeature content = child.getModelFeature();
            if (content == null) {
              Logger.w(TAG, "Content not found");
              return;
            }

            if (content.getStreamFeature().hasCard()) {
              cardFeatures.add(content);
            } else if (content.getStreamFeature().hasCluster()) {
              cardFeatures.addAll(processCursor(content.getCursor()));
            }
          } else if (child.getType() == Type.TOKEN) {
            ModelToken token = child.getModelToken();
            token.registerObserver(
                change -> {
                  // Add the new cursor to the adapter.
                  threadUtils.checkMainThread();
                  List<ModelFeature> modelFeatures = processCursor(change.getCursor());
                  Logger.i(
                      TAG, "TokenCompletedObserver - added Features: %s", modelFeatures.size());
                  tokenHandler.addContentModels(getContentModelsFromModelFeatures(modelFeatures));
                });
            // TODO: This should only be called after the user has clicked on a button
            // to request more.
            tokenHandler.handleToken(token);
          }
        })) {}

    return cardFeatures;
  }
}
