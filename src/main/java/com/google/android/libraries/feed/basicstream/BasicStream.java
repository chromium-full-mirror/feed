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

package com.google.android.libraries.feed.basicstream;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.stream.CardConfiguration;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.api.stream.ScrollListener;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.basicstream.internal.StreamActionApiImpl;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter.ViewHolderBindingObserver;
import com.google.android.libraries.feed.basicstream.internal.piet.PietSharedStateSupplier;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.action.ActionParser;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.PietStylesHelper;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A basic implementation of a Feed {@link Stream} that is just able to render a vertical stream of
 * cards.
 */
public class BasicStream implements Stream {

  private static final String TAG = "BasicStream";

  private final RecyclerView recyclerView;
  private final CardConfiguration cardConfiguration;
  private final ThreadUtils threadUtils;
  private final StreamRecyclerViewAdapter adapter;
  private final ViewHolderBindingObserver viewHolderBindingObserver;
  private final PietManager pietManager;
  private final PietSharedStateSupplier pietSharedStateSupplier;

  @Nullable private ModelToken streamToken = null;
  @Nullable private ModelProvider modelProvider;

  public BasicStream(
      Context context,
      CardConfiguration cardConfiguration,
      AssetProvider assetProvider,
      ActionParser actionParser,
      ActionApi actionApi,
      CustomElementProvider customElementProvider,
      ThreadUtils threadUtils,
      List<View> headers) {
    this.cardConfiguration = cardConfiguration;
    this.threadUtils = threadUtils;
    this.pietSharedStateSupplier = createPietSharedStateSupplier();
    this.pietManager =
        new PietManager(
            new PietStylesHelper(pietSharedStateSupplier),
            assetProvider,
            (action, frame, veLoggingToken) ->
                actionParser.parseAction(
                    action,
                    new StreamActionApiImpl(context, actionApi, actionParser),
                    veLoggingToken),
            customElementProvider);
    recyclerView = new RecyclerView(context);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
    viewHolderBindingObserver = initializeBindingObserver();
    adapter =
        new StreamRecyclerViewAdapter(
            context, this.cardConfiguration, pietManager, viewHolderBindingObserver);
    adapter.setHeaders(headers);
    recyclerView.setAdapter(adapter);
  }

  private ViewHolderBindingObserver initializeBindingObserver() {
    return () -> {
      if (streamToken != null && modelProvider != null) {
        // TODO: Show a Spinner until the observer is called
        Logger.i(TAG, "Handle the continuation Token.");
        streamToken.registerObserver(
            change -> {
              // Add the new cursor to the adapter.
              threadUtils.checkMainThread();
              List<ModelFeature> modelFeatures = processCursor(change.getCursor());
              Logger.i(TAG, "TokenCompletedObserver - added Features: %s", modelFeatures.size());
              adapter.addModelFeatures(modelFeatures);
            });
        modelProvider.handleToken(streamToken);
        streamToken = null;
      }
    };
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    // TODO: Implement save/restore.
  }

  @Override
  public void onShow() {}

  @Override
  public void onActive() {}

  @Override
  public void onInactive() {}

  @Override
  public void onHide() {}

  @Override
  public void onDestroy() {}

  @Override
  public Bundle getSavedInstanceState() {
    // TODO: Implement save/restore.
    return new Bundle();
  }

  @Override
  public View getView() {
    return recyclerView;
  }

  @Override
  public void setHeaderViews(List<View> headers) {
    adapter.setHeaders(headers);
  }

  @Override
  public void setStreamContentVisibility(boolean visible) {}

  @Override
  public void trim() {}

  @Override
  public void smoothScrollBy(int dx, int dy) {}

  @Override
  public int getChildTopAt(int position) {
    return POSITION_NOT_KNOWN;
  }

  @Override
  public boolean isChildAtPositionVisible(int position) {
    return recyclerView.getLayoutManager().findViewByPosition(position).getVisibility()
        == View.VISIBLE;
  }

  @Override
  public void addScrollListener(ScrollListener listener) {}

  @Override
  public void removeScrollListener(ScrollListener listener) {}

  @Override
  public void addOnContentChangedListener(ContentChangedListener listener) {}

  @Override
  public void removeOnContentChangedListener(ContentChangedListener listener) {}

  @Override
  public void setModelProvider(ModelProvider modelProvider) {
    this.modelProvider = modelProvider;
    this.pietSharedStateSupplier.setModelProvider(modelProvider);
    modelProvider.registerObserver(
        new ModelProviderObserver() {
          @Override
          public void onSessionStart() {
            threadUtils.checkMainThread();
            updateAdapter(modelProvider);
          }

          @Override
          public void onSessionFinished() {
            // TODO: Do we clear out the adapter?
          }

          @Override
          public void onRootSet() {
            // TODO: What do we do if the root is reset?
          }
        });
  }

  private void updateAdapter(ModelProvider modelProvider) {
    ModelFeature root = modelProvider.getRootFeature();
    if (root == null) {
      adapter.setModelFeatures(new ArrayList<>());
      return;
    }
    List<ModelFeature> modelFeatures = processCursor(root.getCursor());
    Logger.i(TAG, "updateAdapter - New Features: %s", modelFeatures.size());
    adapter.setModelFeatures(modelFeatures);
  }

  private List<ModelFeature> processCursor(ModelCursor modelCursor) {
    final List<ModelFeature> modelFeatures = new ArrayList<>();
    while (modelCursor.advanceCursor(
        (ModelChild child) -> {
          Logger.i(TAG, "Advance Cursor");
          if (child.getType() == Type.FEATURE) {
            ModelFeature content = child.getModelFeature();
            if (content == null) {
              Logger.w(TAG, "Content not found");
              return;
            }

            modelFeatures.add(content);
          } else if (child.getType() == Type.TOKEN) {
            streamToken = child.getModelToken();
          }
        })) {}
    return modelFeatures;
  }

  @VisibleForTesting
  PietSharedStateSupplier createPietSharedStateSupplier() {
    return new PietSharedStateSupplier();
  }
}
