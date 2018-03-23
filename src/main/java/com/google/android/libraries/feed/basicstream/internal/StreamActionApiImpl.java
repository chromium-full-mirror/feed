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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.action.StreamActionApi;
import com.google.search.now.ui.action.FeedActionProto.LabelledFeedActionData;
import com.google.search.now.ui.action.FeedActionProto.OpenContextMenuData;
import java.util.ArrayList;
import java.util.List;

/** Action handler for Stream. */
public class StreamActionApiImpl implements StreamActionApi {

  private final Context context;
  private final ActionApi actionApi;
  private final ActionParser actionParser;

  public StreamActionApiImpl(Context context, ActionApi actionApi, ActionParser actionParser) {
    this.context = context;
    this.actionApi = actionApi;
    this.actionParser = actionParser;
  }

  @Override
  public void openContextMenu(OpenContextMenuData openContextMenuData) {
    AlertDialog.Builder dialogBuilder = new Builder(context);
    List<CharSequence> items = new ArrayList<>();
    for (LabelledFeedActionData labelledFeedActionData :
        openContextMenuData.getContextMenuDataList()) {
      items.add(labelledFeedActionData.getLabel());
    }
    CharSequence[] itemArray = new String[items.size()];
    dialogBuilder.setItems(
        items.toArray(itemArray),
        (dialogInterface, i) ->
            actionParser.parseFeedAction(
                openContextMenuData.getContextMenuDataList().get(i).getFeedAction(),
                StreamActionApiImpl.this));
    // TODO: Make dialog show up near the click location.
    dialogBuilder.create().show();
  }

  @Override
  public void openUrl(String url) {
    actionApi.openUrl(url);
  }

  @Override
  public boolean canOpenUrl() {
    return actionApi.canOpenUrl();
  }

  @Override
  public void openUrlInIncognitoMode(String url) {
    // TODO: Implement open in Incognito Mode.
  }

  @Override
  public boolean canOpenUrlInIncognitoMode() {
    return false;
  }

  @Override
  public void openUrlInNewTab(String url) {
    // TODO: Implement open in new tab.
  }

  @Override
  public boolean canOpenUrlInNewTab() {
    return false;
  }

  @Override
  public void openUrlInNewWindow(String url) {
    // TODO: Implement open in new window.
  }

  @Override
  public boolean canOpenUrlInNewWindow() {
    return false;
  }

  @Override
  public void downloadUrl(String url) {
    // TODO: Implement download.
  }

  @Override
  public boolean canDownloadUrl() {
    return false;
  }

  @Override
  public void learnMore() {
    // TODO: Implement learn more.
  }

  @Override
  public boolean canLearnMore() {
    return false;
  }
}
