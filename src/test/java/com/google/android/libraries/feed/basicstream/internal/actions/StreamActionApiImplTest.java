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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ListView;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.knowncontent.ContentMetadata;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.logging.ActionType;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.ContentLoggingData;
import com.google.android.libraries.feed.sharedstream.contextmenumanager.ContextMenuManager;
import com.google.android.libraries.feed.sharedstream.contextmenumanager.ContextMenuManager.ContextMenuClickHandler;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.ui.action.FeedActionPayloadProto.FeedActionPayload;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata.Type;
import com.google.search.now.ui.action.FeedActionProto.LabelledFeedActionData;
import com.google.search.now.ui.action.FeedActionProto.OpenContextMenuData;
import com.google.search.now.ui.action.FeedActionProto.OpenUrlData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamActionApiImpl}. */
@RunWith(RobolectricTestRunner.class)
public class StreamActionApiImplTest {

  private static final String URL = "www.google.com";
  private static final String OPEN_LABEL = "Open";
  private static final String OPEN_IN_NEW_WINDOW_LABEL = "Open in new window";
  private static final LabelledFeedActionData OPEN_IN_NEW_WINDOW =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_IN_NEW_WINDOW_LABEL)
          .setFeedActionPayload(
              FeedActionPayload.newBuilder()
                  .setExtension(
                      FeedAction.feedActionExtension,
                      FeedAction.newBuilder()
                          .setMetadata(
                              FeedActionMetadata.newBuilder()
                                  .setType(Type.OPEN_URL_NEW_WINDOW)
                                  .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL)))
                          .build()))
          .build();

  private static final String OPEN_IN_INCOGNITO_MODE_LABEL = "Open in incognito mode";

  private static final LabelledFeedActionData OPEN_IN_INCOGNITO_MODE =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_IN_INCOGNITO_MODE_LABEL)
          .setFeedActionPayload(
              FeedActionPayload.newBuilder()
                  .setExtension(
                      FeedAction.feedActionExtension,
                      FeedAction.newBuilder()
                          .setMetadata(
                              FeedActionMetadata.newBuilder()
                                  .setType(Type.OPEN_URL_INCOGNITO)
                                  .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL)))
                          .build()))
          .build();

  private static final LabelledFeedActionData NORMAL_OPEN_URL =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_LABEL)
          .setFeedActionPayload(
              FeedActionPayload.newBuilder()
                  .setExtension(
                      FeedAction.feedActionExtension,
                      FeedAction.newBuilder()
                          .setMetadata(
                              FeedActionMetadata.newBuilder()
                                  .setType(Type.OPEN_URL)
                                  .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL)))
                          .build()))
          .build();
  private static final String SESSION_ID = "SESSION_ID";
  private static final ContentMetadata CONTENT_METADATA =
      new ContentMetadata(
          URL,
          "title",
          /* timePublished= */ -1,
          /* imageUrl= */ null,
          /* publisher= */ null,
          /* faviconUrl= */ null,
          /* snippet=*/ null);

  @Mock private ActionApi actionApi;
  @Mock private ActionParser actionParser;
  @Mock private ActionManager actionManager;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private ContentLoggingData contentLoggingData;
  @Mock private ContextMenuManager contextMenuManager;

  @Captor private ArgumentCaptor<ContextMenuClickHandler> handlerCaptor;
  @Captor private ArgumentCaptor<List<String>> menuItemCaptor;

  private StreamActionApiImpl streamActionApi;
  private View view;

  @Before
  public void setup() {
    initMocks(this);

    Context context = Robolectric.setupActivity(Activity.class);
    view = new View(context);
    streamActionApi =
        new StreamActionApiImpl(
            actionApi,
            actionParser,
            actionManager,
            basicLoggingApi,
            contentLoggingData,
            contextMenuManager,
            SESSION_ID);
  }

  @Test
  public void testCanDismiss() {
    assertThat(streamActionApi.canDismiss()).isTrue();
  }

  @Test
  public void testDismiss() {
    String contentId = "contentId";
    List<StreamDataOperation> streamDataOperations =
        Collections.singletonList(StreamDataOperation.getDefaultInstance());
    streamActionApi.dismiss(contentId, streamDataOperations);

    verify(actionManager)
        .dismiss(Collections.singletonList(contentId), streamDataOperations, SESSION_ID);
    verify(basicLoggingApi).onContentDismissed(contentLoggingData);
  }

  @Test
  public void testOnClientAction() {
    streamActionApi.onClientAction(ActionType.OPEN_URL);

    verify(basicLoggingApi).onClientAction(contentLoggingData, ActionType.OPEN_URL);
  }

  @Test
  public void testOpenUrl() {
    streamActionApi.openUrlInNewWindow(URL);
    verify(actionApi).openUrlInNewWindow(URL);
  }

  @Test
  public void testCanOpenUrl() {
    when(actionApi.canOpenUrl()).thenReturn(true);
    assertThat(streamActionApi.canOpenUrl()).isTrue();

    when(actionApi.canOpenUrl()).thenReturn(false);
    assertThat(streamActionApi.canOpenUrl()).isFalse();
  }

  @Test
  public void testCanOpenUrlInIncognitoMode() {
    when(actionApi.canOpenUrlInIncognitoMode()).thenReturn(true);
    assertThat(streamActionApi.canOpenUrlInIncognitoMode()).isTrue();

    when(actionApi.canOpenUrlInIncognitoMode()).thenReturn(false);
    assertThat(streamActionApi.canOpenUrlInIncognitoMode()).isFalse();
  }

  @Test
  public void testOpenUrlInNewTab() {
    streamActionApi.openUrlInNewTab(URL);
    verify(actionApi).openUrlInNewTab(URL);
  }

  @Test
  public void testCanOpenUrlInNewTab() {
    when(actionApi.canOpenUrlInNewTab()).thenReturn(true);
    assertThat(streamActionApi.canOpenUrlInNewTab()).isTrue();

    when(actionApi.canOpenUrlInNewTab()).thenReturn(false);
    assertThat(streamActionApi.canOpenUrlInNewTab()).isFalse();
  }

  @Test
  public void testDownloadUrl() {
    streamActionApi.downloadUrl(CONTENT_METADATA);
    verify(actionApi).downloadUrl(CONTENT_METADATA);
  }

  @Test
  public void testCanDownloadUrl() {
    when(actionApi.canDownloadUrl()).thenReturn(true);
    assertThat(streamActionApi.canDownloadUrl()).isTrue();

    when(actionApi.canDownloadUrl()).thenReturn(false);
    assertThat(streamActionApi.canDownloadUrl()).isFalse();
  }

  @Test
  public void testLearnMore() {
    streamActionApi.learnMore();
    verify(actionApi).learnMore();
  }

  @Test
  public void testCanLearnMore() {
    when(actionApi.canLearnMore()).thenReturn(true);
    assertThat(streamActionApi.canLearnMore()).isTrue();

    when(actionApi.canLearnMore()).thenReturn(false);
    assertThat(streamActionApi.canLearnMore()).isFalse();
  }

  @Test
  public void openContextMenuTest() {
    when(actionParser.canPerformAction(any(FeedActionPayload.class), eq(streamActionApi)))
        .thenReturn(true);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build(),
        view);

    verify(contextMenuManager)
        .openContextMenu(eq(view), Mockito.<List<String>>any(), handlerCaptor.capture());

    handlerCaptor.getValue().handleClick(0);
    handlerCaptor.getValue().handleClick(1);
    handlerCaptor.getValue().handleClick(2);

    InOrder inOrder = Mockito.inOrder(actionParser);

    inOrder
        .verify(actionParser)
        .parseFeedActionPayload(NORMAL_OPEN_URL.getFeedActionPayload(), streamActionApi, view);
    inOrder
        .verify(actionParser)
        .parseFeedActionPayload(OPEN_IN_NEW_WINDOW.getFeedActionPayload(), streamActionApi, view);
    inOrder
        .verify(actionParser)
        .parseFeedActionPayload(
            OPEN_IN_INCOGNITO_MODE.getFeedActionPayload(), streamActionApi, view);
  }

  @Test
  public void openContextMenuTest_noNewWindow() {
    when(actionParser.canPerformAction(NORMAL_OPEN_URL.getFeedActionPayload(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(
            OPEN_IN_INCOGNITO_MODE.getFeedActionPayload(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_NEW_WINDOW.getFeedActionPayload(), streamActionApi))
        .thenReturn(false);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build(),
        view);

    verify(contextMenuManager)
        .openContextMenu(eq(view), menuItemCaptor.capture(), any(ContextMenuClickHandler.class));

    List<String> items = menuItemCaptor.getValue();
    assertThat(items.size()).isEqualTo(2);
    assertThat(items.get(0)).isEqualTo(OPEN_LABEL);
    assertThat(items.get(1)).isEqualTo(OPEN_IN_INCOGNITO_MODE_LABEL);
  }

  @Test
  public void openContextMenuTest_noIncognitoWindow() {
    when(actionParser.canPerformAction(NORMAL_OPEN_URL.getFeedActionPayload(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_NEW_WINDOW.getFeedActionPayload(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(
            OPEN_IN_INCOGNITO_MODE.getFeedActionPayload(), streamActionApi))
        .thenReturn(false);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build(),
        view);

    verify(contextMenuManager)
        .openContextMenu(eq(view), menuItemCaptor.capture(), any(ContextMenuClickHandler.class));

    List<String> items = menuItemCaptor.getValue();
    assertThat(items.size()).isEqualTo(2);
    assertThat(items.get(0)).isEqualTo(OPEN_LABEL);
    assertThat(items.get(1)).isEqualTo(OPEN_IN_NEW_WINDOW_LABEL);
  }

  @Test
  public void openContextMenuTest_logsContentContextMenuOpened() {
    when(actionParser.canPerformAction(any(FeedActionPayload.class), eq(streamActionApi)))
        .thenReturn(true);

    // Succeed when requesting a context menu.
    when(contextMenuManager.openContextMenu(
            any(View.class), Mockito.<List<String>>any(), any(ContextMenuClickHandler.class)))
        .thenReturn(true);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder()
            .addAllContextMenuData(Collections.singletonList(NORMAL_OPEN_URL))
            .build(),
        view);

    verify(basicLoggingApi).onContentContextMenuOpened(contentLoggingData);

    reset(basicLoggingApi);

    // Fail to open a context menu.
    when(contextMenuManager.openContextMenu(
            any(View.class), Mockito.<List<String>>any(), any(ContextMenuClickHandler.class)))
        .thenReturn(false);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder()
            .addAllContextMenuData(Collections.singletonList(NORMAL_OPEN_URL))
            .build(),
        view);

    verifyZeroInteractions(basicLoggingApi);
  }

  public String getItemLabelAt(ListView listView, int position) {
    return (String) listView.getAdapter().getItem(position);
  }
}
