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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.api.stream.ScrollListener;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.basicstream.internal.StreamContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.StreamItemAnimator;
import com.google.android.libraries.feed.basicstream.internal.StreamItemTouchCallbacks;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter;
import com.google.android.libraries.feed.basicstream.internal.StreamScrollMonitor;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.host.stream.SnackbarApi;
import com.google.android.libraries.feed.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.android.libraries.feed.sharedstream.piet.PietAssetProvider;
import com.google.android.libraries.feed.sharedstream.piet.PietCustomElementProvider;
import com.google.android.libraries.feed.sharedstream.piet.PietHostBindingProvider;
import java.util.List;

/**
 * A basic implementation of a Feed {@link Stream} that is just able to render a vertical stream of
 * cards.
 */
public class BasicStream implements Stream, ModelProviderObserver {

  private static final String TAG = "BasicStream";

  private final CardConfiguration cardConfiguration;
  private final ThreadUtils threadUtils;
  private final PietManager pietManager;
  private final ModelProviderFactory modelProviderFactory;
  private final ActionParser actionParser;
  private final ActionApi actionApi;
  private final ActionManager actionManager;
  private final Configuration configuration;
  private final SnackbarApi snackbarApi;
  private final StreamContentChangedListener streamContentChangedListener =
      new StreamContentChangedListener();

