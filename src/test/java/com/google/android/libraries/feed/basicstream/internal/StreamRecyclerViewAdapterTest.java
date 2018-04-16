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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.basicstream.internal.StreamRecyclerViewAdapter.ViewHolderBindingObserver;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.piet.FrameAdapter;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.common.collect.Lists;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.Content.Type;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
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
  private static final PietContent PIET_CONTENT = PietContent.getDefaultInstance();

  @Mock private CardConfiguration cardConfiguration;
  @Mock private PietManager pietManager;
  @Mock private ViewHolderBindingObserver viewHolderBindingObserver;
  @Mock private FrameAdapter frameAdapter;
  @Mock private ContentModel contentModel;

  private Context context;
  private List<View> headers;
  private ColorDrawable background;
  private LinearLayout frameContainer;
  private StreamRecyclerViewAdapter streamRecyclerViewAdapter;

  @SuppressWarnings("unchecked") // Needed for templated captures.
  @Before
  public void setUp() {
    initMocks(this);

    context = Robolectric.setupActivity(Activity.class);
    frameContainer = new LinearLayout(context);

    background = new ColorDrawable(Color.RED);
    when(cardConfiguration.getCardBackground()).thenReturn(background);
    when(cardConfiguration.getCardBottomPadding()).thenReturn((float) CARD_BOTTOM_PADDING);

    when(pietManager.createPietFrameAdapter(any(Supplier.class), eq(context)))
        .thenReturn(frameAdapter);
    when(frameAdapter.getFrameContainer()).thenReturn(frameContainer);

    headers = new ArrayList<>();
    for (int i = 0; i < HEADER_COUNT; ++i) {
      headers.add(new View(context));
    }
    streamRecyclerViewAdapter =
        new StreamRecyclerViewAdapter(
            context, cardConfiguration, pietManager, viewHolderBindingObserver);
    streamRecyclerViewAdapter.setHeaders(headers);

    ModelFeature modelFeature = mock(ModelFeature.class);
    when(modelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setCard(Card.newBuilder()).build());
    ModelCursor modelCursor = mock(ModelCursor.class);
    when(modelFeature.getCursor()).thenReturn(modelCursor);

    ModelFeature childModelFeature = mock(ModelFeature.class);
    when(childModelFeature.getStreamFeature()).thenReturn(createStreamFeature(PIET_CONTENT));

    ModelChild modelChild = mock(ModelChild.class);
    when(modelChild.getModelFeature()).thenReturn(childModelFeature);

    streamRecyclerViewAdapter.setContentModels(Lists.newArrayList(contentModel));
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
  public void testBindPiet() {
    FrameLayout parent = new FrameLayout(context);
    ViewHolder viewHolder =
        streamRecyclerViewAdapter.onCreateViewHolder(parent, ViewHolderType.TYPE_CARD);

    FrameLayout cardView = getCardView(viewHolder);
    cardView.setPadding(1, 2, 3, 4);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getPietBindingIndex(0));

    verify(contentModel).bind(viewHolder);
  }

  @Test
  public void testUnBindPiet() {
    PietViewHolder viewHolder = mock(PietViewHolder.class);

    streamRecyclerViewAdapter.onBindViewHolder(viewHolder, getPietBindingIndex(0));

    // Make sure the content model is bound
    verify(contentModel).bind(viewHolder);

    streamRecyclerViewAdapter.onViewRecycled(viewHolder);

    verify(viewHolder).unbind();
  }

  private StreamFeature createStreamFeature(PietContent pietContent) {
    return StreamFeature.newBuilder()
        .setContent(
            Content.newBuilder()
                .setType(Type.PIET)
                .setExtension(PietContent.pietContentExtension, pietContent))
        .build();
  }

  private FrameLayout getCardView(ViewHolder viewHolder) {
    return (FrameLayout) viewHolder.itemView;
  }

  private int getPietBindingIndex(int index) {
    return HEADER_COUNT + index;
  }
}
