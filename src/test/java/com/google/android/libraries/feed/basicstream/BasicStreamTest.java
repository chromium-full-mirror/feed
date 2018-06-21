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

import static com.google.android.libraries.feed.api.stream.Stream.POSITION_NOT_KNOWN;
import static com.google.android.libraries.feed.basicstream.BasicStream.KEY_SCROLL_STATE;
import static com.google.android.libraries.feed.basicstream.BasicStream.KEY_SESSION_TOKEN;
import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelError.ErrorType;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.api.stream.ScrollListener;
import com.google.android.libraries.feed.basicstream.internal.DeepestContentTracker;
import com.google.android.libraries.feed.basicstream.internal.StreamContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter;
import com.google.android.libraries.feed.basicstream.internal.StreamScrollMonitor;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver;
import com.google.android.libraries.feed.basicstream.internal.scroll.ScrollRestorer;
import com.google.android.libraries.feed.common.testing.FakeClock;
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
import com.google.android.libraries.feed.testing.shadows.ShadowRecycledViewPool;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests for {@link BasicStream}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRecycledViewPool.class})
public class BasicStreamTest {

  private static final int START_PADDING = 1;
  private static final int END_PADDING = 2;
  private static final int TOP_PADDING = 3;
  private static final int BOTTOM_PADDING = 4;

  private static final String SESSION_TOKEN = "session-token";

  @Mock private StreamConfiguration streamConfiguration;
  @Mock private ModelProviderFactory modelProviderFactory;
  @Mock private ModelProvider initialModelProvider;
  @Mock private ModelProvider modelProvider;
  @Mock private ModelProvider restoredModelProvider;
  @Mock private PietManager pietManager;
  @Mock private SnackbarApi snackbarApi;
  @Mock private StreamDriver streamDriver;
  @Mock private StreamRecyclerViewAdapter adapter;
  @Mock private StreamScrollMonitor streamScrollMonitor;
  @Mock private ScrollRestorer nonRestoringScrollRestorer;
  @Mock private ScrollRestorer scrollRestorer;

  private Configuration configuration = new Configuration.Builder().build();
  private LinearLayoutManagerWithFakePositioning layoutManager;
  private Context context;
  private BasicStreamForTest basicStream;
  private Bundle scrollRestoreBundle;

  @Before
  public void setUp() {
    initMocks(this);

    when(streamConfiguration.getPaddingStart()).thenReturn(START_PADDING);
    when(streamConfiguration.getPaddingEnd()).thenReturn(END_PADDING);
    when(streamConfiguration.getPaddingTop()).thenReturn(TOP_PADDING);
    when(streamConfiguration.getPaddingBottom()).thenReturn(BOTTOM_PADDING);

    when(modelProviderFactory.createNew(any(ViewDepthProvider.class)))
        .thenReturn(initialModelProvider, modelProvider);

    when(initialModelProvider.getSessionToken()).thenReturn(SESSION_TOKEN);

    when(scrollRestorer.getBundleForScrollRestore()).thenReturn(scrollRestoreBundle);

    context = Robolectric.setupActivity(Activity.class);
    layoutManager = new LinearLayoutManagerWithFakePositioning(context);

    basicStream = createBasicStream(layoutManager);
    basicStream.onCreate(null);
  }

  @Test
  public void testOnSessionStart() {
    basicStream.onShow();
    reset(adapter);

    basicStream.onSessionStart();

    verify(adapter).setDriver(streamDriver);
    assertThat(basicStream.streamDriverScrollRestorer).isSameAs(scrollRestorer);
  }

  @Test
  public void testOnSessionFinished() {
    basicStream.onShow();
    basicStream.onSessionFinished();

    verify(scrollRestorer).abandonRestoringScroll();
    verify(initialModelProvider).unregisterObserver(basicStream);
    verify(modelProviderFactory, times(2)).createNew(any(ViewDepthProvider.class));
    verify(modelProvider).registerObserver(basicStream);
  }

  @Test
  public void testOnError_showsZeroState() {
    basicStream.onShow();
    reset(streamDriver);

    basicStream.onError(new ModelError(ErrorType.NO_CARDS_ERROR, /* continuationToken= */ null));

    verify(scrollRestorer).abandonRestoringScroll();
    verify(streamDriver).showZeroState(/* showSpinner= */ false);
  }

  @Test
  public void testOnError_doesNotShowZeroState() {
    basicStream.onShow();

    assertThatRunnable(
            () ->
                basicStream.onError(
                    new ModelError(ErrorType.PAGINATION_ERROR, /* continuationToken= */ null)))
        .throwsAnExceptionOfType(RuntimeException.class);
  }

