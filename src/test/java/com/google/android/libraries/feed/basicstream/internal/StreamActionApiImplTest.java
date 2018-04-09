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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata.Type;
import com.google.search.now.ui.action.FeedActionProto.LabelledFeedActionData;
import com.google.search.now.ui.action.FeedActionProto.OpenContextMenuData;
import com.google.search.now.ui.action.FeedActionProto.OpenUrlData;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;

/** Tests for {@link StreamActionApiImpl}. */
@RunWith(RobolectricTestRunner.class)
public class StreamActionApiImplTest {

  private static final String URL = "www.google.com";
  private static final String OPEN_LABEL = "Open";
  private static final String OPEN_IN_NEW_WINDOW_LABEL = "Open in new window";
  private static final LabelledFeedActionData OPEN_IN_NEW_WINDOW =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_IN_NEW_WINDOW_LABEL)
          .setFeedAction(
              FeedAction.newBuilder()
                  .setMetadata(
                      FeedActionMetadata.newBuilder()
                          .setType(Type.OPEN_URL_NEW_WINDOW)
                          .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL))))
          .build();
  private static final String OPEN_IN_INCOGNITO_MODE_LABEL = "Open in incognito mode";
  public static final LabelledFeedActionData OPEN_IN_INCOGNITO_MODE =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_IN_INCOGNITO_MODE_LABEL)
          .setFeedAction(
              FeedAction.newBuilder()
                  .setMetadata(
                      FeedActionMetadata.newBuilder()
                          .setType(Type.OPEN_URL_INCOGNITO)
                          .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL))))
          .build();

  private static final LabelledFeedActionData NORMAL_OPEN_URL =
      LabelledFeedActionData.newBuilder()
          .setLabel(OPEN_LABEL)
          .setFeedAction(
              FeedAction.newBuilder()
                  .setMetadata(
                      FeedActionMetadata.newBuilder()
                          .setType(Type.OPEN_URL)
                          .setOpenUrlData(OpenUrlData.newBuilder().setUrl(URL))))
          .build();

  @Mock private ActionApi actionApi;
  @Mock private ActionParser actionParser;

  private StreamActionApiImpl streamActionApi;

  @Before
  public void setup() {
    initMocks(this);

    Context context = Robolectric.setupActivity(Activity.class);
    streamActionApi = new StreamActionApiImpl(context, actionApi, actionParser);
  }

  @Test
  public void testCanDismiss() {
    assertThat(streamActionApi.canDismiss()).isFalse();
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
    streamActionApi.downloadUrl(URL);
    verify(actionApi).downloadUrl(URL);
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
    when(actionParser.canPerformAction(any(FeedAction.class), eq(streamActionApi)))
        .thenReturn(true);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build());

    ShadowAlertDialog shadowAlertDialog =
        Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog());

    assertThat(shadowAlertDialog.getItems())
        .asList()
        .containsExactly(OPEN_LABEL, OPEN_IN_NEW_WINDOW_LABEL, OPEN_IN_INCOGNITO_MODE_LABEL);
    shadowAlertDialog.clickOnItem(0);
    shadowAlertDialog.clickOnItem(1);
    shadowAlertDialog.clickOnItem(2);

    InOrder inOrder = Mockito.inOrder(actionParser);

    inOrder.verify(actionParser).parseFeedAction(NORMAL_OPEN_URL.getFeedAction(), streamActionApi);
    inOrder
        .verify(actionParser)
        .parseFeedAction(OPEN_IN_NEW_WINDOW.getFeedAction(), streamActionApi);
    inOrder
        .verify(actionParser)
        .parseFeedAction(OPEN_IN_INCOGNITO_MODE.getFeedAction(), streamActionApi);
  }

  @Test
  public void openContextMenuTest_noNewWindow() {
    when(actionParser.canPerformAction(NORMAL_OPEN_URL.getFeedAction(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_INCOGNITO_MODE.getFeedAction(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_NEW_WINDOW.getFeedAction(), streamActionApi))
        .thenReturn(false);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build());

    ShadowAlertDialog shadowAlertDialog =
        Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog());

    assertThat(shadowAlertDialog.getItems())
        .asList()
        .containsExactly(OPEN_LABEL, OPEN_IN_INCOGNITO_MODE_LABEL);
  }

  @Test
  public void openContextMenuTest_noIncognitoWindow() {
    when(actionParser.canPerformAction(NORMAL_OPEN_URL.getFeedAction(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_NEW_WINDOW.getFeedAction(), streamActionApi))
        .thenReturn(true);
    when(actionParser.canPerformAction(OPEN_IN_INCOGNITO_MODE.getFeedAction(), streamActionApi))
        .thenReturn(false);

    List<LabelledFeedActionData> labelledFeedActionDataList = new ArrayList<>();
    labelledFeedActionDataList.add(NORMAL_OPEN_URL);
    labelledFeedActionDataList.add(OPEN_IN_NEW_WINDOW);
    labelledFeedActionDataList.add(OPEN_IN_INCOGNITO_MODE);

    streamActionApi.openContextMenu(
        OpenContextMenuData.newBuilder().addAllContextMenuData(labelledFeedActionDataList).build());

    ShadowAlertDialog shadowAlertDialog =
        Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog());

    assertThat(shadowAlertDialog.getItems())
        .asList()
        .containsExactly(OPEN_LABEL, OPEN_IN_NEW_WINDOW_LABEL);
  }
}
