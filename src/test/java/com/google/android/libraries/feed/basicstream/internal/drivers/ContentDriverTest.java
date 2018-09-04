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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.actionparser.ActionParserFactory;
import com.google.android.libraries.feed.api.knowncontent.ContentMetadata;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.logging.LoggingListener;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamContentLoggingData;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.action.StreamActionApi;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.ContentLoggingData;
import com.google.android.libraries.feed.sharedstream.offlinemonitor.StreamOfflineMonitor;
import com.google.common.collect.ImmutableList;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.ui.action.FeedActionPayloadProto.FeedActionPayload;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.stream.StreamStructureProto.BasicLoggingMetadata;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.Content.Type;
import com.google.search.now.ui.stream.StreamStructureProto.OfflineMetadata;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import com.google.search.now.ui.stream.StreamStructureProto.RepresentationData;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.util.ArrayList;
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

/** Tests for {@link ContentDriver}. */
@RunWith(RobolectricTestRunner.class)
public class ContentDriverTest {

  private static final int POSITION = 1;
  private static final String CONTENT_URL = "google.com";
  private static final String CONTENT_TITLE = "title";

  private static final BasicLoggingMetadata BASIC_LOGGING_METADATA =
      BasicLoggingMetadata.newBuilder()
          .setScore(40)
          .build();
  private static final RepresentationData REPRESENTATION_DATA =
      RepresentationData.newBuilder().setUri(CONTENT_URL).setPublishedTimeSeconds(10).build();
  private static final ContentLoggingData CONTENT_LOGGING_DATA =
      new StreamContentLoggingData(POSITION, BASIC_LOGGING_METADATA, REPRESENTATION_DATA);
  private static final OfflineMetadata OFFLINE_METADATA =
      OfflineMetadata.newBuilder().setTitle(CONTENT_TITLE).build();

  private static final ContentId CONTENT_ID_1 =
      ContentId.newBuilder()
          .setContentDomain("piet-shared-state")
          .setId(2)
          .setTable("piet-shared-state")
          .build();
  private static final ContentId CONTENT_ID_2 =
      ContentId.newBuilder()
          .setContentDomain("piet-shared-state")
          .setId(3)
          .setTable("piet-shared-state")
          .build();
  private static final PietContent PIET_CONTENT =
      PietContent.newBuilder()
          .setFrame(Frame.newBuilder().setStylesheetId("id"))
          .addPietSharedStates(CONTENT_ID_1)
          .addPietSharedStates(CONTENT_ID_2)
          .build();
  private static final Content CONTENT_NO_OFFLINE_METADATA =
      Content.newBuilder()
          .setBasicLoggingMetadata(BASIC_LOGGING_METADATA)
          .setRepresentationData(REPRESENTATION_DATA)
          .setType(Type.PIET)
          .setExtension(PietContent.pietContentExtension, PIET_CONTENT)
          .build();
  private static final Content CONTENT =
      CONTENT_NO_OFFLINE_METADATA.toBuilder().setOfflineMetadata(OFFLINE_METADATA).build();

  private static final PietSharedState PIET_SHARED_STATE_1 =
      PietSharedState.newBuilder().addTemplates(Template.newBuilder().setTemplateId("1")).build();
  private static final PietSharedState PIET_SHARED_STATE_2 =
      PietSharedState.newBuilder().addTemplates(Template.newBuilder().setTemplateId("2")).build();
  private static final ImmutableList<PietSharedState> PIET_SHARED_STATES =
      ImmutableList.of(PIET_SHARED_STATE_1, PIET_SHARED_STATE_2);

  private static final StreamSharedState STREAM_SHARED_STATE_1 =
      StreamSharedState.newBuilder()
          .setPietSharedStateItem(
              PietSharedStateItem.newBuilder()
                  .setContentId(CONTENT_ID_1)
                  .setPietSharedState(PIET_SHARED_STATE_1))
          .build();
  private static final StreamSharedState STREAM_SHARED_STATE_2 =
      StreamSharedState.newBuilder()
          .setPietSharedStateItem(
              PietSharedStateItem.newBuilder()
                  .setContentId(CONTENT_ID_2)
                  .setPietSharedState(PIET_SHARED_STATE_2))
          .build();

