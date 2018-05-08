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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;
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

  private static final int CARD_BOTTOM_PADDING = 42;
  private static final int HEADER_COUNT = 2;
  private static final int CONTENT_MODEL_1_ID = 123;
  private static final int CONTENT_MODEL_2_ID = 321;

  @Mock private CardConfiguration cardConfiguration;
  @Mock private PietManager pietManager;
  @Mock private FrameAdapter frameAdapter;
  @Mock private StreamDriver driver;
  @Mock private ContentModel initialContentModel;
  @Mock private ContentModel contentModel1;
  @Mock private ContentModel contentModel2;
  @Mock private ActionParser actionParser;
  @Mock private ActionApi actionApi;
  @Mock private ActionManager actionManager;

  private Context context;
  private List<View> headers;
  private LinearLayout frameContainer;
  private StreamRecyclerViewAdapter streamRecyclerViewAdapter;

  @SuppressWarnings("unchecked") // Needed for templated captures.
  @Before
  public void setUp() {
    initMocks(this);

    context = Robolectric.setupActivity(Activity.class);
    frameContainer = new LinearLayout(context);

    when(cardConfiguration.getCardBottomPadding()).thenReturn((float) CARD_BOTTOM_PADDING);

    when(pietManager.createPietFrameAdapter(
            any(Supplier.class), any(ActionHandler.class), eq(context)))
        .thenReturn(frameAdapter);
    when(frameAdapter.getFrameContainer()).thenReturn(frameContainer);

    when(contentModel1.itemId()).thenReturn(CONTENT_MODEL_1_ID);
    when(contentModel2.itemId()).thenReturn(CONTENT_MODEL_2_ID);

    headers = new ArrayList<>();
    for (int i = 0; i < HEADER_COUNT; ++i) {
      headers.add(new View(context));
    }
    streamRecyclerViewAdapter =
        new StreamRecyclerViewAdapter(
            context, cardConfiguration, pietManager, actionParser, actionApi, actionManager);
    streamRecyclerViewAdapter.setHeaders(headers);

    when(driver.getContentModels()).thenReturn(Lists.newArrayList(initialContentModel));
    streamRecyclerViewAdapter.setDriver(driver);
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
  public void testBindPiet() {
    FrameLayout parent = new FrameLayout(context);
    FeedViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_CARD);

    FrameLayout cardView = getCardView(viewHolder);
    cardView.setPadding(1, 2, 3, 4);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getPietBindingIndex(0));

    verify(initialContentModel).bind(viewHolder);
  }

  @Test
  public void testUnBindPiet() {
    PietViewHolder viewHolder = mock(PietViewHolder.class);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getPietBindingIndex(0));

    // Make sure the content model is bound
    verify(initialContentModel).bind(viewHolder);

    streamRecyclerViewAdapter.onViewRecycled(viewHolder);

    verify(viewHolder).unbind();
  }

  @Test
  public void testSetDriver_initialContentModels() {
    // streamRecyclerViewAdapter.setDriver(driver) is called in setup()
    assertThat(streamRecyclerViewAdapter.getContentModels()).containsExactly(initialContentModel);
  }

  @Test
  public void testSetDriver_newDriver() {
    StreamDriver newDriver = mock(StreamDriver.class);
    List<ContentModel> newContentModels = Lists.newArrayList(contentModel1, contentModel2);

    when(newDriver.getContentModels()).thenReturn(newContentModels);

    streamRecyclerViewAdapter.setDriver(newDriver);

    verify(driver).setStreamContentListener(null);
    assertThat(streamRecyclerViewAdapter.getItemId(getPietBindingIndex(0)))
        .isEqualTo(CONTENT_MODEL_1_ID);
    assertThat(streamRecyclerViewAdapter.getItemId(getPietBindingIndex(1)))
        .isEqualTo(CONTENT_MODEL_2_ID);
  }

  @Test
  public void testNotifyContentsAdded_endOfList() {
    streamRecyclerViewAdapter.notifyContentsAdded(
        1, Lists.newArrayList(contentModel1, contentModel2));
    assertThat(streamRecyclerViewAdapter.getContentModels())
        .containsAllOf(initialContentModel, contentModel1, contentModel2);
  }

  @Test
  public void testNotifyContentsAdded_startOfList() {
    streamRecyclerViewAdapter.notifyContentsAdded(
        0, Lists.newArrayList(contentModel1, contentModel2));
    assertThat(streamRecyclerViewAdapter.getContentModels())
        .containsAllOf(contentModel1, contentModel2, initialContentModel);
  }

  @Test
  public void testNotifyContentsRemoved() {
    streamRecyclerViewAdapter.notifyContentRemoved(0);
    assertThat(streamRecyclerViewAdapter.getContentModels()).isEmpty();
  }

  private FrameLayout getCardView(ViewHolder viewHolder) {
    return (FrameLayout) viewHolder.itemView;
  }

  private int getPietBindingIndex(int index) {
    return HEADER_COUNT + index;
  }
}
