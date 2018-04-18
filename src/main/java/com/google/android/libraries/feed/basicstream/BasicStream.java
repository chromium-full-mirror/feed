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

import static com.google.android.libraries.feed.common.Validators.checkNotNull;
import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.api.stream.ScrollListener;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.basicstream.internal.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.PietViewHolder;
import com.google.android.libraries.feed.basicstream.internal.StreamActionApiImpl;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter.ViewHolderBindingObserver;
import com.google.android.libraries.feed.basicstream.internal.ViewHolderType;
import com.google.android.libraries.feed.basicstream.internal.piet.PietAssetProvider;
import com.google.android.libraries.feed.basicstream.internal.piet.PietCustomElementProvider;
import com.google.android.libraries.feed.basicstream.internal.piet.PietHostBindingProvider;
import com.google.android.libraries.feed.basicstream.internal.piet.PietSharedStateSupplier;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.PietStylesHelper;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import java.util.ArrayList;
import java.util.List;

/**
 * A basic implementation of a Feed {@link Stream} that is just able to render a vertical stream of
 * cards.
 */
public class BasicStream implements Stream, ModelProviderObserver {

  private static final String TAG = "BasicStream";

  private final RecyclerView recyclerView;
  private final CardConfiguration cardConfiguration;
  private final ThreadUtils threadUtils;
  private final PietManager pietManager;
  private final PietSharedStateSupplier pietSharedStateSupplier;
  private final ModelProviderFactory modelProviderFactory;

  /*@Nullable*/ private ModelToken streamToken = null;
  private ModelProvider modelProvider;
  private Context context;
  private List<View> headers;
  private StreamConfiguration streamConfiguration;
  private StreamRecyclerViewAdapter adapter;
  private ViewHolderBindingObserver viewHolderBindingObserver;

  public BasicStream(
      Context context,
      StreamConfiguration streamConfiguration,
      CardConfiguration cardConfiguration,
      ImageLoaderApi imageLoaderApi,
      ActionParser actionParser,
      ActionApi actionApi,
      /*@Nullable*/ CustomElementProvider customElementProvider,
      DebugBehavior debugBehavior,
      ThreadUtils threadUtils,
      List<View> headers,
      Clock clock,
      ModelProviderFactory modelProviderFactory,
      /*@Nullable*/ HostBindingProvider hostBindingProvider) {
    this.cardConfiguration = cardConfiguration;
    this.threadUtils = threadUtils;
    this.pietSharedStateSupplier = createPietSharedStateSupplier();
    this.pietManager =
        new PietManager(
            new PietStylesHelper(pietSharedStateSupplier),
            debugBehavior,
            new PietAssetProvider(imageLoaderApi, cardConfiguration, clock),
            (action, frame, view, veLoggingToken) ->
                actionParser.parseAction(
                    action,
                    new StreamActionApiImpl(context, actionApi, actionParser),
                    view,
                    veLoggingToken),
            new PietCustomElementProvider(context, customElementProvider),
            new PietHostBindingProvider(hostBindingProvider));
    recyclerView = new RecyclerView(context);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
    this.context = context;
    this.headers = headers;
    this.modelProviderFactory = modelProviderFactory;
    this.streamConfiguration = streamConfiguration;
  }

  @Override
  public void onCreate(/*@Nullable*/ Bundle savedInstanceState) {
    viewHolderBindingObserver = initializeBindingObserver();
    adapter =
        new StreamRecyclerViewAdapter(
            context, cardConfiguration, pietManager, viewHolderBindingObserver);
    adapter.setHeaders(headers);
    recyclerView.setAdapter(adapter);
    recyclerView.setClipToPadding(false);
    if (VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN) {
      recyclerView.setPaddingRelative(
          streamConfiguration.getPaddingStart(),
          streamConfiguration.getPaddingTop(),
          streamConfiguration.getPaddingEnd(),
          streamConfiguration.getPaddingBottom());
    } else {
      recyclerView.setPadding(
          streamConfiguration.getPaddingStart(),
          streamConfiguration.getPaddingTop(),
          streamConfiguration.getPaddingEnd(),
          streamConfiguration.getPaddingBottom());
    }
    modelProvider = modelProviderFactory.createNew();
    modelProvider.registerObserver(this);
    // TODO: Implement save/restore.
  }

  private ViewHolderBindingObserver initializeBindingObserver() {
    return () -> {
      ModelToken token = streamToken;
      if (token != null && modelProvider != null) {
        // TODO: Show a Spinner until the observer is called
        Logger.i(TAG, "Handle the continuation Token.");
        token.registerObserver(
            change -> {
              // Add the new cursor to the adapter.
              checkNotNull(threadUtils).checkMainThread();
              List<ModelFeature> modelFeatures = processCursor(change.getCursor());
              Logger.i(TAG, "TokenCompletedObserver - added Features: %s", modelFeatures.size());
              adapter.addContentModels(getContentModels(modelFeatures));
            });
        modelProvider.handleToken(token);
        streamToken = null;
      }
    };
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

  @VisibleForTesting
  StreamRecyclerViewAdapter getAdapter() {
    return adapter;
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
    return checkNotNull(recyclerView.getLayoutManager())
            .findViewByPosition(position)
            .getVisibility()
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
  public void forceRefresh() {
    // We need to clear the token, or we will call modelProvider.handleToken() when we build the
    // the new stream.
    streamToken = null;
    modelProvider.forceRefresh();
  }

  private void updateAdapter(ModelProvider modelProvider) {
    ModelFeature root = modelProvider.getRootFeature();
    if (root == null) {
      adapter.setContentModels(new ArrayList<>());
      return;
    }
    List<ModelFeature> modelFeatures = processCursor(root.getCursor());
    Logger.i(TAG, "updateAdapter - New Features: %s", modelFeatures.size());
    adapter.setContentModels(getContentModels(modelFeatures));
  }

  private List<ContentModel> getContentModels(List<ModelFeature> modelFeatures) {
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

            StreamStructureProto.Content content = streamFeature.getContent();
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
            streamToken = child.getModelToken();
          }
        })) {}

    return cardFeatures;
  }

  @VisibleForTesting
  PietSharedStateSupplier createPietSharedStateSupplier(/*@UnderInitialization*/ BasicStream this) {
    return new PietSharedStateSupplier();
  }

  @Override
  public void onSessionStart() {
    threadUtils.checkMainThread();
    pietSharedStateSupplier.setModelProvider(modelProvider);
    updateAdapter(modelProvider);
  }

  @Override
  public void onSessionFinished() {
    modelProvider.unregisterObserver(this);
    modelProvider = modelProviderFactory.createNew();
    modelProvider.registerObserver(this);
  }

  @Override
  public void onRootSet() {
    // TODO: Handle onRootSet
  }

  // TODO: Move this out of the BasicStream into its own class.
  static class PietContentModel implements ContentModel {

    private final PietContent pietContent;

    public PietContentModel(PietContent pietContent) {
      this.pietContent = pietContent;
    }

    @Override
    public void bind(ViewHolder viewHolder) {
      ((PietViewHolder) viewHolder).bind(pietContent.getFrame());
    }

    @Override
    public void unbind(ViewHolder viewHolder) {
      ((PietViewHolder) viewHolder).unbind();
    }

    @Override
    public int itemId() {
      return pietContent.hashCode();
    }

    @Override
    public int getItemViewType() {
      return ViewHolderType.TYPE_CARD;
    }

    @VisibleForTesting
    public PietContent getPietContent() {
      return pietContent;
    }
  }
}
