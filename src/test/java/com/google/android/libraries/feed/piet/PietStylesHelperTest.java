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

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.util.Producer;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.GradientsProto.ColorStop;
import com.google.search.now.ui.piet.GradientsProto.Fill;
import com.google.search.now.ui.piet.GradientsProto.LinearGradient;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Stylesheet;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.BoundStyle;
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

/** Tests of the {@link PietStylesHelper}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PietStylesHelperTest {

  private static final String STYLESHEET_ID = "ss1";
  private static final String STYLE_ID = "s1";
  private static final Style STYLE = Style.newBuilder().setStyleId(STYLE_ID).build();
  private static final String TEMPLATE_ID = "t1";
  private static final Template TEMPLATE = Template.newBuilder().setTemplateId(TEMPLATE_ID).build();

  private PietSharedState sharedState;

  @Mock Producer<PietSharedState> sharedStateProducer;
  @Mock FrameContext frameContext;

  private PietStylesHelper helper;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    Stylesheet.Builder stylesheet =
        Stylesheet.newBuilder().setStylesheetId(STYLESHEET_ID).addStyles(STYLE);

    sharedState =
        PietSharedState.newBuilder()
            .addStylesheets(stylesheet.build())
            .addTemplates(TEMPLATE)
            .build();

    when(sharedStateProducer.get()).thenReturn(sharedState);

    helper = new PietStylesHelper(sharedStateProducer);
  }

  @Test
  public void testGetStylesheet() {
    Map<String, Style> resultMap = helper.getStylesheet(STYLESHEET_ID);
    assertThat(resultMap).containsExactly(STYLE_ID, STYLE);

    // Retrieve map again to test caching
    assertThat(helper.getStylesheet(STYLESHEET_ID)).isSameAs(resultMap);
  }

  @Test
  public void testGetStylesheet_notExist() {
    assertThat(helper.getStylesheet("NOT EXIST")).isEqualTo(null);
  }

  @Test
  public void testCreateMapFromStylesheet() {
    Stylesheet.Builder myStyles = Stylesheet.newBuilder();
    Style sixties = Style.newBuilder().setStyleId("60s").build();
    myStyles.addStyles(sixties);
    Style eighties = Style.newBuilder().setStyleId("80s").build();
    myStyles.addStyles(eighties);

    assertThat(PietStylesHelper.createMapFromStylesheet(myStyles.build()))
        .containsExactly("60s", sixties, "80s", eighties);
  }

  @Test
  public void testGetTemplate() {
    assertThat(helper.getTemplate(TEMPLATE_ID)).isEqualTo(TEMPLATE);
  }

  @Test
  public void testGetTemplate_notExist() {
    assertThat(helper.getTemplate("NOT EXIST")).isNull();
  }

  @Test
  public void testNewPietSharedState() {
    // Set up the current state
    helper.getTemplate(TEMPLATE_ID);

    // Change to a new state
    PietSharedState.Builder sharedState2 = PietSharedState.newBuilder();

    Stylesheet.Builder stylesheet2 = Stylesheet.newBuilder().setStylesheetId("ss2");
    Style style2 = Style.newBuilder().setStyleId("s2").build();
    stylesheet2.addStyles(style2);
    sharedState2.addStylesheets(stylesheet2.build());

    Template template2 = Template.newBuilder().setTemplateId("t2").build();
    sharedState2.addTemplates(template2);

    when(sharedStateProducer.get()).thenReturn(sharedState2.build());

    // Then access the new data
    assertThat(helper.getTemplate("t2")).isEqualTo(template2);
    assertThat(helper.getStylesheet("ss2")).containsExactly("s2", style2);

    // And the old data is gone
    assertThat(helper.getTemplate(TEMPLATE_ID)).isNull();
    assertThat(helper.getStylesheet(STYLESHEET_ID)).isNull();
  }

  @Test
  public void testUpdatesProtos() {
    String templateId = "same";
    Template template = Template.newBuilder().setTemplateId(templateId).build();
    Template sameTemplate = Template.newBuilder().setTemplateId(templateId).build();
    assertThat(template).isEqualTo(sameTemplate);

    PietSharedState sharedState = PietSharedState.newBuilder().addTemplates(template).build();
    PietSharedState sameSharedState =
        PietSharedState.newBuilder().addTemplates(sameTemplate).build();
    assertThat(sharedState).isEqualTo(sameSharedState);

    reset(sharedStateProducer);
    when(sharedStateProducer.get()).thenReturn(sharedState).thenReturn(sameSharedState);

    // checkCurrent sets up templates from "sharedState" and returns "template"
    Template fetchTemplate = helper.getTemplate(templateId);
    assertThat(fetchTemplate).isSameAs(template);

    // checkCurrent updates templates from "sameSharedState" and returns "sameTemplate"
    Template fetchSameTemplate = helper.getTemplate(templateId);
    assertThat(fetchSameTemplate).isSameAs(sameTemplate);
  }

  @Test
  public void testMergeStyleIdsStack() {
    // These constants are in the final output and are not overridden
    int baseWidth = 999;
    int style1Color = 12345;
    int style2MaxLines = 22222;
    int style2MinHeight = 33333;
    FontWeight style1FontWeight = FontWeight.MEDIUM;
    int style2FontSize = 13;
    int style1GradientColor = 1234;
    int style2GradientDirection = 321;
    int boundStyleColor = 5050;
    Fill boundStyleBackground =
        Fill.newBuilder()
            .setLinearGradient(LinearGradient.newBuilder().setDirectionDeg(boundStyleColor))
            .build();

    Style baseStyle =
        Style.newBuilder()
            .setWidth(baseWidth) // Not overridden
            .setColor(888) // Overridden
            .build();
    String styleId1 = "STYLE1";
    Style style1 =
        Style.newBuilder()
            .setColor(style1Color) // Not overridden
            .setMaxLines(54321) // Overridden
            .setFont(Font.newBuilder().setSize(11).setWeight(style1FontWeight))
            .setBackground(
                Fill.newBuilder()
                    .setLinearGradient(
                        LinearGradient.newBuilder()
                            .addStops(ColorStop.newBuilder().setColor(style1GradientColor))))
            .build();
    String styleId2 = "STYLE2";
    Style style2 =
        Style.newBuilder()
            .setMaxLines(style2MaxLines) // Overrides
            .setMinHeight(style2MinHeight) // Not an override
            .setFont(Font.newBuilder().setSize(style2FontSize)) // Just override size
            .setBackground(
                Fill.newBuilder()
                    .setLinearGradient(
                        LinearGradient.newBuilder().setDirectionDeg(style2GradientDirection)))
            .build();
    Map<String, Style> stylesheetMap = new HashMap<>();
    stylesheetMap.put(styleId1, style1);
    stylesheetMap.put(styleId2, style2);

    // Test merging a style IDs stack
    Style mergedStyle =
        PietStylesHelper.mergeStyleIdsStack(
            baseStyle,
            StyleIdsStack.newBuilder().addStyleIds(styleId1).addStyleIds(styleId2).build(),
            stylesheetMap,
            frameContext,
            false);

    assertThat(mergedStyle)
        .isEqualTo(
            Style.newBuilder()
                .setColor(style1Color)
                .setMaxLines(style2MaxLines)
                .setMinHeight(style2MinHeight)
                .setFont(Font.newBuilder().setSize(style2FontSize).setWeight(style1FontWeight))
                .setBackground(
                    Fill.newBuilder()
                        .setLinearGradient(
                            LinearGradient.newBuilder()
                                .addStops(ColorStop.newBuilder().setColor(style1GradientColor))
                                .setDirectionDeg(style2GradientDirection)))
                .setWidth(baseWidth)
                .build());

    // Test merging a style IDs stack with a bound style
    String styleBindingId = "BOUND_STYLE";
    BoundStyle boundStyle =
        BoundStyle.newBuilder()
            .setColor(boundStyleColor)
            .setBackground(boundStyleBackground)
            .build();
    StyleBindingRef styleBindingRef =
        StyleBindingRef.newBuilder().setBindingId(styleBindingId).build();
    when(frameContext.getStyleFromBinding(styleBindingRef)).thenReturn(boundStyle);

    mergedStyle =
        PietStylesHelper.mergeStyleIdsStack(
            baseStyle,
            StyleIdsStack.newBuilder()
                .addStyleIds(styleId1)
                .addStyleIds(styleId2)
                .setStyleBinding(styleBindingRef)
                .build(),
            stylesheetMap,
            frameContext,
            false);
    assertThat(mergedStyle)
        .isEqualTo(
            Style.newBuilder()
                .setColor(boundStyleColor)
                .setMaxLines(style2MaxLines)
                .setMinHeight(style2MinHeight)
                .setFont(Font.newBuilder().setSize(style2FontSize).setWeight(style1FontWeight))
                .setBackground(
                    Fill.newBuilder()
                        .setLinearGradient(
                            LinearGradient.newBuilder()
                                .addStops(ColorStop.newBuilder().setColor(style1GradientColor))
                                .setDirectionDeg(boundStyleColor)))
                .setWidth(baseWidth)
                .build());

    // Binding styles fails when frameContext is null.
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                PietStylesHelper.mergeStyleIdsStack(
                    Style.getDefaultInstance(),
                    StyleIdsStack.newBuilder()
                        .addStyleIds(styleId1)
                        .addStyleIds(styleId2)
                        .setStyleBinding(styleBindingRef)
                        .build(),
                    stylesheetMap,
                    null,
                    false);
              }
            })
        .throwsAnExceptionOfType(NullPointerException.class)
        .that()
        .hasMessageThat()
        .contains("Binding styles not supported when frameContext is null");
  }
}
