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

import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.stream.CardConfiguration;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.FrameAdapter;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.Content.Type;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import java.util.ArrayList;
import java.util.List;

/** A RecyclerView adapter which can show a list of views with Piet Stream features. */
public class StreamRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {
  private static final String TAG = "StreamRecyclerViewAdapt";

  private final Context context;
  private final CardConfiguration cardConfiguration;
  private final PietManager pietManager;
  // TODO: convert to ModelChild so the adapter can properly add a spinner
  private final List<PietContent> pietContents;
  private final List<View> headers;

  private final ViewHolderBindingObserver viewHolderBindingObserver;

  private static final int TYPE_HEADER = 0;
  private static final int TYPE_CARD = 1;

  /** A callback called when the last item is bound to a view holder */
  public interface ViewHolderBindingObserver {
    /** called when the last item in the model in bound */
    void onBindLastItem();
  }

  public StreamRecyclerViewAdapter(
      Context context,
      CardConfiguration cardConfiguration,
      PietManager pietManager,
      ViewHolderBindingObserver viewHolderBindingObserver) {
    this.context = context;
    this.cardConfiguration = cardConfiguration;
    this.pietManager = pietManager;
    this.viewHolderBindingObserver = viewHolderBindingObserver;
    headers = new ArrayList<>();
    pietContents = new ArrayList<>();
    setHasStableIds(true);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      FrameLayout frameLayout = new FrameLayout(parent.getContext());
      frameLayout.setLayoutParams(
          new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
      return new HeaderViewHolder(frameLayout);
    }

    // create card views
    final CardView cardView = createCardView();
    FrameAdapter frameAdapter = pietManager.createPietFrameAdapter(() -> cardView, context);
    cardView.addView(frameAdapter.getFrameContainer());
    return new PietViewHolder(cardView, frameAdapter);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int i) {
    if (isHeader(i)) {
      injectHeader((HeaderViewHolder) viewHolder, i);
      return;
    }
    Logger.d(TAG, "onBindViewHolder %s", i);
    PietContent pietContent = pietContents.get(positionToStreamIndex(i));
    ((PietViewHolder) viewHolder).bind(pietContent.getFrame());

    if (i == getItemCount() - 1) {
      // when binding the last item, check to see if it's a continuation token
      viewHolderBindingObserver.onBindLastItem();
    }
  }

  private void injectHeader(HeaderViewHolder headerViewHolder, int position) {
    View header = headers.get(position);
    if (header.getParent() == null) {
      headerViewHolder.frameLayout.addView(header);
    }
  }

  @Override
  public void onViewRecycled(ViewHolder viewHolder) {
    if (viewHolder instanceof HeaderViewHolder) {
      Logger.d(TAG, "onViewRecycled - HeaderViewHolder");
      ((HeaderViewHolder) viewHolder).frameLayout.removeAllViews();
    } else {
      Logger.d(TAG, "onViewRecycled - PietViewHolder");
      ((PietViewHolder) viewHolder).unbind();
    }
  }

  @Override
  public int getItemCount() {
    return pietContents.size() + headers.size();
  }

  @Override
  public int getItemViewType(int position) {
    if (isHeader(position)) {
      return TYPE_HEADER;
    }

    return TYPE_CARD;
  }

  @Override
  public long getItemId(int position) {
    if (isHeader(position)) {
      return headers.get(position).hashCode();
    }

    return pietContents.get(positionToStreamIndex(position)).hashCode();
  }

  private boolean isHeader(int position) {
    return position < headers.size();
  }

  private int positionToStreamIndex(int position) {
    return position - headers.size();
  }

  private CardView createCardView() {
    CardView cardView = new CardView(context);
    cardView.setRadius(cardConfiguration.getDefaultCornerRadius());
    cardView.setCardBackgroundColor(Color.WHITE);
    cardView.setCardElevation(cardConfiguration.getDefaultCardElevation());
    cardView.setMaxCardElevation(cardConfiguration.getMaxDefaultCardElevation());
    cardView.setPreventCornerOverlap(false);

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.bottomMargin = (int) cardConfiguration.getCardBottomPadding();
    cardView.setLayoutParams(lp);
    return cardView;
  }

  public void setHeaders(List<View> newHeaders) {
    headers.clear();
    headers.addAll(newHeaders);
    notifyDataSetChanged();
  }

  public void setModelFeatures(List<ModelFeature> newModelFeatures) {
    pietContents.clear();
    addPietContent(newModelFeatures);
  }

  public void addModelFeatures(List<ModelFeature> newModelFeatures) {
    addPietContent(newModelFeatures);
  }

  private void addPietContent(List<ModelFeature> newModelFeatures) {
    for (ModelFeature modelFeature : newModelFeatures) {
      if (!modelFeature.getStreamFeature().hasCard()) {
        throw new RuntimeException("Expected card feature as a StreamFeature");
      }

      ModelCursor cardCursor = modelFeature.getCursor();
      while (cardCursor.advanceCursor(
          child -> {
            StreamFeature streamFeature = child.getModelFeature().getStreamFeature();
            checkState(streamFeature.hasContent(), "Expected content for feature");

            Content content = streamFeature.getContent();
            checkState(content.getType() == Type.PIET, "Expected Piet type for feature");

            checkState(
                content.hasExtension(PietContent.pietContentExtension),
                "Expected Piet content for feature");
            pietContents.add(content.getExtension(PietContent.pietContentExtension));
          })) {}
    }
    notifyDataSetChanged();
  }

  static class HeaderViewHolder extends ViewHolder {

    private final FrameLayout frameLayout;

    public HeaderViewHolder(FrameLayout itemView) {
      super(itemView);
      this.frameLayout = itemView;
    }
  }

  static class PietViewHolder extends ViewHolder {

    private final FrameAdapter frameAdapter;
    private boolean bound;

    public PietViewHolder(View itemView, FrameAdapter frameAdapter) {
      super(itemView);
      this.frameAdapter = frameAdapter;
    }

    void bind(Frame frame) {
      if (bound) {
        return;
      }
      frameAdapter.bindModel(frame, (ShardingControl) null);
      bound = true;
    }

    void unbind() {
      if (!bound) {
        return;
      }
      frameAdapter.unbindModel();
      bound = false;
    }
  }
}