  @Mock private ActionApi actionApi;
  @Mock private ActionManager actionManager;
  @Mock private ActionParserFactory actionParserFactory;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private ModelFeature modelFeature;
  @Mock private ModelProvider modelProvider;
  @Mock private PietViewHolder pietViewHolder;
  @Mock private StreamActionApi streamActionApi;
  @Mock private StreamOfflineMonitor streamOfflineMonitor;
  @Mock private ContentChangedListener contentChangedListener;
  @Mock private ActionParser actionParser;
  @Captor private ArgumentCaptor<Consumer<Boolean>> offlineConsumerCaptor;

  private Context context;
  private ContentDriver contentDriver;

  @Before
  public void setup() {
    initMocks(this);
    when(modelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setContent(CONTENT).build());
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(STREAM_SHARED_STATE_1);
    when(modelProvider.getSharedState(CONTENT_ID_2)).thenReturn(STREAM_SHARED_STATE_2);
    when(actionParserFactory.build(Mockito.<Supplier<ContentMetadata>>any()))
        .thenReturn(actionParser);
    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance(),
            streamOfflineMonitor,
            contentChangedListener) {
          @Override
          StreamActionApi createStreamActionApi(
              Context context,
              ActionApi actionApi,
              ActionParser actionParser,
              ActionManager actionManager,
              BasicLoggingApi basicLoggingApi,
              ContentLoggingData contentLoggingData,
              String sessionToken) {
            return streamActionApi;
          }
        };

