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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.piet.TemplateInstanceAdapter.TemplateAdapterModel;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link ElementContainerAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class ElementContainerAdapterTest {

  private static final Element DEFAULT_ELEMENT =
      Element.newBuilder().setElementList(ElementList.newBuilder().build()).build();

  private static final Element DEFAULT_BOUND_ELEMENT =
      Element.newBuilder().setGridRow(GridRow.getDefaultInstance()).build();
  private static final String DEFAULT_ELEMENT_BINDING_ID = "bound-grid-element";
  private static final ElementBindingRef DEFAULT_ELEMENT_BINDING =
      ElementBindingRef.newBuilder().setBindingId(DEFAULT_ELEMENT_BINDING_ID).build();
  private static final BindingValue DEFAULT_ELEMENT_BINDING_VALUE =
      BindingValue.newBuilder()
          .setBindingId(DEFAULT_ELEMENT_BINDING_ID)
          .setElement(DEFAULT_BOUND_ELEMENT)
          .build();

  private static final String DEFAULT_TEMPLATE_ID = "inline-text-template";
  private static final Template DEFAULT_TEMPLATE =
      Template.newBuilder()
          .setTemplateId(DEFAULT_TEMPLATE_ID)
          .setElement(
              Element.newBuilder()
                  .setTextElement(
                      TextElement.newBuilder()
                          .setParameterizedText(ParameterizedText.getDefaultInstance())))
          .build();
  private static final TemplateInvocation DEFAULT_TEMPLATE_INVOCATION =
      TemplateInvocation.newBuilder()
          .setTemplateId(DEFAULT_TEMPLATE_ID)
          .addBindingContexts(
              BindingContext.newBuilder()
                  .addBindingValues(BindingValue.newBuilder().setBindingId("T1")))
          .addBindingContexts(
              BindingContext.newBuilder()
                  .addBindingValues(BindingValue.newBuilder().setBindingId("T2")))
          .build();

  private static final String DEFAULT_BOUND_TEMPLATE_ID = "bound-image-template";
  private static final Template DEFAULT_BOUND_TEMPLATE =
      Template.newBuilder()
          .setTemplateId(DEFAULT_BOUND_TEMPLATE_ID)
          .setElement(
              Element.newBuilder()
                  .setImageElement(ImageElement.newBuilder().setImage(Image.getDefaultInstance())))
          .build();
  private static final TemplateInvocation DEFAULT_BOUND_TEMPLATE_INVOCATION =
      TemplateInvocation.newBuilder()
          .setTemplateId(DEFAULT_BOUND_TEMPLATE_ID)
          .addBindingContexts(
              BindingContext.newBuilder()
                  .addBindingValues(BindingValue.newBuilder().setBindingId("BT1")))
          .addBindingContexts(
              BindingContext.newBuilder()
                  .addBindingValues(BindingValue.newBuilder().setBindingId("BT2")))
          .addBindingContexts(
              BindingContext.newBuilder()
                  .addBindingValues(BindingValue.newBuilder().setBindingId("BT3")))
          .build();
  private static final String DEFAULT_TEMPLATE_BINDING_ID = "bind-the-bound-image-template";
  private static final TemplateBindingRef DEFAULT_TEMPLATE_BINDING =
      TemplateBindingRef.newBuilder().setBindingId(DEFAULT_TEMPLATE_BINDING_ID).build();
  private static final BindingValue TEMPLATE_BINDING_VALUE =
      BindingValue.newBuilder()
          .setBindingId(DEFAULT_TEMPLATE_BINDING_ID)
          .setTemplateInvocation(DEFAULT_BOUND_TEMPLATE_INVOCATION)
          .build();

  @Mock private FrameContext frameContext;
  @Mock private AssetProvider assetProvider;

  private Context context;
  private AdapterParameters adapterParameters;
  private LinearLayout view;

  private TestElementContainerAdapter adapter;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);

    when(frameContext.getCurrentStyle()).thenReturn(StyleProvider.DEFAULT_STYLE_PROVIDER);
    when(frameContext.bindNewStyle(any())).thenReturn(frameContext);
    when(frameContext.bindTemplate(eq(DEFAULT_TEMPLATE), any(BindingContext.class)))
        .thenReturn(frameContext);
    when(frameContext.bindTemplate(eq(DEFAULT_BOUND_TEMPLATE), any(BindingContext.class)))
        .thenReturn(frameContext);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class)))
        .thenReturn(StyleProvider.DEFAULT_STYLE_PROVIDER);
    when(frameContext.getAssetProvider()).thenReturn(assetProvider);

    // Default no-errors behavior
    when(frameContext.getTemplateInvocationBindingValue(DEFAULT_TEMPLATE_BINDING))
        .thenReturn(TEMPLATE_BINDING_VALUE);
    when(frameContext.getTemplate(DEFAULT_TEMPLATE_ID)).thenReturn(DEFAULT_TEMPLATE);
    when(frameContext.getTemplate(DEFAULT_BOUND_TEMPLATE_ID)).thenReturn(DEFAULT_BOUND_TEMPLATE);
    when(frameContext.getElementBindingValue(DEFAULT_ELEMENT_BINDING))
        .thenReturn(DEFAULT_ELEMENT_BINDING_VALUE);

    view = new LinearLayout(context);
    adapterParameters = new AdapterParameters(context, Suppliers.of(null));
    adapter = new TestElementContainerAdapter(context, adapterParameters, view);
  }

  @Test
  public void testCreateChildAdapters_elementCreatesOneAdapter() {
    adapter.createInlineChildAdapters(
        Collections.singletonList(Content.newBuilder().setElement(DEFAULT_ELEMENT).build()),
        frameContext);

    // A single adapter created from the Element.
    assertThat(adapter.childAdapters).hasSize(1);
    assertThat(adapter.childAdapters.get(0)).isInstanceOf(ElementListAdapter.class);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {1});
  }

  @Test
  public void testCreateChildAdapters_templateCreatesMultipleAdapters() {
    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build()),
        frameContext);

    // One TemplateInstanceAdapter for each BindingContext in the TemplateInvocation.
    assertThat(DEFAULT_TEMPLATE_INVOCATION.getBindingContextsCount()).isEqualTo(2);
    assertThat(adapter.childAdapters).hasSize(2);
    assertThat(adapter.childAdapters.get(0)).isInstanceOf(TemplateInstanceAdapter.class);
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(0)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(0));
    assertThat(adapter.childAdapters.get(1)).isInstanceOf(TemplateInstanceAdapter.class);
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(1)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(1));

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(2);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.getBaseView().getChildAt(1))
        .isSameAs(adapter.childAdapters.get(1).getView());
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2});
  }

  @Test
  public void testCreateChildAdapters_templateNotFound() {
    when(frameContext.getTemplate(DEFAULT_TEMPLATE_ID)).thenReturn(null);

    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build()),
        frameContext);

    // No child adapters are created.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testCreateChildAdapters_boundElement_doesNothing() {
    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setBoundElement(DEFAULT_ELEMENT_BINDING).build()),
        frameContext);

    // No child adapters are created.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testCreateChildAdapters_boundElementNotFound_doesNothing() {
    when(frameContext.getElementBindingValue(DEFAULT_ELEMENT_BINDING))
        .thenReturn(BindingValue.getDefaultInstance());

    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setBoundElement(DEFAULT_ELEMENT_BINDING).build()),
        frameContext);

    // No child adapters are created.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testCreateChildAdapters_boundTemplate_doesNothing() {
    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build()),
        frameContext);

    // No child adapters are created.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testCreateChildAdapters_boundTemplateNotFound_doesNothing() {
    when(frameContext.getTemplateInvocationBindingValue(DEFAULT_TEMPLATE_BINDING))
        .thenReturn(BindingValue.getDefaultInstance());

    adapter.createInlineChildAdapters(
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build()),
        frameContext);

    // No child adapters are created.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testCreateChildAdapters_boundTemplateBetweenInlineContents() {
    List<Content> contents = new ArrayList<>();
    contents.add(Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    contents.add(Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    contents.add(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createInlineChildAdapters(contents, frameContext);

    // Adapters created:  2 templates, (nothing for the binding), 1 element list
    assertThat(adapter.childAdapters).hasSize(3);
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(0)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(0));
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(1)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(1));

    assertThat(adapter.childAdapters.get(2)).isInstanceOf(ElementListAdapter.class);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      assertThat(adapter.getBaseView().getChildAt(i))
          .isSameAs(adapter.childAdapters.get(i).getView());
    }

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2, 0, 1});
  }

  @Test
  public void testCreateChildAdapters_failsIfAdapterNotReleased() {
    adapter.createInlineChildAdapters(
        Collections.singletonList(Content.newBuilder().setElement(DEFAULT_ELEMENT).build()),
        frameContext);

    assertThatRunnable(
            () ->
                adapter.createInlineChildAdapters(
                    Collections.singletonList(
                        Content.newBuilder().setElement(DEFAULT_ELEMENT).build()),
                    frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("release adapter before creating");
  }

  @Test
  public void testBindChildAdapters_errorIfNotCreatedFirst() {
    assertThatRunnable(
            () ->
                adapter.bindChildAdapters(
                    Collections.singletonList(
                        Content.newBuilder().setElement(DEFAULT_ELEMENT).build()),
                    frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Adapter has not been created?");
  }

  @Test
  public void testBindChildAdapters_element() {
    List<Content> contents =
        Collections.singletonList(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());
    adapter.createInlineChildAdapters(contents, frameContext);
    ElementAdapter<? extends View, ?> mockChildAdapter = mock(ElementListAdapter.class);
    adapter.childAdapters.set(0, mockChildAdapter);

    adapter.bindChildAdapters(contents, frameContext);

    verify(mockChildAdapter).bindModel(DEFAULT_ELEMENT, frameContext);
  }

  @Test
  public void testBindChildAdapters_template() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    adapter.createInlineChildAdapters(contents, frameContext);
    TemplateInstanceAdapter mockChildAdapter1 = mock(TemplateInstanceAdapter.class);
    TemplateInstanceAdapter mockChildAdapter2 = mock(TemplateInstanceAdapter.class);
    adapter.childAdapters.set(0, mockChildAdapter1);
    adapter.childAdapters.set(1, mockChildAdapter2);

    adapter.bindChildAdapters(contents, frameContext);

    verify(mockChildAdapter1)
        .bindModel(
            new TemplateAdapterModel(
                DEFAULT_TEMPLATE, DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(0)),
            frameContext);
    verify(mockChildAdapter2)
        .bindModel(
            new TemplateAdapterModel(
                DEFAULT_TEMPLATE, DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(1)),
            frameContext);
  }

  @Test
  public void testBindChildAdapters_templateNotFound() {
    when(frameContext.getTemplate(DEFAULT_TEMPLATE_ID)).thenReturn(null);

    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    adapter.createInlineChildAdapters(contents, frameContext);

    adapter.bindChildAdapters(contents, frameContext);

    // No child adapters are created; binding does nothing.
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testBindChildAdapters_withExtraViews() {
    List<Content> contents =
        Collections.singletonList(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());
    View paddingViewL = new View(context);
    View paddingViewR = new View(context);
    adapter.getBaseView().addView(paddingViewL);
    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.getBaseView().addView(paddingViewR);

    adapter.setFirstAdapterViewOffset(1);
    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    assertThat(adapter.getBaseView().getChildAt(0)).isSameAs(paddingViewL);
    assertThat(adapter.getBaseView().getChildAt(1))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.getBaseView().getChildAt(2)).isSameAs(paddingViewR);

    // Assert the child has been bound.
    assertThat(adapter.childAdapters.get(0).getModel()).isSameAs(DEFAULT_ELEMENT.getElementList());

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {1});
  }

  @Test
  public void testBindChildAdapters_boundElement() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setBoundElement(DEFAULT_ELEMENT_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);

    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {1});
  }

  @Test
  public void testBindChildAdapters_boundTemplate() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);

    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.getBaseView().getChildAt(1))
        .isSameAs(adapter.childAdapters.get(1).getView());
    assertThat(adapter.getBaseView().getChildAt(2))
        .isSameAs(adapter.childAdapters.get(2).getView());

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {3});
  }

  @Test
  public void testBindChildAdapters_boundTemplate_withExtraViews() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    View paddingViewL = new View(context);
    View paddingViewR = new View(context);
    adapter.getBaseView().addView(paddingViewL);
    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.getBaseView().addView(paddingViewR);

    adapter.setFirstAdapterViewOffset(1);
    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(5);
    assertThat(adapter.getBaseView().getChildAt(0)).isSameAs(paddingViewL);
    assertThat(adapter.getBaseView().getChildAt(1))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.getBaseView().getChildAt(2))
        .isSameAs(adapter.childAdapters.get(1).getView());
    assertThat(adapter.getBaseView().getChildAt(3))
        .isSameAs(adapter.childAdapters.get(2).getView());
    assertThat(adapter.getBaseView().getChildAt(4)).isSameAs(paddingViewR);

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {3});
  }

  @Test
  public void testBindChildAdapters_boundTemplateNotFound_doesNothing() {
    when(frameContext.getTemplate(DEFAULT_BOUND_TEMPLATE_ID)).thenReturn(null);

    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);

    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testBindChildAdapters_boundTemplateBindingNotFound_doesNothing() {
    when(frameContext.getTemplateInvocationBindingValue(DEFAULT_TEMPLATE_BINDING))
        .thenReturn(BindingValue.getDefaultInstance());

    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);

    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testBindChildAdapters_boundTemplateBetweenInlineContents() {
    List<Content> contents = new ArrayList<>();
    contents.add(Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    contents.add(Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    contents.add(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    // 2 templates, 3 templates, 1 element list
    assertThat(adapter.childAdapters).hasSize(6);
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(0)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(0));
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(1)).getModel().getBindingContext())
        .isSameAs(DEFAULT_TEMPLATE_INVOCATION.getBindingContexts(1));

    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(2)).getModel().getBindingContext())
        .isSameAs(DEFAULT_BOUND_TEMPLATE_INVOCATION.getBindingContexts(0));
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(3)).getModel().getBindingContext())
        .isSameAs(DEFAULT_BOUND_TEMPLATE_INVOCATION.getBindingContexts(1));
    assertThat(
            ((TemplateInstanceAdapter) adapter.childAdapters.get(4)).getModel().getBindingContext())
        .isSameAs(DEFAULT_BOUND_TEMPLATE_INVOCATION.getBindingContexts(2));

    assertThat(adapter.childAdapters.get(5)).isInstanceOf(ElementListAdapter.class);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(6);
    for (int i = 0; i < 6; i++) {
      assertThat(adapter.getBaseView().getChildAt(i))
          .isSameAs(adapter.childAdapters.get(i).getView());
    }

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2, 3, 1});
  }

  @Test
  public void testUnbindChildAdapters_inline() {
    List<Content> contents = new ArrayList<>();
    contents.add(Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    contents.add(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    adapter.unbindChildAdapters(contents);

    // 2 templates, 3 templates, 1 element list
    assertThat(adapter.childAdapters).hasSize(3);
    assertThat(adapter.childAdapters.get(0)).isInstanceOf(TemplateInstanceAdapter.class);
    assertThat(adapter.childAdapters.get(1)).isInstanceOf(TemplateInstanceAdapter.class);
    assertThat(adapter.childAdapters.get(2)).isInstanceOf(ElementListAdapter.class);

    assertThat(adapter.childAdapters.get(0).getRawModel()).isNull();
    assertThat(adapter.childAdapters.get(1).getRawModel()).isNull();
    assertThat(adapter.childAdapters.get(2).getRawModel()).isNull();

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      assertThat(adapter.getBaseView().getChildAt(i))
          .isSameAs(adapter.childAdapters.get(i).getView());
    }

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2, 1});
  }

  @Test
  public void testUnbindChildAdapters_boundElement() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setBoundElement(DEFAULT_ELEMENT_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    adapter.unbindChildAdapters(contents);

    // All (bound) adapters and views have been destroyed
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testUnbindChildAdapters_boundTemplate() {
    List<Content> contents =
        Collections.singletonList(
            Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    adapter.unbindChildAdapters(contents);

    // All (bound) adapters and views have been destroyed
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {0});
  }

  @Test
  public void testUnbindChildAdapters_boundContentBetweenInlineContents() {
    List<Content> contents = new ArrayList<>();
    contents.add(Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    contents.add(Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    contents.add(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.childAdapters).hasSize(6);
    TemplateInstanceAdapter mockTemplateAdapter = mock(TemplateInstanceAdapter.class);
    adapter.childAdapters.set(3, mockTemplateAdapter);

    adapter.unbindChildAdapters(contents);

    // The inline adapters have been unbound; the bound adapters have been destroyed.
    assertThat(adapter.childAdapters).hasSize(3);
    assertThat(adapter.childAdapters.get(0).getRawModel()).isNull();
    assertThat(adapter.childAdapters.get(1).getRawModel()).isNull();
    assertThat(adapter.childAdapters.get(2).getRawModel()).isNull();

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      assertThat(adapter.getBaseView().getChildAt(i))
          .isSameAs(adapter.childAdapters.get(i).getView());
    }

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2, 0, 1});

    verify(mockTemplateAdapter).unbindModel();
    verify(mockTemplateAdapter).releaseAdapter();
  }

  @Test
  public void testUnbindChildAdapters_withExtraViews() {
    List<Content> contents = new ArrayList<>();
    contents.add(Content.newBuilder().setTemplateInvocation(DEFAULT_TEMPLATE_INVOCATION).build());
    contents.add(Content.newBuilder().setTemplateBinding(DEFAULT_TEMPLATE_BINDING).build());
    contents.add(Content.newBuilder().setElement(DEFAULT_ELEMENT).build());

    View paddingView = new View(context);
    adapter.getBaseView().addView(paddingView);
    adapter.setFirstAdapterViewOffset(1);
    adapter.createInlineChildAdapters(contents, frameContext);
    adapter.bindChildAdapters(contents, frameContext);

    assertThat(adapter.childAdapters).hasSize(6);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(7);

    adapter.unbindChildAdapters(contents);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(4);
    for (int i = 0; i < 3; i++) {
      assertThat(adapter.getBaseView().getChildAt(i + 1))
          .isSameAs(adapter.childAdapters.get(i).getView());
    }

    assertThat(adapter.adaptersPerContent).isEqualTo(new int[] {2, 0, 1});
  }

  private static class TestElementContainerAdapter
      extends ElementContainerAdapter<LinearLayout, String> {

    TestElementContainerAdapter(
        Context context, AdapterParameters adapterParameters, LinearLayout view) {
      super(context, adapterParameters, view);
    }

    @Override
    String getModelFromElement(Element baseElement) {
      return baseElement.getVed();
    }
  }
}
