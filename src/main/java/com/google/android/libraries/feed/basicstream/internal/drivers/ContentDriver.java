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

import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.PietContentModel;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.stream.StreamStructureProto;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.ArrayList;
import java.util.List;

/** {@link FeatureDriver} for content. */
public class ContentDriver implements FeatureDriver {

  private static final String TAG = "ContentDriver";

  private final ModelFeature contentFeatureModel;
  private final ModelProvider modelProvider;
  /*@Nullable*/ private ContentModel contentModel;

  ContentDriver(ModelFeature contentFeatureModel, ModelProvider modelProvider) {
    this.contentFeatureModel = contentFeatureModel;
    this.modelProvider = modelProvider;
  }

  @Override
  /*@Nullable*/
  public ContentModel getContentModel() {
    // TODO: Add observer for changes in content.
    if (contentModel == null) {
      StreamStructureProto.Content content = contentFeatureModel.getStreamFeature().getContent();
      checkState(
          content.getType() == StreamStructureProto.Content.Type.PIET,
          "Expected Piet type for feature");

      checkState(
          content.hasExtension(PietContent.pietContentExtension),
          "Expected Piet content for feature");
      List<PietSharedState> pietSharedStates = new ArrayList<>();

      PietContent pietContent = content.getExtension(PietContent.pietContentExtension);
      for (ContentId contentId : pietContent.getPietSharedStatesList()) {
        PietSharedState pietSharedState = extractPietSharedState(contentId);
        if (pietSharedState == null) {
          return null;
        }

        pietSharedStates.add(pietSharedState);
      }
      contentModel = new PietContentModel(pietSharedStates, pietContent);
    }

    return contentModel;
  }

  /*@Nullable*/
  private PietSharedState extractPietSharedState(ContentId pietSharedStateId) {
    StreamSharedState sharedState = modelProvider.getSharedState(pietSharedStateId);
    if (sharedState != null) {
      return sharedState.getPietSharedStateItem().getPietSharedState();
    }
    Logger.e(
        TAG,
        "Shared state was null. Stylesheets and templates on PietSharedState "
            + "will not be loaded.");
    return null;
  }
}
