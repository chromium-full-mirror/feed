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

package com.google.android.libraries.feed.basicstream.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.drivers.LeafFeatureDriver;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.NoContentViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ZeroStateViewHolder;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.piet.FrameAdapter;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamRecyclerViewAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class StreamRecyclerViewAdapterTest {

  private static final int HEADER_COUNT = 2;
  private static final long FEATURE_DRIVER_1_ID = 123;
  private static final long FEATURE_DRIVER_2_ID = 321;

  @Mock private CardConfiguration cardConfiguration;
  @Mock private PietManager pietManager;
  @Mock private FrameAdapter frameAdapter;
  @Mock private StreamDriver driver;
  @Mock private LeafFeatureDriver initialFeatureDriver;
  @Mock private LeafFeatureDriver featureDriver1;
  @Mock private LeafFeatureDriver featureDriver2;
  @Mock private ActionParser actionParser;
  @Mock private ActionApi actionApi;
  @Mock private ActionManager actionManager;
  @Mock private ContentChangedListener contentChangedListener;
  private Context context;
  private LinearLayout frameContainer;
  private StreamRecyclerViewAdapter streamRecyclerViewAdapter;
  private ArrayList headers;
  private StreamAdapterObserver streamAdapterObserver;

  @SuppressWarnings("unchecked") // Needed for templated captures.
  @Before
  public void setUp() {
    initMocks(this);

    context = Robolectric.setupActivity(Activity.class);
    frameContainer = new LinearLayout(context);

    when(pietManager.createPietFrameAdapter(
            any(Supplier.class), any(ActionHandler.class), eq(context)))
        .thenReturn(frameAdapter);
    when(frameAdapter.getFrameContainer()).thenReturn(frameContainer);

    when(featureDriver1.itemId()).thenReturn(FEATURE_DRIVER_1_ID);
    when(featureDriver2.itemId()).thenReturn(FEATURE_DRIVER_2_ID);

    headers = new ArrayList<>();
    for (int i = 0; i < HEADER_COUNT; ++i) {
      headers.add(new View(context));
    }
    streamRecyclerViewAdapter =
        new StreamRecyclerViewAdapter(
            context,
            cardConfiguration,
            pietManager,
            actionParser,
            actionApi,
            actionManager,
            contentChangedListener);
    streamRecyclerViewAdapter.setHeaders(headers);

    when(driver.getLeafFeatureDrivers()).thenReturn(Lists.newArrayList(initialFeatureDriver));
    streamRecyclerViewAdapter.setDriver(driver);

    streamAdapterObserver = new StreamAdapterObserver();
    streamRecyclerViewAdapter.registerAdapterDataObserver(streamAdapterObserver);
  }

  @Test
  public void testCreateViewHolderPiet() {
    FrameLayout parent = new FrameLayout(context);
    ViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_CARD);

    FrameLayout cardView = getCardView(viewHolder);
    assertThat(cardView.getChildAt(0)).isEqualTo(frameContainer);
  }

  @Test
  public void testCreateViewHolderContinuation() {
    FrameLayout parent = new FrameLayout(context);
    ViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_CONTINUATION);
    FrameLayout viewHolderFrameLayout = getCardView(viewHolder);

    assertThat(viewHolder).isInstanceOf(ContinuationViewHolder.class);
    assertThat(viewHolderFrameLayout.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(viewHolderFrameLayout.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
  }

  @Test
  public void testCreateViewHolderNoContent() {
    FrameLayout parent = new FrameLayout(context);
    ViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_NO_CONTENT);
    FrameLayout viewHolderFrameLayout = getCardView(viewHolder);

    assertThat(viewHolder).isInstanceOf(NoContentViewHolder.class);
    assertThat(viewHolderFrameLayout.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(viewHolderFrameLayout.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
  }

  @Test
  public void testCreateViewHolderZeroState() {
    FrameLayout parent = new FrameLayout(context);
    ViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_ZERO_STATE);
    FrameLayout viewHolderFrameLayout = getCardView(viewHolder);

    assertThat(viewHolder).isInstanceOf(ZeroStateViewHolder.class);
    assertThat(viewHolderFrameLayout.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(viewHolderFrameLayout.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
  }

  @Test
  public void testOnBindViewHolder() {
    FrameLayout parent = new FrameLayout(context);
    FeedViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_CARD);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getContentBindingIndex(0));

    verify(initialFeatureDriver).bind(viewHolder);
  }

  @Test
  public void testOnViewRecycled() {
    PietViewHolder viewHolder = mock(PietViewHolder.class);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getContentBindingIndex(0));

    // Make sure the content model is bound
    verify(initialFeatureDriver).bind(viewHolder);

    streamRecyclerViewAdapter.onViewRecycled(viewHolder);

    verify(initialFeatureDriver).unbind();
  }

  @Test
  public void testSetDriver_initialContentModels() {
    // streamRecyclerViewAdapter.setDriver(driver) is called in setup()
    assertThat(streamRecyclerViewAdapter.getLeafFeatureDrivers())
        .containsExactly(initialFeatureDriver);
  }

  @Test
  public void testSetDriver_newDriver() {
    StreamDriver newDriver = mock(StreamDriver.class);
    List<LeafFeatureDriver> newFeatureDrivers = Lists.newArrayList(featureDriver1, featureDriver2);

    when(newDriver.getLeafFeatureDrivers()).thenReturn(newFeatureDrivers);

    streamRecyclerViewAdapter.setDriver(newDriver);

    verify(driver).setStreamContentListener(null);
    assertThat(streamRecyclerViewAdapter.getItemId(getContentBindingIndex(0)))
        .isEqualTo(FEATURE_DRIVER_1_ID);
    assertThat(streamRecyclerViewAdapter.getItemId(getContentBindingIndex(1)))
        .isEqualTo(FEATURE_DRIVER_2_ID);
  }

  @Test
  public void testNotifyContentsAdded_endOfList() {
    streamRecyclerViewAdapter.notifyContentsAdded(
        1, Lists.newArrayList(featureDriver1, featureDriver2));
    assertThat(streamRecyclerViewAdapter.getLeafFeatureDrivers())
        .containsAllOf(initialFeatureDriver, featureDriver1, featureDriver2);
  }

  @Test
  public void testNotifyContentsAdded_startOfList() {
    streamRecyclerViewAdapter.notifyContentsAdded(
        0, Lists.newArrayList(featureDriver1, featureDriver2));
    assertThat(streamRecyclerViewAdapter.getLeafFeatureDrivers())
        .containsAllOf(featureDriver1, featureDriver2, initialFeatureDriver);
  }

  @Test
  public void testNotifyContentsRemoved() {
    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentRemoved(0);
    assertThat(streamRecyclerViewAdapter.getLeafFeatureDrivers()).isEmpty();
  }

  @Test
  public void testNotifyContentsAdded_notifiesListener() {
    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentsAdded(
        0, Lists.newArrayList(featureDriver1, featureDriver2));
    assertThat(streamAdapterObserver.insertedStart).isEqualTo(headers.size());
    assertThat(streamAdapterObserver.insertedCount).isEqualTo(2);
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void testNotifyContentsAdded_streamVisibleFalse() {
    streamRecyclerViewAdapter.setStreamContentVisible(false);
    streamAdapterObserver.reset();

    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentsAdded(
        0, Lists.newArrayList(featureDriver1, featureDriver2));
    assertThat(streamAdapterObserver.insertedStart).isEqualTo(0);
    assertThat(streamAdapterObserver.insertedCount).isEqualTo(0);
    verify(contentChangedListener, never()).onContentChanged();
  }

  @Test
  public void testNotifyContentsRemoved_notifiesListener() {
    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentRemoved(0);
    assertThat(streamAdapterObserver.removedStart).isEqualTo(headers.size());
    assertThat(streamAdapterObserver.removedCount).isEqualTo(1);
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void testNotifyContentsRemoved_streamVisibleFalse() {
    streamRecyclerViewAdapter.setStreamContentVisible(false);
    streamAdapterObserver.reset();

    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentRemoved(0);
    assertThat(streamAdapterObserver.removedStart).isEqualTo(0);
    assertThat(streamAdapterObserver.removedCount).isEqualTo(0);
    verify(contentChangedListener, never()).onContentChanged();
  }

  @Test
  public void testSetHeaders_notifiesListener() {
    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.setHeaders(headers);
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void testNotifyContentsAdded_noOpWithZeroLength() {
    // Listener triggered in setup.
    reset(contentChangedListener);
    streamRecyclerViewAdapter.notifyContentsAdded(0, new ArrayList<>());
    verifyNoMoreInteractions(contentChangedListener);
  }

  @Test
  public void testGetItemCount() {
    assertThat(streamRecyclerViewAdapter.getItemCount()).isEqualTo(headers.size() + 1);
  }

  @Test
  public void testGetItemCount_streamVisibleFalse() {
    streamRecyclerViewAdapter.setStreamContentVisible(false);
    assertThat(streamRecyclerViewAdapter.getItemCount()).isEqualTo(headers.size());
  }

  @Test
  public void setStreamContentVisible_hidesContent() {
    reset(contentChangedListener);

    streamRecyclerViewAdapter.setStreamContentVisible(false);
    assertThat(streamAdapterObserver.removedStart).isEqualTo(headers.size());
    assertThat(streamAdapterObserver.removedCount).isEqualTo(1);
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void setStreamContentVisible_hidesContent_doubleCall() {
    reset(contentChangedListener);

    streamRecyclerViewAdapter.setStreamContentVisible(false);

    streamAdapterObserver.reset();
    streamRecyclerViewAdapter.setStreamContentVisible(false);

    // Nothing additional should get called
    assertThat(streamAdapterObserver.removedStart).isEqualTo(0);
    assertThat(streamAdapterObserver.removedCount).isEqualTo(0);

    // Only a single content changed should get called
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void setStreamContentVisible_showsContent() {
    streamRecyclerViewAdapter.setStreamContentVisible(false);
    streamAdapterObserver.reset();
    reset(contentChangedListener);

    streamRecyclerViewAdapter.setStreamContentVisible(true);
    assertThat(streamAdapterObserver.insertedStart).isEqualTo(headers.size());
    assertThat(streamAdapterObserver.insertedCount).isEqualTo(1);
    verify(contentChangedListener).onContentChanged();
  }

  @Test
  public void setStreamContentVisible_showsContent_doubleCall() {
    streamRecyclerViewAdapter.setStreamContentVisible(false);
    streamAdapterObserver.reset();
    reset(contentChangedListener);

    streamRecyclerViewAdapter.setStreamContentVisible(true);

    streamAdapterObserver.reset();
    streamRecyclerViewAdapter.setStreamContentVisible(true);

    // Nothing additional should get called
    assertThat(streamAdapterObserver.insertedStart).isEqualTo(0);
    assertThat(streamAdapterObserver.insertedCount).isEqualTo(0);

    // Only a single content changed should get called
    verify(contentChangedListener).onContentChanged();
  }

  private FrameLayout getCardView(ViewHolder viewHolder) {
    return (FrameLayout) viewHolder.itemView;
  }

  private int getContentBindingIndex(int index) {
    return HEADER_COUNT + index;
  }

  private class StreamAdapterObserver extends RecyclerView.AdapterDataObserver {

    private int insertedStart;
    private int insertedCount;
    private int removedStart;
    private int removedCount;

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      insertedStart = positionStart;
      insertedCount = itemCount;
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
      removedStart = positionStart;
      removedCount = itemCount;
    }

    public void reset() {
      insertedStart = 0;
      insertedCount = 0;
      removedStart = 0;
      removedCount = 0;
    }
  }
}
