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

package com.google.android.libraries.feed.basicstream.internal.actions;

import android.content.Context;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.knowncontent.ContentMetadata;
import com.google.android.libraries.feed.basicstream.internal.util.ContextMenuHelper;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.action.StreamActionApi;
import com.google.android.libraries.feed.host.logging.ActionType;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.ContentLoggingData;
import com.google.android.libraries.feed.sharedstream.publicapi.menumeasurer.MenuMeasurer;
import com.google.android.libraries.feed.sharedstream.publicapi.menumeasurer.Size;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.ui.action.FeedActionProto.LabelledFeedActionData;
import com.google.search.now.ui.action.FeedActionProto.OpenContextMenuData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Action handler for Stream. */
public class StreamActionApiImpl implements StreamActionApi {

  private static final String TAG = "StreamActionApiImpl";

  // Indicates how deep into the card the menu should be set if the menu is showing below a card. IE
  // 4 indicates that the menu should be placed 1/4 of the way from the edge of the card.
  private static final int FRACTION_FROM_EDGE = 4;

  // Indicates how far from the bottom of the card the menu should be set if the menu is showing
  // above a card.
  private static final int FRACTION_FROM_BOTTOM_EDGE = FRACTION_FROM_EDGE - 1;

  private final Context context;
  private final ActionApi actionApi;
  private final ActionParser actionParser;
  private final ActionManager actionManager;
  private final BasicLoggingApi basicLoggingApi;
  private final ContentLoggingData contentLoggingData;
  private final ContextMenuHelper contextMenuHelper;
  private final MenuMeasurer menuMeasurer;

  /*@Nullable*/ private final String sessionToken;

  public StreamActionApiImpl(
      Context context,
      ActionApi actionApi,
      ActionParser actionParser,
      ActionManager actionManager,
      BasicLoggingApi basicLoggingApi,
      ContentLoggingData contentLoggingData,
      ContextMenuHelper contextMenuHelper,
      MenuMeasurer menuMeasurer,
      /*@Nullable*/ String sessionToken) {
    this.context = context;
    this.actionApi = actionApi;
    this.actionParser = actionParser;
    this.actionManager = actionManager;
    this.basicLoggingApi = basicLoggingApi;
    this.contentLoggingData = contentLoggingData;
    this.contextMenuHelper = contextMenuHelper;
    this.menuMeasurer = menuMeasurer;
    this.sessionToken = sessionToken;
  }

  @Override
  public void openContextMenu(OpenContextMenuData openContextMenuData, View anchorView) {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
    List<LabelledFeedActionData> enabledActions = new ArrayList<>();
    for (LabelledFeedActionData labelledFeedActionData :
        openContextMenuData.getContextMenuDataList()) {
      if (actionParser.canPerformAction(labelledFeedActionData.getFeedActionPayload(), this)) {
        adapter.add(labelledFeedActionData.getLabel());
        enabledActions.add(labelledFeedActionData);
      }
    }

    ListView listView = new ListView(context);
    listView.setAdapter(adapter);
    listView.setBackgroundColor(Color.WHITE);
    listView.setDivider(null);
    listView.setDividerHeight(0);

    Size measurements =
        menuMeasurer.measureAdapterContent(
            listView,
            adapter,
            /* windowPadding= */ 0,
            contextMenuHelper.getStreamWidth(),
            contextMenuHelper.getStreamHeight());

    int dialogHeight = measurements.getHeight();
    PopupWindow popupWindow = new PopupWindow(listView, measurements.getWidth(), dialogHeight);
    popupWindow.setFocusable(true);
    popupWindow.setBackgroundDrawable(
        context.getResources().getDrawable(R.drawable.popup_window_bg));
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      popupWindow.setElevation(context.getResources().getDimension(R.dimen.popup_window_elevation));
    }

    listView.setOnItemClickListener(
        (parent, view, position, id) -> {
          actionParser.parseFeedActionPayload(
              enabledActions.get(position).getFeedActionPayload(),
              StreamActionApiImpl.this,
              anchorView);
          popupWindow.dismiss();
        });

    int[] anchorViewLocation = new int[2];
    anchorView.getLocationInWindow(anchorViewLocation);
    int anchorViewYInWindow = anchorViewLocation[1] - contextMenuHelper.getStreamYPosition();

    int yOffset =
        getYOffsetForContextMenu(
            dialogHeight,
            anchorViewYInWindow,
            anchorView.getHeight(),
            contextMenuHelper.getStreamHeight());

    popupWindow.showAsDropDown(anchorView, anchorView.getWidth() / FRACTION_FROM_EDGE, yOffset);

