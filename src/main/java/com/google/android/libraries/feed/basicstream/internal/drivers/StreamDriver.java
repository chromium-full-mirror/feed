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

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.common.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/** Generates a list of {@link ContentModel} instances for an entire stream. */
public class StreamDriver {

  private static final String TAG = "StreamDriver";
  private final ThreadUtils threadUtils;
  private final ModelProvider modelProvider;

  /*@Nullable*/ private List<FeatureDriver> featureDrivers;

  /*@Nullable*/ private StreamContentListener contentListener;

  public StreamDriver(ModelProvider modelProvider, ThreadUtils threadUtils) {
    this.threadUtils = threadUtils;
    this.modelProvider = modelProvider;
  }

  /**
   * Returns a the list of {@link ContentModel} instances for the children generated from the given
   * {@link ModelFeature}.
   */
  public List<ContentModel> getContentModels() {
    if (featureDrivers == null) {
      featureDrivers = createChildren(modelProvider.getRootFeature(), modelProvider);
    }

    return buildContentModels(featureDrivers);
  }

  private List<FeatureDriver> createChildren(
      /*@Nullable*/ ModelFeature streamFeature, ModelProvider modelProvider) {
    if (streamFeature == null) {
      Logger.i(TAG, "Root feature is null so returning empty content models.");
      return new ArrayList<>();
    }

    return createChildren(streamFeature.getCursor(), modelProvider);
  }

  private List<FeatureDriver> createChildren(
      ModelCursor streamCursor, ModelProvider modelProvider) {
    List<FeatureDriver> children = new ArrayList<>();
    ModelChild child;
    while ((child = streamCursor.getNextItem()) != null) {
      Logger.i(TAG, "Advancing Cursor");
      switch (child.getType()) {
        case Type.FEATURE:
          FeatureDriver featureDriver = createFeatureChild(child.getModelFeature());
          if (featureDriver != null) {
            children.add(featureDriver);
          }
          break;
        case Type.TOKEN:
          ModelToken modelToken = child.getModelToken();
          ContinuationDriver continuationDriver =
              createContinuationDriver(modelProvider, modelToken);
          children.add(continuationDriver);

          // TODO: Place the observing of the token in the ContinuationDriver.
          modelToken.registerObserver(
              tokenCompleted -> {
                if (featureDrivers == null) {
                  Logger.e(TAG, "featureDrivers null while handling a token");
                  return;
                }
                List<FeatureDriver> featureDrivers = this.featureDrivers;

                threadUtils.checkMainThread();

                int continuationIndex = featureDrivers.indexOf(continuationDriver);
                if (continuationIndex == -1) {
                  Logger.wtf(TAG, "continuationDriver not in list when attempting to remove");
                  return;
                }
                featureDrivers.remove(continuationIndex);

                if (contentListener != null) {
                  contentListener.notifyContentRemoved(continuationIndex);
                }
                List<FeatureDriver> tokenFeatureDrivers =
                    createChildren(tokenCompleted.getCursor(), modelProvider);

                featureDrivers.addAll(continuationIndex, tokenFeatureDrivers);

                if (contentListener != null) {
                  contentListener.notifyContentsAdded(
                      continuationIndex, buildContentModels(tokenFeatureDrivers));
                }
              });
          break;
        case Type.UNBOUND:
          Logger.e(TAG, "Found unbound child %s, ignoring it", child.getContentId());
          break;
        default:
          Logger.wtf(TAG, "Received illegal child: %s from cursor.", child.getType());
          break;
      }
    }
    return children;
  }

  private /*@Nullable*/ FeatureDriver createFeatureChild(ModelFeature modelFeature) {
    if (modelFeature.getStreamFeature().hasCard()) {
      return createCardDriver(modelFeature);
    } else if (modelFeature.getStreamFeature().hasCluster()) {
      return createClusterDriver(modelFeature);
    }

    Logger.w(
        TAG,
        "Invalid StreamFeature Type, must be Card or Cluster but was %s",
        modelFeature.getStreamFeature().getFeaturePayloadCase());
    return null;
  }

  private List<ContentModel> buildContentModels(List<FeatureDriver> featureDrivers) {
    List<ContentModel> contentModels = new ArrayList<>();
    for (FeatureDriver featureDriver : featureDrivers) {
      ContentModel childContentModel = featureDriver.getContentModel();
      if (childContentModel != null) {
        contentModels.add(childContentModel);
      }
    }
    return contentModels;
  }

  @VisibleForTesting
  FeatureDriver createClusterDriver(ModelFeature modelFeature) {
    return new ClusterDriver(modelFeature, modelProvider);
  }

  @VisibleForTesting
  FeatureDriver createCardDriver(ModelFeature modelFeature) {
    return new CardDriver(modelFeature, modelProvider);
  }

  @VisibleForTesting
  ContinuationDriver createContinuationDriver(ModelProvider modelProvider, ModelToken modelToken) {
    return new ContinuationDriver(modelProvider, modelToken);
  }

  public void setStreamContentListener(/*@Nullable*/ StreamContentListener contentListener) {
    this.contentListener = contentListener;
  }

  /** Allows listening for changes in the contents held by a {@link StreamDriver} */
  public interface StreamContentListener {

    /** Called when the given content has been added at the given index of stream content. */
    void notifyContentsAdded(int index, List<ContentModel> newContents);

    /** Called when the content at the given index of stream content has been removed. */
    void notifyContentRemoved(int index);
  }
}