  @Test
  public void testLifecycle_onCreateCalledOnlyOnce() {
    // onCreate is called once in setup
    assertThatRunnable(() -> basicStream.onCreate(null))
        .throwsAnExceptionOfType(IllegalStateException.class);
  }

  @Test
  public void testLifecycle_getViewBeforeOnCreateCrashes() {
    // create BasicStream that has not had onCreate() called.
    basicStream = createBasicStream(new LinearLayoutManagerWithFakePositioning(context));
    assertThatRunnable(() -> basicStream.getView())
        .throwsAnExceptionOfType(IllegalStateException.class);
  }

  @Test
  public void testLifecycle_onCreate_onDestroy() {
    basicStream.onDestroy();
    verify(initialModelProvider, never()).invalidate();
  }

  @Test
  public void testLifecycle_onCreate_onShow_onHide_onDestroy() {
    basicStream.onShow();
    basicStream.onHide();
    basicStream.onDestroy();
    verify(initialModelProvider).invalidate();
  }

  @Test
  public void testGetSavedInstanceState_beforeShow() {
    Bundle bundle = basicStream.getSavedInstanceState();
    assertThat(bundle.getString(KEY_SESSION_TOKEN)).isNull();
    assertThat(bundle.getBundle(KEY_SCROLL_STATE)).isSameAs(scrollRestoreBundle);
  }

  @Test
  public void testGetSavedInstanceState_afterOnShow_beforeSessionStart() {
    when(initialModelProvider.getSessionToken()).thenReturn(null);

    basicStream.onShow();

    Bundle bundle = basicStream.getSavedInstanceState();
    assertThat(bundle.getString(KEY_SESSION_TOKEN)).isNull();
    assertThat(bundle.getBundle(KEY_SCROLL_STATE)).isSameAs(scrollRestoreBundle);
  }

  @Test
  public void testGetSavedInstanceState_afterOnShow_afterSessionStart() {
    basicStream.onShow();

    Bundle bundle = basicStream.getSavedInstanceState();
    assertThat(bundle.getString(KEY_SESSION_TOKEN)).isEqualTo(SESSION_TOKEN);
    assertThat(bundle.getBundle(KEY_SCROLL_STATE)).isSameAs(scrollRestoreBundle);
  }

  @Test
  public void testGetSavedInstanceState_noScrollRestoreBundle() {
    basicStream.onShow();

    when(scrollRestorer.getBundleForScrollRestore()).thenReturn(null);
    Bundle bundle = basicStream.getSavedInstanceState();
    assertThat(bundle.getBundle(KEY_SCROLL_STATE)).isNull();
  }

  @Test
  public void testRestore() {
    basicStream.onShow();

    Bundle bundle = basicStream.getSavedInstanceState();

    basicStream.onHide();
    basicStream.onDestroy();

    when(modelProviderFactory.create(SESSION_TOKEN)).thenReturn(restoredModelProvider);

    basicStream = createBasicStream(new LinearLayoutManagerWithFakePositioning(context));
    basicStream.onCreate(bundle);

    basicStream.onShow();

    verify(restoredModelProvider).registerObserver(basicStream);
  }

  @Test
  public void testRestore_invalidSession() {
    basicStream.onShow();

    Bundle bundle = basicStream.getSavedInstanceState();

    basicStream.onHide();
    basicStream.onDestroy();

    basicStream = createBasicStream(new LinearLayoutManagerWithFakePositioning(context));
    basicStream.onCreate(bundle);
    basicStream.onShow();

    verify(modelProvider).registerObserver(basicStream);
  }

  @Test
  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  public void testPadding_jellyBean() {
    // Padding is setup in constructor.
    View view = basicStream.getView();

    assertThat(view.getPaddingLeft()).isEqualTo(START_PADDING);
    assertThat(view.getPaddingRight()).isEqualTo(END_PADDING);
    assertThat(view.getPaddingTop()).isEqualTo(TOP_PADDING);
    assertThat(view.getPaddingBottom()).isEqualTo(BOTTOM_PADDING);
  }

  @Test
  @Config(sdk = VERSION_CODES.KITKAT)
  public void testPadding_kitKat() {
    // Padding is setup in constructor.
    View view = basicStream.getView();

    assertThat(view.getPaddingStart()).isEqualTo(START_PADDING);
    assertThat(view.getPaddingEnd()).isEqualTo(END_PADDING);
    assertThat(view.getPaddingTop()).isEqualTo(TOP_PADDING);
    assertThat(view.getPaddingBottom()).isEqualTo(BOTTOM_PADDING);
  }

