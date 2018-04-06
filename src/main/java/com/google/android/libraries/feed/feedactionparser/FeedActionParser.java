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

package com.google.android.libraries.feed.feedactionparser;

import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.feedactionparser.internal.PietFeedActionRetriever;
import com.google.android.libraries.feed.host.action.StreamActionApi;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata;
import com.google.search.now.ui.piet.ActionsProto.Action;
import javax.annotation.Nullable;

/**
 * Action parser which is able to parse Feed actions and notify clients about which action needs to
 * be performed.
 */
public class FeedActionParser implements ActionParser {

  private static final String TAG = "FeedActionParser";

  private final PietFeedActionRetriever pietFeedActionRetriever;

  public FeedActionParser() {
    this.pietFeedActionRetriever = new PietFeedActionRetriever();
  }

  @Override
  public void parseAction(
      Action action, StreamActionApi streamActionApi, /*@Nullable*/ String veLoggingToken) {
    FeedAction feedAction = pietFeedActionRetriever.getFeedAction(action);
    if (feedAction == null) {
      Logger.w(TAG, "Unable to get FeedAction from PietFeedActionRetriever");
      return;
    }
    parseFeedAction(feedAction, streamActionApi);
  }

  @Override
  public void parseFeedAction(FeedAction feedAction, StreamActionApi streamActionApi) {
    FeedActionMetadata feedActionMetadata = feedAction.getMetadata();
    switch (feedActionMetadata.getType()) {
      case OPEN_URL:
        if (!feedActionMetadata.getOpenUrlData().hasUrl()) {
          Logger.e(TAG, "Cannot open url: openUrlData is null, or the url is missing.");
          break;
        }
        streamActionApi.openUrl(feedActionMetadata.getOpenUrlData().getUrl());
        break;
      case OPEN_CONTEXT_MENU:
        if (!feedActionMetadata.hasOpenContextMenuData()) {
          Logger.e(TAG, "Cannot open context menu: does not have context menu data");
          break;
        }
        streamActionApi.openContextMenu(feedActionMetadata.getOpenContextMenuData());
        break;
      default:
        Logger.e(TAG, "Haven't implemented host handling of %s", feedActionMetadata.getType());
    }
  }
}
