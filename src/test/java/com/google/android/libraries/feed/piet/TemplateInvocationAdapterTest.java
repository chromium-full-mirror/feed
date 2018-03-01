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
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.Producers;
import com.google.search.now.ui.piet.BindingRefsProto.ParameterizedTextBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.Stylesheet;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link TemplateInvocationAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class TemplateInvocationAdapterTest {

  private static final int FRAME_COLOR = 12345;
  private static final int TEMPLATE_COLOR = 54321;
  private static final String FRAME_STYLESHEET_ID = "coolcat";
  private static final String TEXT_STYLE_ID = "catalog";
  private static final String TEMPLATE_ID = "duplicat";
  private static final String TEXT_BINDING_ID = "ofmiceandmen";
  private static final String TEXT_CONTENTS = "afewmilessouth";
  private static final String TEXT_CONTENTS_2 = "gangaftagley";

  private static final Stylesheet TEMPLATE_STYLESHEET =
      Stylesheet.newBuilder()
          .addStyles(Style.newBuilder().setStyleId(TEXT_STYLE_ID).setColor(TEMPLATE_COLOR))
          .build();
  private static final Template DEFAULT_TEMPLATE =
      Template.newBuilder()
          .setTemplateId(TEMPLATE_ID)
          .setElementList(
              ElementList.newBuilder()
                  .addElements(
                      Element.newBuilder()
                          .setTextElement(
                              TextElement.newBuilder()
                                  .setStyleReferences(
                                      StyleIdsStack.newBuilder().addStyleIds(TEXT_STYLE_ID))
                                  .setParameterizedTextBinding(
                                      ParameterizedTextBindingRef.newBuilder()
                                          .setBindingId(TEXT_BINDING_ID)))))
          .build();

  private static final Frame DEFAULT_FRAME =
      Frame.newBuilder()
          .setStylesheet(
              Stylesheet.newBuilder()
                  .setStylesheetId(FRAME_STYLESHEET_ID)
                  .addStyles(Style.newBuilder().setStyleId(TEXT_STYLE_ID).setColor(FRAME_COLOR)))
          .addTemplates(DEFAULT_TEMPLATE)
          .build();

  @Mock private PietStylesHelper stylesHelper;
  @Mock private AssetProvider assetProvider;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private ActionHandler actionHandler;

  private Context context;
  private AdapterParameters adapterParameters;
  private FrameContext frameContext;

  private TemplateInvocationAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    frameContext =
        new FrameModelBinder(stylesHelper, assetProvider, customElementProvider)
            .bindFrame(DEFAULT_FRAME);
    adapterParameters = new AdapterParameters(context, Producers.of(null), actionHandler);

    adapter = new TemplateInvocationAdapter(context, adapterParameters);
  }

  @Test
  public void testOnCreateAdapter_templateNotFound() {
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder()
            .setTemplateId("NOT_A_REAL_ID")
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS))))
            .build();

    adapter.createAdapter(templateInvocation, frameContext);

    // No children have been added because the template was not found.
    LinearLayout templateView = adapter.getBaseView();
    assertThat(templateView.getChildCount()).isEqualTo(0);
    // TODO: make this a more visible error on dev/alpha builds.
  }

  @Test
  public void testOnCreateAdapter_noBindingContexts() {
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder().setTemplateId(TEMPLATE_ID).build();

    adapter.createAdapter(templateInvocation, frameContext);

    // No children have been added because there were no binding contexts.
    LinearLayout templateView = adapter.getBaseView();
    assertThat(templateView.getChildCount()).isEqualTo(0);
  }

  @Test
  public void testOnBindModel_singleInstanceWithFrameStyle() {
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder()
            .setTemplateId(TEMPLATE_ID)
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS))))
            .build();

    adapter.createAdapter(templateInvocation, frameContext);

    // Assert that all child views have been created
    LinearLayout templateView = adapter.getBaseView();
    assertThat(templateView.getChildCount()).isEqualTo(1);

    LinearLayout elementListView = (LinearLayout) templateView.getChildAt(0);

    // Check that default layout params are set.
    assertThat(elementListView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(elementListView.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);

    // Check for child element
    assertThat(elementListView.getChildCount()).isEqualTo(1);
    TextView textElementView = (TextView) elementListView.getChildAt(0);

    // Text style comes from the Frame stylesheet.
    assertThat(textElementView.getTextColors().getDefaultColor()).isEqualTo(FRAME_COLOR);

    // No text before binding
    assertThat(textElementView.getText().toString()).isEmpty();

    // Binding sets text.
    adapter.bindModel(templateInvocation, frameContext);
    assertThat(textElementView.getText().toString()).isEqualTo(TEXT_CONTENTS);
  }

  @Test
  public void testOnBindModel_singleInstanceWithTemplateStyle() {
    Template defaultTemplateWithLocalStylesheet =
        DEFAULT_TEMPLATE.toBuilder().setStylesheet(TEMPLATE_STYLESHEET).build();
    frameContext =
        new FrameModelBinder(stylesHelper, assetProvider, customElementProvider)
            .bindFrame(
                DEFAULT_FRAME
                    .toBuilder()
                    .clearTemplates()
                    .addTemplates(defaultTemplateWithLocalStylesheet)
                    .build());

    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder()
            .setTemplateId(TEMPLATE_ID)
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS))))
            .build();

    adapter.createAdapter(templateInvocation, frameContext);

    // Assert that all child views have been created and that text is set.
    LinearLayout templateView = adapter.getBaseView();
    LinearLayout elementListView = (LinearLayout) templateView.getChildAt(0);
    TextView textElementView = (TextView) elementListView.getChildAt(0);

    // Text style comes from the Template stylesheet.
    assertThat(textElementView.getTextColors().getDefaultColor()).isEqualTo(TEMPLATE_COLOR);

    // Text is not populated yet
    assertThat(textElementView.getText().toString()).isEmpty();

    // Text is populated after binding.
    adapter.bindModel(templateInvocation, frameContext);
    assertThat(textElementView.getText().toString()).isEqualTo(TEXT_CONTENTS);
  }

  @Test
  public void testOnBindModel_multipleInstances() {
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder()
            .setTemplateId(TEMPLATE_ID)
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS))))
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS_2))))
            .build();

    adapter.createAdapter(templateInvocation, frameContext);

    // Assert that all child views have been created and that text is set.
    LinearLayout templateView = adapter.getBaseView();

    assertThat(templateView.getChildCount()).isEqualTo(2);
    assertThat(((LinearLayout) templateView.getChildAt(0)).getChildCount()).isEqualTo(1);
    assertThat(((LinearLayout) templateView.getChildAt(1)).getChildCount()).isEqualTo(1);

    // Check that text is not yet set.
    assertThat(
            ((TextView) ((LinearLayout) templateView.getChildAt(0)).getChildAt(0))
                .getText()
                .toString())
        .isEmpty();
    assertThat(
            ((TextView) ((LinearLayout) templateView.getChildAt(1)).getChildAt(0))
                .getText()
                .toString())
        .isEmpty();

    // Bind and check child models.
    adapter.bindModel(templateInvocation, frameContext);

    // First instance has TEXT_CONTENTS
    LinearLayout elementListView = (LinearLayout) templateView.getChildAt(0);
    TextView textElementView = (TextView) elementListView.getChildAt(0);
    assertThat(textElementView.getText().toString()).isEqualTo(TEXT_CONTENTS);

    // Second instance has TEXT_CONTENTS_2
    elementListView = (LinearLayout) templateView.getChildAt(1);
    textElementView = (TextView) elementListView.getChildAt(0);
    assertThat(textElementView.getText().toString()).isEqualTo(TEXT_CONTENTS_2);
  }

  @Test
  public void testReleaseAdapter() {
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder()
            .setTemplateId(TEMPLATE_ID)
            .addBindingContexts(
                BindingContext.newBuilder()
                    .addBindingValues(
                        BindingValue.newBuilder()
                            .setBindingId(TEXT_BINDING_ID)
                            .setParameterizedText(
                                ParameterizedText.newBuilder().setText(TEXT_CONTENTS))))
            .build();

    adapter.createAdapter(templateInvocation, frameContext);
    adapter.bindModel(templateInvocation, frameContext);
    LinearLayout templateView = adapter.getBaseView();
    assertThat(templateView.getChildCount()).isEqualTo(1);
    LinearLayout elementListView = (LinearLayout) templateView.getChildAt(0);
    assertThat(((TextView) elementListView.getChildAt(0)).getText().toString())
        .isEqualTo(TEXT_CONTENTS);

    adapter.releaseAdapter();

    assertThat(templateView.getChildCount()).isEqualTo(0);
    assertThat(elementListView.getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testCreateViewGroup() {
    LinearLayout viewGroup = adapter.createViewGroup();

    assertThat(viewGroup.getOrientation()).isEqualTo(LinearLayout.VERTICAL);
    assertThat(viewGroup.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(viewGroup.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testGetModelFromElement() {
    Element elementWithModel =
        Element.newBuilder().setTemplateBinding(TemplateBindingRef.getDefaultInstance()).build();
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.getModelFromElement(elementWithModel);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Templates must be re-bound");

    Element elementWithWrongModel =
        Element.newBuilder().setCustomElement(CustomElement.getDefaultInstance()).build();
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.getModelFromElement(elementWithWrongModel);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Templates must be re-bound");

    Element emptyElement = Element.getDefaultInstance();
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.getModelFromElement(emptyElement);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Templates must be re-bound");
  }
}
