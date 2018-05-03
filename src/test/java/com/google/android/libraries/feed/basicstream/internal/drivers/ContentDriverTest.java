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

import static com.google.android.libraries.feed.basicstream.internal.drivers.ContentDriver.GLOBAL_SHARED_STATE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.PietContentModel;
import com.google.common.collect.ImmutableList;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import com.google.search.now.ui.stream.StreamStructureProto.Content.Type;
import com.google.search.now.ui.stream.StreamStructureProto.PietContent;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.PietSharedStateItemProto.PietSharedStateItem;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContentDriver}. */
@RunWith(RobolectricTestRunner.class)
public class ContentDriverTest {

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
                  .setType(Type.PIET)
                  .setExtension(PietContent.pietContentExtension, PIET_CONTENT))
          .build();

  private static final PietSharedState PIET_SHARED_STATE_1 =
      PietSharedState.newBuilder().addTemplates(Template.newBuilder().setTemplateId("1")).build();
  private static final PietSharedState PIET_SHARED_STATE_2 =
      PietSharedState.newBuilder().addTemplates(Template.newBuilder().setTemplateId("2")).build();
  private static final List<PietSharedState> PIET_SHARED_STATES =
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

  @Mock private ModelFeature modelFeature;
  @Mock private ModelProvider modelProvider;

  private ContentDriver contentDriver;

  @Before
  public void setup() {
    initMocks(this);
    when(modelFeature.getStreamFeature()).thenReturn(STREAM_FEATURE);
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(STREAM_SHARED_STATE_1);
    when(modelProvider.getSharedState(CONTENT_ID_2)).thenReturn(STREAM_SHARED_STATE_2);
    contentDriver = new ContentDriver(modelFeature, modelProvider);
  }

  @Test
  public void testGetContentModel_pietContentSetCorrectly() {
    ContentModel contentModel = contentDriver.getContentModel();

    assertThat(contentModel).isInstanceOf(PietContentModel.class);

    assertThat(((PietContentModel) contentModel).getPietContent()).isEqualTo(PIET_CONTENT);
  }

  @Test
  public void testGetContentModel_sharedStatesSetCorrectly() {
    ContentModel contentModel = contentDriver.getContentModel();

    assertThat(contentModel).isInstanceOf(PietContentModel.class);

    assertThat(((PietContentModel) contentModel).getPietSharedStates())
        .isEqualTo(PIET_SHARED_STATES);
    verify(modelProvider).getSharedState(CONTENT_ID_1);
    verify(modelProvider).getSharedState(CONTENT_ID_2);
  }

  @Test
  public void testGetContentModel_noContentIds() {
    PietContent pietContent =
        PietContent.newBuilder().setFrame(Frame.newBuilder().setStylesheetId("id")).build();
    StreamFeature streamFeature =
        StreamFeature.newBuilder()
            .setContent(
                Content.newBuilder()
                    .setType(Type.PIET)
                    .setExtension(PietContent.pietContentExtension, pietContent))
            .build();
    PietSharedState pietSharedState =
        PietSharedState.newBuilder().addTemplates(Template.newBuilder().setTemplateId("3")).build();
    StreamSharedState streamSharedState =
        StreamSharedState.newBuilder()
            .setPietSharedStateItem(
                PietSharedStateItem.newBuilder()
                    .setContentId(GLOBAL_SHARED_STATE)
                    .setPietSharedState(pietSharedState))
            .build();

    when(modelFeature.getStreamFeature()).thenReturn(streamFeature);
    when(modelProvider.getSharedState(GLOBAL_SHARED_STATE)).thenReturn(streamSharedState);

    ContentModel contentModel = contentDriver.getContentModel();

    assertThat(contentModel).isInstanceOf(PietContentModel.class);

    assertThat(((PietContentModel) contentModel).getPietContent()).isEqualTo(pietContent);
    assertThat(((PietContentModel) contentModel).getPietSharedStates())
        .isEqualTo(ImmutableList.of(pietSharedState));
    verify(modelProvider).getSharedState(GLOBAL_SHARED_STATE);
  }

  @Test
  public void testGetContentModel_sharedStateItemIsNull() {
    when(modelProvider.getSharedState(CONTENT_ID_1)).thenReturn(null);
    ContentModel contentModel = contentDriver.getContentModel();

    assertThat(contentModel).isNull();
    verify(modelProvider).getSharedState(CONTENT_ID_1);
    verifyNoMoreInteractions(modelProvider);
  }

  @Test
  public void testGetContentModel_reusesPreviousContentModel() {
    ContentModel contentModel1 = contentDriver.getContentModel();

    verify(modelFeature).getStreamFeature();

    ContentModel contentModel2 = contentDriver.getContentModel();

    verifyNoMoreInteractions(modelFeature);

    assertThat(contentModel1).isSameAs(contentModel2);
  }
}