    context = Robolectric.setupActivity(Activity.class);
  }

  @Test
  public void testBind() {
    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            eq(PIET_SHARED_STATES),
            eq(streamActionApi),
            eq(FeedActionPayload.getDefaultInstance()),
            any(LoggingListener.class),
            eq(actionParser));
  }

  @Test
  public void testBind_visibilityListenerLogsContentViewed() {
    contentDriver.bind(pietViewHolder);

    ArgumentCaptor<LoggingListener> visibilityListenerCaptor =
        ArgumentCaptor.forClass(LoggingListener.class);

    verify(pietViewHolder)
        .bind(
            any(),
            any(),
            any(),
            any(),
            visibilityListenerCaptor.capture(),
            any(ActionParser.class));
    LoggingListener listener = visibilityListenerCaptor.getValue();
    listener.onViewVisible();

    verify(basicLoggingApi).onContentViewed(eq(CONTENT_LOGGING_DATA));
  }

  @Test
  public void testBind_visibilityListenerLogsContentClicked() {
    contentDriver.bind(pietViewHolder);

    ArgumentCaptor<LoggingListener> visibilityListenerCaptor =
        ArgumentCaptor.forClass(LoggingListener.class);

    verify(pietViewHolder)
        .bind(
            any(),
            any(),
            any(),
            any(),
            visibilityListenerCaptor.capture(),
            any(ActionParser.class));
    LoggingListener listener = visibilityListenerCaptor.getValue();
    listener.onContentClicked();

    verify(basicLoggingApi).onContentClicked(eq(CONTENT_LOGGING_DATA));
  }

  @Test
  public void testBind_nullSharedStates() {
    reset(modelProvider);
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(null);

    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance(),
            streamOfflineMonitor,
            contentChangedListener);

    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            /* pietSharedStates= */ eq(new ArrayList<>()),
            any(StreamActionApi.class),
            eq(FeedActionPayload.getDefaultInstance()),
            any(LoggingListener.class),
            any(ActionParser.class));
    verify(modelProvider).getSharedState(CONTENT_ID_1);
  }

  @Test
  public void testBind_swipeableWithNonDefaultFeedActionPayload() {
    FeedActionPayload payload =
        FeedActionPayload.newBuilder()
            .setExtension(FeedAction.feedActionExtension, FeedAction.getDefaultInstance())
            .build();
    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            payload,
            streamOfflineMonitor,
            contentChangedListener);

    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            eq(PIET_SHARED_STATES),
            any(StreamActionApi.class),
            eq(payload),
            any(LoggingListener.class),
            any(ActionParser.class));
  }

  @Test
  public void testOfflineStatusChange() {
    reset(streamOfflineMonitor);

    when(streamOfflineMonitor.isAvailableOffline(CONTENT_URL)).thenReturn(false);
    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance(),
            streamOfflineMonitor,
            contentChangedListener);

    contentDriver.bind(pietViewHolder);

    verify(streamOfflineMonitor)
        .addOfflineStatusConsumer(eq(CONTENT_URL), offlineConsumerCaptor.capture());

    reset(pietViewHolder);

    offlineConsumerCaptor.getValue().accept(true);

    verify(contentChangedListener).onContentChanged();
    InOrder inOrder = Mockito.inOrder(pietViewHolder);
    inOrder.verify(pietViewHolder).unbind();
    inOrder
        .verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            eq(PIET_SHARED_STATES),
            any(StreamActionApi.class),
            eq(FeedActionPayload.getDefaultInstance()),
            any(LoggingListener.class),
            any(ActionParser.class));
  }

  @Test
  public void testOfflineStatusChange_noOpWithoutUpdate() {
    reset(streamOfflineMonitor);

    when(streamOfflineMonitor.isAvailableOffline(CONTENT_URL)).thenReturn(true);
    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance(),
            streamOfflineMonitor,
            contentChangedListener);

    contentDriver.bind(pietViewHolder);

    verify(streamOfflineMonitor)
        .addOfflineStatusConsumer(eq(CONTENT_URL), offlineConsumerCaptor.capture());

    reset(pietViewHolder);

    offlineConsumerCaptor.getValue().accept(true);

    verifyNoMoreInteractions(pietViewHolder, contentChangedListener);
  }

  @Test
  public void testNotEnoughInformationForContentMetadata() {
    assertThat(getContentMetadataFor(CONTENT_NO_OFFLINE_METADATA)).isNull();
  }

  @Test
  public void testContentMetadataProvider() {
    ContentMetadata contentMetadata = getContentMetadataFor(CONTENT);

    assertThat(contentMetadata).isNotNull();
    assertThat(contentMetadata.getTitle()).isEqualTo(CONTENT_TITLE);
    assertThat(contentMetadata.getUrl()).isEqualTo(CONTENT_URL);
  }

  @Test
  public void testUnbind() {
    contentDriver.bind(pietViewHolder);

    contentDriver.unbind();

    verify(pietViewHolder).unbind();
    assertThat(contentDriver.isBound()).isFalse();
  }

  /** For Captor of generic. */
  @SuppressWarnings("unchecked")
  /*@Nullable*/
  private ContentMetadata getContentMetadataFor(Content content) {
    when(modelFeature.getStreamFeature())
        .thenReturn(StreamFeature.newBuilder().setContent(content).build());

    reset(actionParserFactory);
    when(actionParserFactory.build(Mockito.<Supplier<ContentMetadata>>any()))
        .thenReturn(actionParser);

    contentDriver =
        new ContentDriver(
            actionApi,
            actionManager,
            actionParserFactory,
            basicLoggingApi,
            context,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance(),
            streamOfflineMonitor,
            contentChangedListener);

    ArgumentCaptor<Supplier<ContentMetadata>> contentMetadataSupplierCaptor =
        ArgumentCaptor.forClass((Class<Supplier<ContentMetadata>>) (Class) Supplier.class);

    verify(actionParserFactory).build(contentMetadataSupplierCaptor.capture());

    return contentMetadataSupplierCaptor.getValue().get();
  }
}