    // We want to prevent any more touch events from be used (if a user has yet to end their current
    // touch session). This prevents the possibility of scrolling after the context menu opens.
    ViewParent parent = anchorView.getParent();
    if (parent != null) {
      parent.requestDisallowInterceptTouchEvent(true);
    }
    basicLoggingApi.onContentContextMenuOpened(contentLoggingData);
  }

  /**
   * Gets the offset in pixels from the bottom of the anchorview to place the menu.
   *
   * <p>First, we attempt to place the top of the menu {@code 1/FRACTION_FROM_EDGE} down from the
   * top of the anchor view. If the menu cannot fit there, we attempt to put the bottom of the menu
   * {@code 1/FRACTION_FROM_EDGE} up from the bottom of the anchor view. If the menu can fit in
   * neither position it instead is centered in the stream.
   */
  // TODO: Add tests for each case here.
  private int getYOffsetForContextMenu(
      int dialogHeight, int anchorViewYInWindow, int anchorViewHeight, int windowHeight) {
    // Check if the menu can fit below the card.
    if (dialogHeight + anchorViewYInWindow + anchorViewHeight / FRACTION_FROM_EDGE < windowHeight) {
      // Check if 1/FRACTION_FROM_EDGE of the way down the card is visible.
      if (-FRACTION_FROM_EDGE * anchorViewYInWindow < anchorViewHeight) {
        // 1/FRACTION_FROM_EDGE of the way down the card is visible stream, so offset to that point,
        // from the bottom of the anchor view.
        return -FRACTION_FROM_BOTTOM_EDGE * anchorViewHeight / FRACTION_FROM_EDGE;
      } else {
        // 1/FRACTION_FROM_EDGE down from the top of the anchor view is not visible, offset the menu
        // to the top of the stream.
        return -1 * (anchorViewHeight + anchorViewYInWindow);
      }
      // Check if the menu can have the bottom 1/FRACTION_FROM_EDGE off from the bottom of the
      // anchor.
    } else if (anchorViewYInWindow - dialogHeight + anchorViewHeight / FRACTION_FROM_EDGE >= 0) {
      // Check if 1/FRACTION_FROM_EDGE from the bottom of the anchor is visible.
      if (anchorViewYInWindow + FRACTION_FROM_BOTTOM_EDGE * anchorViewHeight / FRACTION_FROM_EDGE
          < windowHeight) {
        // FRACTION_FROM_BOTTOM_EDGE/FRACTION_FROM_EDGE of the way down the card is visible, so
        // position the bottom there.
        return -(dialogHeight + anchorViewHeight / FRACTION_FROM_EDGE);
      } else {
        // Less than the top 1/FRACTION_FROM_EDGE of the card is on the screen. Offset so the menu
        // is at the bottom of the screen
        return -(dialogHeight + anchorViewHeight - (windowHeight - anchorViewYInWindow));
      }
    } else {
      // The menu will fit neither above, nor below the content. Center it in the middle of the
      // screen.
      return -(dialogHeight + anchorViewYInWindow - windowHeight / 2);
    }
  }

  @Override
  public boolean canOpenContextMenu() {
    return true;
  }

  @Override
  public boolean canDismiss() {
    return true;
  }

  @Override
  public void dismiss(String contentId, List<StreamDataOperation> dataOperations) {
    actionManager.dismiss(Collections.singletonList(contentId), dataOperations, sessionToken);
    basicLoggingApi.onContentDismissed(contentLoggingData);
  }

  @Override
  public void onClientAction(@ActionType int actionType) {
    basicLoggingApi.onClientAction(contentLoggingData, actionType);
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
    actionApi.openUrlInIncognitoMode(url);
  }

  @Override
  public boolean canOpenUrlInIncognitoMode() {
    return actionApi.canOpenUrlInIncognitoMode();
  }

  @Override
  public void openUrlInNewTab(String url) {
    actionApi.openUrlInNewTab(url);
  }

  @Override
  public boolean canOpenUrlInNewTab() {
    return actionApi.canOpenUrlInNewTab();
  }

  @Override
  public void openUrlInNewWindow(String url) {
    actionApi.openUrlInNewWindow(url);
  }

  @Override
  public boolean canOpenUrlInNewWindow() {
    return actionApi.canOpenUrlInNewWindow();
  }

  @Override
  public void downloadUrl(ContentMetadata contentMetadata) {
    actionApi.downloadUrl(contentMetadata);
  }

  @Override
  public boolean canDownloadUrl() {
    return actionApi.canDownloadUrl();
  }

  @Override
  public void learnMore() {
    actionApi.learnMore();
  }

  @Override
  public boolean canLearnMore() {
    return actionApi.canLearnMore();
  }
}