  @Test
  public void testTrim() {
    ShadowRecycledViewPool viewPool = Shadow.extract(getStreamRecyclerView().getRecycledViewPool());

    // RecyclerView ends up clearing the pool initially when the adapter is set on the RecyclerView.
    // Verify that has happened before anything else
    assertThat(viewPool.getClearCallCount()).isEqualTo(1);

    basicStream.trim();

    verify(pietManager).purgeRecyclerPools();

    // We expect the clear() call to be 2 as RecyclerView ends up clearing the pool initially when
    // the adapter is set on the RecyclerView.  So one call for that and one call for trim() call
    // on stream.
    assertThat(viewPool.getClearCallCount()).isEqualTo(2);
  }

  @Test
  public void testAddScrollListener() {
    ScrollListener scrollListener = mock(ScrollListener.class);
    basicStream.addScrollListener(scrollListener);
    verify(streamScrollMonitor).addScrollListener(scrollListener);
  }

  @Test
  public void testRemoveScrollListener() {
    ScrollListener scrollListener = mock(ScrollListener.class);
    basicStream.removeScrollListener(scrollListener);
    verify(streamScrollMonitor).removeScrollListener(scrollListener);
  }

  @Test
  public void testIsChildAtPositionVisible() {
    layoutManager.firstVisiblePosition = 0;
    layoutManager.lastVisiblePosition = 1;
    assertThat(basicStream.isChildAtPositionVisible(-2)).isFalse();
    assertThat(basicStream.isChildAtPositionVisible(-1)).isFalse();
    assertThat(basicStream.isChildAtPositionVisible(0)).isTrue();
    assertThat(basicStream.isChildAtPositionVisible(1)).isTrue();
    assertThat(basicStream.isChildAtPositionVisible(2)).isFalse();
  }

  @Test
  public void testIsChildAtPositionVisible_nothingVisible() {
    assertThat(basicStream.isChildAtPositionVisible(0)).isFalse();
  }

  @Test
  public void testIsChildAtPositionVisible_validTop() {
    layoutManager.firstVisiblePosition = 0;
    assertThat(basicStream.isChildAtPositionVisible(0)).isFalse();
  }

  @Test
  public void testIsChildAtPositionVisible_validBottom() {
    layoutManager.lastVisiblePosition = 1;
    assertThat(basicStream.isChildAtPositionVisible(0)).isFalse();
  }

  @Test
  public void testGetChildTopAt_noVisibleChild() {
    assertThat(basicStream.getChildTopAt(0)).isEqualTo(POSITION_NOT_KNOWN);
  }

  @Test
  public void testGetChildTopAt_noChild() {
    layoutManager.firstVisiblePosition = 0;
    layoutManager.lastVisiblePosition = 1;
    assertThat(basicStream.getChildTopAt(0)).isEqualTo(POSITION_NOT_KNOWN);
  }

  @Test
  public void testGetChildTopAt() {
    layoutManager.firstVisiblePosition = 0;
    layoutManager.lastVisiblePosition = 1;
    View view = new FrameLayout(context);
    layoutManager.addChildToPosition(0, view);

    assertThat(basicStream.getChildTopAt(0)).isEqualTo(view.getTop());
  }

  @Test
  public void testStreamContentVisible() {
    basicStream.setStreamContentVisibility(false);
    verify(adapter).setStreamContentVisible(false);

    reset(adapter);
    basicStream.setStreamContentVisibility(true);
    verify(adapter).setStreamContentVisible(true);
  }

  @Test
  public void testTriggerRefresh() {
    basicStream.onShow();

    reset(adapter, streamDriver);

    basicStream.triggerRefresh();

    verify(initialModelProvider).triggerRefresh();
    verify(streamDriver).showZeroState(/* showSpinner= */ true);
  }

  @Test
  public void testTriggerRefresh_beforeOnShow() {
    basicStream.triggerRefresh();

    verify(initialModelProvider, never()).triggerRefresh();
    verify(streamDriver, never()).showZeroState(/* showSpinner= */ true);
  }

