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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.basicstream.internal.logging.LoggingListener;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamBasicLoggingApi;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamContentLoggingData;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder;
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
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContentDriver}. */
@RunWith(RobolectricTestRunner.class)
public class ContentDriverTest {

  private static final int POSITION = 1;
  private static final BasicLoggingMetadata BASIC_LOGGING_METADATA =
      BasicLoggingMetadata.newBuilder()
          .setPublishedTimeSeconds(10)
          .setAvailabilityTimeSeconds(20)
          .setScore(40)
          .build();
  private static final StreamContentLoggingData CONTENT_LOGGING_DATA =
      new StreamContentLoggingData(POSITION, BASIC_LOGGING_METADATA);

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
  private static final StreamFeature STREAM_FEATURE =
      StreamFeature.newBuilder()
          .setContent(
              Content.newBuilder()
                  .setBasicLoggingMetadata(BASIC_LOGGING_METADATA)
                  .setType(Type.PIET)
                  .setExtension(PietContent.pietContentExtension, PIET_CONTENT))
          .build();

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

  @Mock private StreamBasicLoggingApi basicLoggingApi;
  @Mock private ModelFeature modelFeature;
  @Mock private ModelProvider modelProvider;
  @Mock private PietViewHolder pietViewHolder;

  private ContentDriver contentDriver;

  @Before
  public void setup() {
    initMocks(this);
    when(modelFeature.getStreamFeature()).thenReturn(STREAM_FEATURE);
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(STREAM_SHARED_STATE_1);
    when(modelProvider.getSharedState(CONTENT_ID_2)).thenReturn(STREAM_SHARED_STATE_2);
  }

  @Test
  public void testBind() {
    contentDriver =
        new ContentDriver(
            basicLoggingApi,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance());

    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            eq(PIET_SHARED_STATES),
            eq(FeedActionPayload.getDefaultInstance()),
            any(LoggingListener.class));
  }

  @Test
  public void testBind_visibilityListenerLogsContentViewed() {
    contentDriver =
        new ContentDriver(
            basicLoggingApi,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance());

    contentDriver.bind(pietViewHolder);

    ArgumentCaptor<LoggingListener> visibilityListenerCaptor =
        ArgumentCaptor.forClass(LoggingListener.class);

    verify(pietViewHolder).bind(any(), any(), any(), visibilityListenerCaptor.capture());
    LoggingListener listener = visibilityListenerCaptor.getValue();
    listener.onViewVisible();

    verify(basicLoggingApi).onContentViewed(eq(CONTENT_LOGGING_DATA));
  }

  @Test
  public void testBind_nullSharedStates() {
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(null);

    contentDriver =
        new ContentDriver(
            basicLoggingApi,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance());

    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            /* pietSharedStates= */ eq(new ArrayList<>()),
            eq(FeedActionPayload.getDefaultInstance()),
            any(LoggingListener.class));
    verify(modelProvider).getSharedState(CONTENT_ID_1);
    verifyNoMoreInteractions(modelProvider);
  }

  @Test
  public void testBind_swipeableWithNonDefaultFeedActionPayload() {
    FeedActionPayload payload =
        FeedActionPayload.newBuilder()
            .setExtension(FeedAction.feedActionExtension, FeedAction.getDefaultInstance())
            .build();
    contentDriver =
        new ContentDriver(basicLoggingApi, modelFeature, modelProvider, POSITION, payload);

    contentDriver.bind(pietViewHolder);

    verify(pietViewHolder)
        .bind(
            eq(PIET_CONTENT.getFrame()),
            eq(PIET_SHARED_STATES),
            eq(payload),
            any(LoggingListener.class));
  }

  @Test
  public void testUnbind() {
    contentDriver =
        new ContentDriver(
            basicLoggingApi,
            modelFeature,
            modelProvider,
            POSITION,
            FeedActionPayload.getDefaultInstance());

    contentDriver.bind(pietViewHolder);

    contentDriver.unbind();

    verify(pietViewHolder).unbind();
    assertThat(contentDriver.isBound()).isFalse();
  }
}
