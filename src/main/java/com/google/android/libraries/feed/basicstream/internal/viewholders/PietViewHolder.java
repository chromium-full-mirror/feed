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

package com.google.android.libraries.feed.basicstream.internal.viewholders;

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamBasicLoggingApi;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamContentLoggingData;
import com.google.android.libraries.feed.basicstream.internal.logging.VisibilityListener;
import com.google.android.libraries.feed.basicstream.internal.logging.VisibilityMonitor;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.host.action.StreamActionApi;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.piet.FrameAdapter;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.host.ActionHandler.ActionType;
import com.google.search.now.ui.action.FeedActionPayloadProto.FeedActionPayload;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import java.util.List;

/**
 * {@link ViewHolder} for {@link com.google.search.now.ui.stream.StreamStructureProto.PietContent}.
 */
public class PietViewHolder extends FeedViewHolder {

  private static final String TAG = "PietViewHolder";
  private final ActionParser actionParser;
  private final CardConfiguration cardConfiguration;
  private final FrameLayout cardView;
  private final FrameAdapter frameAdapter;
  private final StreamActionApi streamActionApi;
  private final VisibilityMonitor visibilityMonitor;
  private boolean bound;

  /*@Nullable*/ private StreamContentLoggingData contentLoggingData;
  /*@Nullable*/ private FeedActionPayload swipeAction;

  public PietViewHolder(
      CardConfiguration cardConfiguration,
      FrameLayout cardView,
      PietManager pietManager,
      Context context,
      StreamActionApi streamActionApi,
      ActionParser actionParser,
      StreamBasicLoggingApi basicLoggingApi,
      Configuration configuration) {
    super(cardView);
    this.actionParser = actionParser;
    this.cardConfiguration = cardConfiguration;
    this.cardView = cardView;
    this.streamActionApi = streamActionApi;
    this.frameAdapter =
        pietManager.createPietFrameAdapter(
            () -> cardView,
            (action, actionType, frame, view, veLoggingToken) -> {
              if (actionType == ActionType.CLICK) {
                basicLoggingApi.onContentClicked(getContentLoggingData());
              }
              actionParser.parseAction(action, streamActionApi, view, veLoggingToken);
            },
            context);
    visibilityMonitor =
        createVisibilityMonitor(
            cardView, configuration.getValueOrDefault(ConfigKey.VIEW_LOG_THRESHOLD, .66));
    cardView.addView(frameAdapter.getFrameContainer());
  }

  public void bind(
      Frame frame,
      List<PietSharedState> pietSharedStates,
      StreamContentLoggingData contentLoggingData,
      FeedActionPayload swipeAction,
      VisibilityListener visibilityListener) {
    if (bound) {
      return;
    }
    this.contentLoggingData = contentLoggingData;
    visibilityMonitor.setListener(visibilityListener);
    this.swipeAction = swipeAction;

    // Need to reset padding here.  Setting a background can affect padding so if we switch from
    // a background which has padding to one that does not, then the padding needs to be removed.
    cardView.setPadding(0, 0, 0, 0);
    cardView.setBackground(cardConfiguration.getCardBackground());
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.bottomMargin = cardConfiguration.getCardBottomMargin();
    cardView.setLayoutParams(lp);

    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    bound = true;
  }

  @Override
  public void unbind() {
    if (!bound) {
      return;
    }

    contentLoggingData = null;
    swipeAction = null;
    visibilityMonitor.setListener(null);
    frameAdapter.unbindModel();
    bound = false;
  }

  public boolean canSwipe() {
    return swipeAction != null && !swipeAction.equals(FeedActionPayload.getDefaultInstance());
  }

  public void swipeToDismissPerformed() {
    if (swipeAction == null) {
      Logger.wtf(TAG, "Swipe performed on unbound ViewHolder.");
      return;
    }
    actionParser.parseFeedActionPayload(swipeAction, streamActionApi, itemView);
  }

  @VisibleForTesting
  VisibilityMonitor createVisibilityMonitor(
      /*@UnderInitialization*/ PietViewHolder this, View view, double viewLogThreshold) {
    return new VisibilityMonitor(view, viewLogThreshold);
  }

  private StreamContentLoggingData getContentLoggingData(
      /*@UnknownInitialization*/ PietViewHolder this) {
    return checkNotNull(
        contentLoggingData, "Logging data can only be retrieved once view holder has been bound.");
  }
}
