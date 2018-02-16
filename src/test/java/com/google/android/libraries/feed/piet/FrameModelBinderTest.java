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

package com.google.android.libraries.feed.piet;

import static com.google.android.libraries.feed.piet.StyleProviderImpl.DEFAULT_STYLE;
import static com.google.android.libraries.feed.piet.StyleProviderImpl.DEFAULT_STYLE_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.GradientsProto.ColorStop;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.GradientsProto.LinearGradient;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.Stylesheet;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.Font;
import com.google.search.now.ui.piet.StylesProto.Font.FontWeight;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

/** Tests of the {@link FrameModelBinder}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
// TODO: Need to fully test FrameModelBinder.
public class FrameModelBinderTest {
  private static final String STYLESHEET_ID = "styleseheet-id";
  private static final String FRAME_STYLE_ID = "style-id";
  private static final int FRAME_COLOR = 12345;
  private static final Style FRAME_STYLE =
      Style.newBuilder().setStyleId(FRAME_STYLE_ID).setColor(FRAME_COLOR).build();
  private static final StyleIdsStack FRAME_STYLE_IDS =
      StyleIdsStack.newBuilder().addStyleIds(FRAME_STYLE_ID).build();

  @Mock private PietStylesHelper stylesHelper;
  @Mock private AssetProvider assetProvider;
  @Mock private CustomElementProvider customElementProvider;

  private FrameModelBinder frameModelBinder;

  @Before
  public void setUp() {
    initMocks(this);
    frameModelBinder = new FrameModelBinder(stylesHelper, assetProvider, customElementProvider);
  }

  @Test
  public void testError_noStylesheet() {
    Frame frame = Frame.getDefaultInstance();
    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);
    assertThat(boundFrameContext).isNotNull();
    assertThat(boundFrameContext.errors.size()).isEqualTo(1);
  }

  @Test
  public void testError_stylesheetNotFound() {
    Frame frame = getBaseFrame();
    when(stylesHelper.getStylesheet(STYLESHEET_ID)).thenReturn(null);
    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);
    assertThat(boundFrameContext).isNotNull();
    assertThat(boundFrameContext.errors.size()).isEqualTo(1);
  }

  @Test
  public void testError_styleNotFound() {
    Frame frame = getBaseFrame();
    when(stylesHelper.getStylesheet(STYLESHEET_ID)).thenReturn(new HashMap<>());
    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);
    assertThat(boundFrameContext).isNotNull();
    assertThat(boundFrameContext.errors.size()).isEqualTo(1);
  }

  @Test
  public void testCreateWithoutError() {
    Frame frame = getWorkingFrame();
    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);
    assertThat(boundFrameContext).isNotNull();
    assertThat(boundFrameContext.errors.size()).isEqualTo(0);
  }

  @Test
  public void testBindFrame_withStylesheetId() {
    Frame frame = Frame.newBuilder().setStylesheetId(STYLESHEET_ID).build();
    Map<String, Style> stylesheet = new HashMap<>();
    stylesheet.put(FRAME_STYLE_ID, FRAME_STYLE);
    when(stylesHelper.getStylesheet(STYLESHEET_ID)).thenReturn(stylesheet);

    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);

    // The style is not currently bound, but available from the stylesheet.
    assertThat(boundFrameContext.getCurrentStyle().getColor()).isNotEqualTo(FRAME_COLOR);
    assertThat(boundFrameContext.makeStyleFor(FRAME_STYLE_IDS).getColor()).isEqualTo(FRAME_COLOR);
  }

  @Test
  public void testBindFrame_withStylesheet() {
    Frame frame =
        Frame.newBuilder().setStylesheet(Stylesheet.newBuilder().addStyles(FRAME_STYLE)).build();

    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);

    // The style is not currently bound, but available from the stylesheet.
    assertThat(boundFrameContext.getCurrentStyle().getColor()).isNotEqualTo(FRAME_COLOR);
    assertThat(boundFrameContext.makeStyleFor(FRAME_STYLE_IDS).getColor()).isEqualTo(FRAME_COLOR);
  }

  @Test
  public void testBindFrame_withFrameStyle() {
    Frame frame =
        Frame.newBuilder()
            .setStylesheet(Stylesheet.newBuilder().addStyles(FRAME_STYLE))
            .setStyleReferences(FRAME_STYLE_IDS)
            .build();

    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);

    assertThat(boundFrameContext.getCurrentStyle().getColor()).isEqualTo(FRAME_COLOR);
  }

  @Test
  public void testBindFrame_withoutFrameStyle() {
    Frame frame =
        Frame.newBuilder().setStylesheet(Stylesheet.newBuilder().addStyles(FRAME_STYLE)).build();

    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);

    assertThat(boundFrameContext.getCurrentStyle()).isSameAs(DEFAULT_STYLE_PROVIDER);
  }

  @Test
  public void testBindFrame_baseStyle() {
    int styleHeight = 747;
    String heightStyleId = "JUMBO";
    Frame frame =
        Frame.newBuilder()
            .setStylesheet(
                Stylesheet.newBuilder()
                    .addStyles(FRAME_STYLE)
                    .addStyles(Style.newBuilder().setStyleId(heightStyleId).setHeight(styleHeight)))
            .setStyleReferences(FRAME_STYLE_IDS)
            .build();

    // Set up a frame with a color applied to the frame.
    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);
    StyleProvider baseStyleWithHeight =
        boundFrameContext.makeStyleFor(
            StyleIdsStack.newBuilder().addStyleIds(heightStyleId).build());

    // Make a style for something that doesn't override color, and check that the base color is a
    // default, not the frame color.
    assertThat(baseStyleWithHeight.getColor()).isEqualTo(DEFAULT_STYLE.getColor());
    assertThat(baseStyleWithHeight.getHeight()).isEqualTo(styleHeight);
  }

  @Test
  public void testMergeStyleIdsStack() {
    String styleId1 = "STYLE1";
    Style style1 =
        Style.newBuilder()
            .setColor(12345) // Not overridden
            .setMaxLines(54321) // Overridden
            .setFont(Font.newBuilder().setSize(11).setWeight(FontWeight.MEDIUM))
            .setBackground(
                Fill.newBuilder()
                    .setLinearGradient(
                        LinearGradient.newBuilder()
                            .addStops(ColorStop.newBuilder().setColor(1234))))
            .build();
    String styleId2 = "STYLE2";
    Style style2 =
        Style.newBuilder()
            .setMaxLines(22222) // Overrides
            .setMinHeight(33333) // Not an override
            .setFont(Font.newBuilder().setSize(13))
            .setBackground(
                Fill.newBuilder()
                    .setLinearGradient(LinearGradient.newBuilder().setDirectionDeg(321)))
            .build();
    Frame frame =
        Frame.newBuilder()
            .setStylesheetId(STYLESHEET_ID)
            .setStyleReferences(
                StyleIdsStack.newBuilder().addStyleIds(styleId1).addStyleIds(styleId2))
            .build();
    Map<String, Style> stylesheet = new HashMap<>();
    stylesheet.put(styleId1, style1);
    stylesheet.put(styleId2, style2);
    when(stylesHelper.getStylesheet(STYLESHEET_ID)).thenReturn(stylesheet);

    BoundFrameContext boundFrameContext = (BoundFrameContext) frameModelBinder.bindFrame(frame);

    assertThat(boundFrameContext.getCurrentStyle().getColor()).isEqualTo(12345);
    assertThat(boundFrameContext.getCurrentStyle().getMaxLines()).isEqualTo(22222);
    assertThat(boundFrameContext.getCurrentStyle().getMinHeight()).isEqualTo(33333);
    assertThat(boundFrameContext.getCurrentStyle().getFont())
        .isEqualTo(Font.newBuilder().setSize(13).setWeight(FontWeight.MEDIUM).build());
    assertThat(boundFrameContext.getCurrentStyle().getBackground())
        .isEqualTo(
            Fill.newBuilder()
                .setLinearGradient(
                    LinearGradient.newBuilder()
                        .addStops(ColorStop.newBuilder().setColor(1234))
                        .setDirectionDeg(321))
                .build());
    assertThat(boundFrameContext.errors.size()).isEqualTo(0);
  }

  @Test
  public void testError_bindingTemplate() {
    BoundFrameContext boundFrameContext =
        (BoundFrameContext) frameModelBinder.bindFrame(getWorkingFrame());
    Template template = Template.getDefaultInstance();
    BoundFrameContext boundCardContext2 =
        (BoundFrameContext) boundFrameContext.bindTemplate(template, null);
    assertThat(boundCardContext2).isEqualTo(boundFrameContext);
    assertThat(boundFrameContext.errors.size()).isEqualTo(1);
  }

  private Frame getWorkingFrame() {
    Map<String, Style> stylesheet = new HashMap<>();
    stylesheet.put(FRAME_STYLE_ID, FRAME_STYLE);
    when(stylesHelper.getStylesheet(STYLESHEET_ID)).thenReturn(stylesheet);
    return getBaseFrame();
  }

  private Frame getBaseFrame() {
    return Frame.newBuilder()
        .setStylesheetId(STYLESHEET_ID)
        .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds(FRAME_STYLE_ID))
        .build();
  }
}