  private RecyclerView recyclerView;
  private ModelProvider modelProvider;
  private Context context;
  private List<View> headers;
  private StreamConfiguration streamConfiguration;
  private StreamRecyclerViewAdapter adapter;
  private StreamDriver streamDriver;
  private StreamScrollMonitor streamScrollMonitor;

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
      /*@Nullable*/ HostBindingProvider hostBindingProvider,
      ActionManager actionManager,
      Configuration configuration,
      SnackbarApi snackbarApi) {
    this.cardConfiguration = cardConfiguration;
    this.threadUtils = threadUtils;
    this.pietManager =
        createPietManager(
            context,
            cardConfiguration,
            imageLoaderApi,
            customElementProvider,
            debugBehavior,
            clock,
            hostBindingProvider);
    this.context = context;
    this.headers = headers;
    this.modelProviderFactory = modelProviderFactory;
    this.streamConfiguration = streamConfiguration;
    this.actionParser = actionParser;
    this.actionApi = actionApi;
    this.actionManager = actionManager;
    this.configuration = configuration;
    this.snackbarApi = snackbarApi;
  }

  @VisibleForTesting
  PietManager createPietManager(
      /*@UnderInitialization*/ BasicStream this,
      Context context,
      CardConfiguration cardConfiguration,
      ImageLoaderApi imageLoaderApi,
      /*@Nullable*/ CustomElementProvider customElementProvider,
      DebugBehavior debugBehavior,
      Clock clock,
      /*@Nullable*/ HostBindingProvider hostBindingProvider) {
    return new PietManager(
        debugBehavior,
        new PietAssetProvider(imageLoaderApi, cardConfiguration, clock),
        new PietCustomElementProvider(context, customElementProvider),
        new PietHostBindingProvider(hostBindingProvider));
  }

  @Override
  public void onCreate(/*@Nullable*/ Bundle savedInstanceState) {
    checkState(recyclerView == null, "Can't call onCreate() multiple times.");
    // TODO: Implement save/restore.
    adapter =
        createRecyclerViewAdapter(
            context, cardConfiguration, pietManager, actionParser, actionApi, actionManager);
    adapter.setHeaders(headers);

    recyclerView = new RecyclerView(context);
    recyclerView.setLayoutManager(createRecyclerViewLayoutManager(context));

    new ItemTouchHelper(new StreamItemTouchCallbacks()).attachToRecyclerView(recyclerView);

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

    recyclerView.setItemAnimator(new StreamItemAnimator(streamContentChangedListener));

    streamScrollMonitor = createStreamScrollMonitor(recyclerView, streamContentChangedListener);

    modelProvider = modelProviderFactory.createNew();
    modelProvider.registerObserver(this);
    streamDriver =
        createStreamDriver(
            modelProvider,
            threadUtils,
            configuration,
            context,
            snackbarApi,
            streamContentChangedListener);
    adapter.setDriver(streamDriver);
    streamDriver.showLoadingSpinner();
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
  public void onDestroy() {
    if (modelProvider != null) {
      modelProvider.unregisterObserver(this);
      modelProvider.invalidate();
    }
  }

  @Override
  public Bundle getSavedInstanceState() {
    // TODO: Implement save/restore.
    return new Bundle();
  }

  @Override
  public View getView() {
    checkState(recyclerView != null, "Must call onCreate() before getView()");
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
  public void setStreamContentVisibility(boolean visible) {
    checkNotNull(adapter, "onCreate must be called before setStreamContentVisibility");
    adapter.setStreamContentVisible(visible);
  }

  @Override
  public void trim() {
    pietManager.purgeRecyclerPools();
    recyclerView.getRecycledViewPool().clear();
  }

  @Override
  public void smoothScrollBy(int dx, int dy) {
    recyclerView.smoothScrollBy(dx, dy);
  }

  @Override
  public int getChildTopAt(int position) {
    if (!isChildAtPositionVisible(position)) {
      return POSITION_NOT_KNOWN;
    }

    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    if (layoutManager == null) {
      return POSITION_NOT_KNOWN;
    }

    View view = layoutManager.findViewByPosition(position);
    if (view == null) {
      return POSITION_NOT_KNOWN;
    }

    return view.getTop();
  }

  @Override
  public boolean isChildAtPositionVisible(int position) {
    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    if (layoutManager == null) {
      return false;
    }

    int firstItemPosition = layoutManager.findFirstVisibleItemPosition();
    int lastItemPosition = layoutManager.findLastVisibleItemPosition();
    if (firstItemPosition == RecyclerView.NO_POSITION
        || lastItemPosition == RecyclerView.NO_POSITION) {
      return false;
    }

    if (position < firstItemPosition || position > lastItemPosition) {
      return false;
    }

    return true;
  }

  @Override
  public void addScrollListener(ScrollListener listener) {
    streamScrollMonitor.addScrollListener(listener);
  }

  @Override
  public void removeScrollListener(ScrollListener listener) {
    streamScrollMonitor.removeScrollListener(listener);
  }

  @Override
  public void addOnContentChangedListener(ContentChangedListener listener) {
    streamContentChangedListener.addContentChangedListener(listener);
  }

  @Override
  public void removeOnContentChangedListener(ContentChangedListener listener) {
    streamContentChangedListener.removeContentChangedListener(listener);
  }

  @Override
  public void triggerRefresh() {
    // This invalidates the modelProvider, which results in onSessionFinished() then
    // onSessionStart() being called, leading to recreating the entire stream.
    streamDriver.showLoadingSpinner();
    modelProvider.triggerRefresh();
  }

  private void updateAdapter(ModelProvider modelProvider) {
    streamDriver =
        createStreamDriver(
            modelProvider,
            threadUtils,
            configuration,
            context,
            snackbarApi,
            streamContentChangedListener);

    adapter.setDriver(streamDriver);
  }

  @Override
  public void onSessionStart() {
    threadUtils.checkMainThread();
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

  @VisibleForTesting
  StreamDriver createStreamDriver(
      ModelProvider modelProvider,
      ThreadUtils threadUtils,
      Configuration configuration,
      Context context,
      SnackbarApi snackbarApi,
      ContentChangedListener contentChangedListener) {
    return new StreamDriver(
        modelProvider, threadUtils, configuration, context, snackbarApi, contentChangedListener);
  }

  @VisibleForTesting
  StreamRecyclerViewAdapter createRecyclerViewAdapter(
      Context context,
      CardConfiguration cardConfiguration,
      PietManager pietManager,
      ActionParser actionParser,
      ActionApi actionApi,
      ActionManager actionManager) {
    return new StreamRecyclerViewAdapter(
        context, cardConfiguration, pietManager, actionParser, actionApi, actionManager);
  }

  @VisibleForTesting
  StreamScrollMonitor createStreamScrollMonitor(
      RecyclerView recyclerView, ContentChangedListener contentChangedListener) {
    return new StreamScrollMonitor(recyclerView, contentChangedListener);
  }

  @VisibleForTesting
  LinearLayoutManager createRecyclerViewLayoutManager(Context context) {
    return new LinearLayoutManager(context);
  }
}
