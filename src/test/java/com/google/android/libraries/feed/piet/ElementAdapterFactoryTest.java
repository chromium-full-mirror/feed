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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.FrameContext.ErrorType;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.TemplateBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

/** Tests for the ElementAdapterFactory. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ElementAdapterFactoryTest {

  @Mock private FrameContext frameContext;

  @Mock private CustomElementAdapter customElementAdapter;
  @Mock private TextElementAdapter textElementAdapter;
  @Mock private ImageElementAdapter imageElementAdapter;
  @Mock private SpacerElementAdapter spacerElementAdapter;
  @Mock private GridRowAdapter gridRowAdapter;
  @Mock private ElementListAdapter elementListAdapter;
  @Mock private TemplateInvocationAdapter templateAdapter;

  @Mock private AdapterFactory<CustomElementAdapter, CustomElement> customElementFactory;

  @Mock private AdapterFactory<TextElementAdapter, TextElement> textElementFactory;

  @Mock private AdapterFactory<ImageElementAdapter, ImageElement> imageElementFactory;

  @Mock private AdapterFactory<SpacerElementAdapter, SpacerElement> spacerElementFactory;

  @Mock private AdapterFactory<GridRowAdapter, GridRow> gridRowFactory;

  @Mock private AdapterFactory<ElementListAdapter, ElementList> elementListFactory;

  @Mock private AdapterFactory<TemplateInvocationAdapter, TemplateInvocation> templateFactory;

  private final List<AdapterFactory<?, ?>> adapterFactories = new ArrayList<>();

  private ElementAdapterFactory adapterFactory;

  @Before
  public void setUp() {
    initMocks(this);
    adapterFactory =
        new ElementAdapterFactory(
            customElementFactory,
            textElementFactory,
            imageElementFactory,
            spacerElementFactory,
            gridRowFactory,
            elementListFactory,
            templateFactory);
    when(customElementFactory.get(any(CustomElement.class), any(), eq(frameContext)))
        .thenReturn(customElementAdapter);
    when(textElementFactory.get(any(TextElement.class), any(), eq(frameContext)))
        .thenReturn(textElementAdapter);
    when(imageElementFactory.get(any(ImageElement.class), any(), eq(frameContext)))
        .thenReturn(imageElementAdapter);
    when(spacerElementFactory.get(any(SpacerElement.class), any(), eq(frameContext)))
        .thenReturn(spacerElementAdapter);
    when(gridRowFactory.get(any(GridRow.class), any(), eq(frameContext)))
        .thenReturn(gridRowAdapter);
    when(elementListFactory.get(any(ElementList.class), eq(frameContext)))
        .thenReturn(elementListAdapter);
    when(elementListFactory.get(any(ElementList.class), any(), eq(frameContext)))
        .thenReturn(elementListAdapter);
    when(templateFactory.get(any(TemplateInvocation.class), eq(frameContext)))
        .thenReturn(templateAdapter);
    when(templateFactory.get(any(TemplateInvocation.class), any(), eq(frameContext)))
        .thenReturn(templateAdapter);
    adapterFactories.add(customElementFactory);
    adapterFactories.add(textElementFactory);
    adapterFactories.add(imageElementFactory);
    adapterFactories.add(spacerElementFactory);
    adapterFactories.add(gridRowFactory);
    adapterFactories.add(elementListFactory);
    adapterFactories.add(templateFactory);
  }

  @Test
  public void testCreateAdapterForElement_customElement() {
    CustomElement model = CustomElement.getDefaultInstance();
    Element element = Element.newBuilder().setCustomElement(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(customElementAdapter);
    verify(customElementFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_textElement() {
    TextElement model = TextElement.getDefaultInstance();
    Element element = Element.newBuilder().setTextElement(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(textElementAdapter);
    verify(textElementFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_imageElement() {
    ImageElement model = ImageElement.getDefaultInstance();
    Element element = Element.newBuilder().setImageElement(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(imageElementAdapter);
    verify(imageElementFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_spacerElement() {
    SpacerElement model = SpacerElement.getDefaultInstance();
    Element element = Element.newBuilder().setSpacerElement(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(spacerElementAdapter);
    verify(spacerElementFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_gridRow() {
    GridRow model = GridRow.getDefaultInstance();
    Element element = Element.newBuilder().setGridRow(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(gridRowAdapter);
    verify(gridRowFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_elementList() {
    ElementList model = ElementList.getDefaultInstance();
    Element element = Element.newBuilder().setElementList(model).build();
    assertThat(adapterFactory.createAdapterForElement(element, frameContext))
        .isSameAs(elementListAdapter);
    verify(elementListFactory).get(model, element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_elementListBinding() {
    ElementListBindingRef bindingRef =
        ElementListBindingRef.newBuilder().setBindingId("ELEMENTLIST").build();
    Element element = Element.newBuilder().setElementListBinding(bindingRef).build();
    ElementList elementList =
        ElementList.newBuilder().setGravityVertical(GravityVertical.GRAVITY_MIDDLE).build();
    BindingValue bindingValue = BindingValue.newBuilder().setElementList(elementList).build();
    when(frameContext.getElementListBindingValue(bindingRef)).thenReturn(bindingValue);
    ElementAdapter<?, ?> adapter = adapterFactory.createAdapterForElement(element, frameContext);
    ArgumentCaptor<ElementList> captor = ArgumentCaptor.forClass(ElementList.class);
    verify(elementListFactory).get(captor.capture(), eq(element), eq(frameContext));
    assertThat(adapter).isSameAs(elementListAdapter);
    assertThat(captor.getValue()).isSameAs(elementList);
  }

  @Test
  public void testCreateAdapterForElement_templateBinding() {
    TemplateBindingRef bindingRef =
        TemplateBindingRef.newBuilder().setBindingId("TEMPLATE").build();
    Element element = Element.newBuilder().setTemplateBinding(bindingRef).build();
    TemplateInvocation templateInvocation =
        TemplateInvocation.newBuilder().setTemplateId("potato").build();
    when(frameContext.getTemplateInvocationFromBinding(bindingRef)).thenReturn(templateInvocation);

    ElementAdapter<?, ?> adapter = adapterFactory.createAdapterForElement(element, frameContext);

    verify(templateFactory).get(templateInvocation, element, frameContext);
    assertThat(adapter).isSameAs(templateAdapter);

    when(frameContext.getTemplateInvocationFromBinding(bindingRef)).thenReturn(null);

    adapterFactory.createAdapterForElement(element, frameContext);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(frameContext).reportError(eq(ErrorType.ERROR), captor.capture());
    verify(templateFactory).get(TemplateInvocation.getDefaultInstance(), element, frameContext);
  }

  @Test
  public void testCreateAdapterForElement_unsupported() {
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapterFactory.createAdapterForElement(Element.getDefaultInstance(), frameContext);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Unsupported Element type");
  }

  @Test
  public void testReleaseAdapter_custom() {
    adapterFactory.releaseAdapter(customElementAdapter);
    verify(customElementFactory).release(customElementAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_text() {
    adapterFactory.releaseAdapter(textElementAdapter);
    verify(textElementFactory).release(textElementAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_imageElement() {
    adapterFactory.releaseAdapter(imageElementAdapter);
    verify(imageElementFactory).release(imageElementAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_spacer() {
    adapterFactory.releaseAdapter(spacerElementAdapter);
    verify(spacerElementFactory).release(spacerElementAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_gridRow() {
    adapterFactory.releaseAdapter(gridRowAdapter);
    verify(gridRowFactory).release(gridRowAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_list() {
    adapterFactory.releaseAdapter(elementListAdapter);
    verify(elementListFactory).release(elementListAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testReleaseAdapter_template() {
    adapterFactory.releaseAdapter(templateAdapter);
    verify(templateFactory).release(templateAdapter);
    testNoOtherFactoryInteractions();
  }

  @Test
  public void testCreateElementListAdapter() {
    ElementList list = ElementList.getDefaultInstance();
    ElementListAdapter adapter = adapterFactory.createElementListAdapter(list, frameContext);
    verify(elementListFactory).get(list, frameContext);
    assertThat(adapter).isSameAs(elementListAdapter);
  }

  @Test
  public void testCreateTemplateAdapter() {
    TemplateInvocation template = TemplateInvocation.getDefaultInstance();
    TemplateInvocationAdapter adapter =
        adapterFactory.createTemplateAdapter(template, frameContext);
    verify(templateFactory).get(template, frameContext);
    assertThat(adapter).isSameAs(templateAdapter);
  }

  private void testNoOtherFactoryInteractions() {
    for (AdapterFactory<?, ?> adapterFactory : adapterFactories) {
      verifyNoMoreInteractions(adapterFactory);
    }
  }
}