  @Test
  public void testAddOnContentChangedListener() {
    ContentChangedListener contentChangedListener = mock(ContentChangedListener.class);

    basicStream.addOnContentChangedListener(contentChangedListener);
    basicStream.contentChangedListener.onContentChanged();

    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void testRemoveOnContentChangedListener() {
    ContentChangedListener contentChangedListener = mock(ContentChangedListener.class);

    basicStream.addOnContentChangedListener(contentChangedListener);
    basicStream.removeOnContentChangedListener(contentChangedListener);

    basicStream.contentChangedListener.onContentChanged();

    verify(contentChangedListener, never()).onContentChanged();
  }

  @Test
  public void testOnShow_nonRestoringRestorer() {
    basicStream.onShow();

    assertThat(basicStream.streamDriverScrollRestorer).isEqualTo(nonRestoringScrollRestorer);
  }

  @Test
  public void testOnShow_restoringScrollRestorer() {
    when(initialModelProvider.getCurrentState()).thenReturn(State.READY);
    basicStream.onShow();

    assertThat(basicStream.streamDriverScrollRestorer).isEqualTo(scrollRestorer);
  }

  private RecyclerView getStreamRecyclerView() {
    return (RecyclerView) basicStream.getView();
  }

  private BasicStreamForTest createBasicStream(LinearLayoutManager layoutManager) {

    return new BasicStreamForTest(
        context,
        streamConfiguration,
        mock(CardConfiguration.class),
        mock(ImageLoaderApi.class),
        mock(ActionParser.class),
        mock(ActionApi.class),
        mock(CustomElementProvider.class),
        DebugBehavior.VERBOSE,
        new ThreadUtils(),
        /* headers= */ new ArrayList<>(),
        new FakeClock(),
        modelProviderFactory,
        new HostBindingProvider(),
        mock(ActionManager.class),
        configuration,
        layoutManager);
  }

  private class BasicStreamForTest extends BasicStream {

    private final LinearLayoutManager layoutManager;

    private ScrollRestorer streamDriverScrollRestorer;
    private StreamContentChangedListener contentChangedListener;

    public BasicStreamForTest(
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
        LinearLayoutManager layoutManager) {
      super(
          context,
          streamConfiguration,
          cardConfiguration,
          imageLoaderApi,
          actionParser,
          actionApi,
          customElementProvider,
          debugBehavior,
          threadUtils,
          headers,
          clock,
          modelProviderFactory,
          hostBindingProvider,
          actionManager,
          configuration,
          snackbarApi);
      this.layoutManager = layoutManager;
    }

    @Override
    PietManager createPietManager(
        Context context,
        CardConfiguration cardConfiguration,
        ImageLoaderApi imageLoaderApi,
        /*@Nullable*/ CustomElementProvider customElementProvider,
        DebugBehavior debugBehavior,
        Clock clock,
        /*@Nullable*/ HostBindingProvider hostBindingProvider) {
      return pietManager;
    }

    @Override
    StreamDriver createStreamDriver(
        ModelProvider modelProvider,
        ThreadUtils threadUtils,
        Configuration configuration,
        Context context,
        SnackbarApi snackbarApi,
        ContentChangedListener contentChangedListener,
        ScrollRestorer scrollRestorer) {
      streamDriverScrollRestorer = scrollRestorer;
      return streamDriver;
    }

    @Override
    StreamContentChangedListener createStreamContentChangedListener() {
      contentChangedListener = new StreamContentChangedListener();
      return contentChangedListener;
    }

    @Override
    StreamRecyclerViewAdapter createRecyclerViewAdapter(
        Context context,
        CardConfiguration cardConfiguration,
        PietManager pietManager,
        ActionParser actionParser,
        ActionApi actionApi,
        ActionManager actionManager,
        DeepestContentTracker deepestContentTracker) {
      return adapter;
    }

    @Override
    StreamScrollMonitor createStreamScrollMonitor(
        RecyclerView recyclerView, ContentChangedListener contentChangedListener) {
      return streamScrollMonitor;
    }

    @Override
    LinearLayoutManager createRecyclerViewLayoutManager(Context context) {
      return layoutManager;
    }

    @Override
    ScrollRestorer createScrollRestorer(
        RecyclerView recyclerView, /*@Nullable*/ Bundle savedInstanceState) {
      return scrollRestorer;
    }

    @Override
    ScrollRestorer createNonRestoringScrollRestorer(RecyclerView recyclerView) {
      return nonRestoringScrollRestorer;
    }
  }

  private class LinearLayoutManagerWithFakePositioning extends LinearLayoutManager {

    private final List<View> childMap;
    private int firstVisiblePosition = RecyclerView.NO_POSITION;
    private int lastVisiblePosition = RecyclerView.NO_POSITION;

    public LinearLayoutManagerWithFakePositioning(Context context) {
      super(context);
      childMap = new ArrayList<>();
    }

    @Override
    public int findFirstVisibleItemPosition() {
      return firstVisiblePosition;
    }

    @Override
    public int findLastVisibleItemPosition() {
      return lastVisiblePosition;
    }

    @Override
    public View findViewByPosition(int i) {
      if (i < 0 || i >= childMap.size()) {
        return null;
      }
      return childMap.get(i);
    }

    private void addChildToPosition(int position, View child) {
      childMap.add(position, child);
    }
  }
}
