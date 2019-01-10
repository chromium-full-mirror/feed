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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.common.testing.FakeClock;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.piet.ElementListAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.testing.shadows.ExtendedShadowLinearLayout;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.ElementsProto.Visibility;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.GravityHorizontal;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests of the {@link ElementListAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class ElementListAdapterTest {

  private static final String LIST_STYLE_ID = "manycats";
  private static final StyleIdsStack LIST_STYLES =
      StyleIdsStack.newBuilder().addStyleIds(LIST_STYLE_ID).build();

  private static final Element DEFAULT_ELEMENT =
      Element.newBuilder().setSpacerElement(SpacerElement.getDefaultInstance()).build();
  private static final Content DEFAULT_CONTENT =
      Content.newBuilder().setElement(DEFAULT_ELEMENT).build();
  private static final Element IMAGE_ELEMENT =
      Element.newBuilder().setImageElement(ImageElement.getDefaultInstance()).build();
  private static final ElementList DEFAULT_LIST =
      ElementList.newBuilder().addContents(DEFAULT_CONTENT).build();

  private static final ElementBindingRef ELEMENT_BINDING_REF =
      ElementBindingRef.newBuilder().setBindingId("shopping").build();
  private static final Element LIST_WITH_BOUND_LIST =
      asElement(
          ElementList.newBuilder()
              .addContents(Content.newBuilder().setBoundElement(ELEMENT_BINDING_REF))
              .build());

  @Mock private ActionHandler actionHandler;
  @Mock private FrameContext frameContext;
  @Mock private StyleProvider styleProvider;
  @Mock private HostProviders hostProviders;
  @Mock private AssetProvider assetProvider;

  private Context context;
  private AdapterParameters adapterParameters;

  private ElementListAdapter adapter;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();

    adapterParameters =
        new AdapterParameters(context, Suppliers.of(null), hostProviders, new FakeClock());

    when(frameContext.makeStyleFor(StyleIdsStack.getDefaultInstance()))
        .thenReturn(adapterParameters.defaultStyleProvider);
    when(frameContext.makeStyleFor(LIST_STYLES)).thenReturn(styleProvider);
    when(styleProvider.getPadding()).thenReturn(EdgeWidths.getDefaultInstance());
    when(frameContext.getActionHandler()).thenReturn(actionHandler);
    when(hostProviders.getAssetProvider()).thenReturn(assetProvider);
    when(styleProvider.getRoundedCorners()).thenReturn(RoundedCorners.getDefaultInstance());

    adapter = new KeySupplier().getAdapter(context, adapterParameters);
  }

  @Test
  public void testViewDoesNotClip() {
    assertThat(adapter.getBaseView().getClipToPadding()).isFalse();
  }

  @Test
  public void testOnCreateAdapter_makesList() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    assertThat(adapter.getBaseView().getOrientation()).isEqualTo(LinearLayout.VERTICAL);
  }

  @Test
  public void testOnCreateAdapter_setsStyles() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);

    verify(styleProvider).applyElementStyles(adapter);
  }

  @Config(shadows = {ExtendedShadowLinearLayout.class})
  @Test
  public void testTriggerActions_triggersChildren() {
    Frame frame = Frame.newBuilder().setTag("Frame").build();
    when(frameContext.getFrame()).thenReturn(frame);
    Element baseElement =
        Element.newBuilder()
            .setElementList(
                ElementList.newBuilder()
                    .addContents(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setElementList(ElementList.getDefaultInstance()))))
            .build();
    adapter.createAdapter(baseElement, frameContext);
    adapter.bindModel(baseElement, frameContext);

    // Replace the child adapter so we can verify on it
    ElementAdapter<?, ?> mockChildAdapter = mock(ElementAdapter.class);
    adapter.childAdapters.set(0, mockChildAdapter);

    ExtendedShadowLinearLayout shadowView = Shadow.extract(adapter.getView());
    adapter.getView().setVisibility(View.VISIBLE);
    shadowView.setAttachedToWindow(true);

    View viewport = new View(context);

    adapter.triggerViewActions(viewport, frameContext);

    verify(mockChildAdapter).triggerViewActions(viewport, frameContext);
  }

  @Test
  public void testOnBindModel_setsStylesOnlyIfBindingIsDefined() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);
    verify(frameContext).makeStyleFor(LIST_STYLES);

    // Binding an element with a different style will not apply the new style
    StyleIdsStack otherStyles = StyleIdsStack.newBuilder().addStyleIds("bobcat").build();
    ElementList otherListWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(otherStyles)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.bindModel(asElement(otherListWithStyles), frameContext);
    verify(frameContext, never()).makeStyleFor(otherStyles);

    // But binding an element with a style binding will re-apply the style
    StyleIdsStack otherStylesWithBinding =
        StyleIdsStack.newBuilder()
            .addStyleIds("bobcat")
            .setStyleBinding(StyleBindingRef.newBuilder().setBindingId("lynx"))
            .build();
    ElementList otherListWithStyleBindings =
        ElementList.newBuilder()
            .setStyleReferences(otherStylesWithBinding)
            .addContents(DEFAULT_CONTENT)
            .build();
    when(frameContext.makeStyleFor(otherStylesWithBinding)).thenReturn(styleProvider);

    adapter.bindModel(asElement(otherListWithStyleBindings), frameContext);
    verify(frameContext).makeStyleFor(otherStylesWithBinding);
  }

  @Test
  public void testOnBindModel_failsWithIncompatibleModel() {
    ElementList listWithThreeElements =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithThreeElements), frameContext);
    adapter.bindModel(asElement(listWithThreeElements), frameContext);
    adapter.unbindModel();

    ElementList listWithTwoElements =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .build();

    assertThatRunnable(() -> adapter.bindModel(asElement(listWithTwoElements), frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Internal error in adapters per content");
  }

  @Test
  public void testOnBindModel_elementListBindingRecreatesAdapter() {
    Element listWithOneItem =
        Element.newBuilder()
            .setElementList(ElementList.newBuilder().addContents(DEFAULT_CONTENT))
            .build();
    Element listWithTwoItems =
        Element.newBuilder()
            .setElementList(
                ElementList.newBuilder().addContents(DEFAULT_CONTENT).addContents(DEFAULT_CONTENT))
            .build();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(BindingValue.newBuilder().setElement(listWithOneItem).build());
    adapter.createAdapter(LIST_WITH_BOUND_LIST, frameContext);
    // No child adapters have been created yet.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(BindingValue.newBuilder().setElement(listWithTwoItems).build());
    adapter.bindModel(LIST_WITH_BOUND_LIST, frameContext);
    // The list adapter creates its one view on bind.
    assertThat(((LinearLayout) adapter.getBaseView().getChildAt(0)).getChildCount()).isEqualTo(2);

    adapter.unbindModel();
    // The list adapter has been released.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(BindingValue.newBuilder().setElement(listWithOneItem).build());
    adapter.bindModel(LIST_WITH_BOUND_LIST, frameContext);
    // The list adapter can bind to a different model.
    assertThat(((LinearLayout) adapter.getBaseView().getChildAt(0)).getChildCount()).isEqualTo(1);
  }

  @Test
  public void testOnBindModel_bindingWithVisibilityGone() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(ELEMENT_BINDING_REF.getBindingId())
                .setElement(Element.newBuilder().setElementList(DEFAULT_LIST))
                .build());
    adapter.createAdapter(LIST_WITH_BOUND_LIST, frameContext);
    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(ELEMENT_BINDING_REF.getBindingId())
                .setVisibility(Visibility.GONE)
                .build());

    adapter.bindModel(LIST_WITH_BOUND_LIST, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_bindingWithNoContent() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(ELEMENT_BINDING_REF.getBindingId())
                .setElement(DEFAULT_ELEMENT)
                .build());
    adapter.createAdapter(LIST_WITH_BOUND_LIST, frameContext);
    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(
            BindingValue.newBuilder().setBindingId(ELEMENT_BINDING_REF.getBindingId()).build());

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_bindingWithOptionalAbsent() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING_REF))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(ELEMENT_BINDING_REF.getBindingId())
                .setElement(DEFAULT_ELEMENT)
                .build());
    adapter.createAdapter(LIST_WITH_BOUND_LIST, frameContext);

    ElementBindingRef optionalBinding = ELEMENT_BINDING_REF.toBuilder().setIsOptional(true).build();
    ElementList optionalBindingList =
        ElementList.newBuilder()
            .addContents(Content.newBuilder().setBoundElement(optionalBinding))
            .build();
    when(frameContext.getElementBindingValue(optionalBinding))
        .thenReturn(BindingValue.newBuilder().setBindingId(optionalBinding.getBindingId()).build());

    adapter.bindModel(asElement(optionalBindingList), frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_setsLayoutParams() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .addContents(
                Content.newBuilder()
                    .setElement(
                        DEFAULT_ELEMENT
                            .toBuilder()
                            .setGravityHorizontal(GravityHorizontal.GRAVITY_CENTER)))
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);
    adapter.bindModel(asElement(listWithStyles), frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).gravity)
        .isEqualTo(Gravity.CENTER_HORIZONTAL);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).width)
        .isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).height)
        .isEqualTo(0);
  }

  @Test
  public void testOnBindModel_setsMargins() {
    String marginsStyleId = "spacecat";
    StyleIdsStack marginsStyles = StyleIdsStack.newBuilder().addStyleIds(marginsStyleId).build();
    Element elementWithMargins =
        Element.newBuilder()
            .setSpacerElement(SpacerElement.newBuilder().setStyleReferences(marginsStyles))
            .build();
    ElementList listWithStyles =
        ElementList.newBuilder()
            .addContents(Content.newBuilder().setElement(elementWithMargins))
            .build();
    when(frameContext.makeStyleFor(marginsStyles)).thenReturn(styleProvider);

    adapter.createAdapter(asElement(listWithStyles), frameContext);
    adapter.bindModel(asElement(listWithStyles), frameContext);

    // Assert that applyMargins is called on the child's layout params
    ArgumentCaptor<MarginLayoutParams> capturedLayoutParams =
        ArgumentCaptor.forClass(MarginLayoutParams.class);
    verify(styleProvider).applyMargins(eq(context), capturedLayoutParams.capture());

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(MarginLayoutParams.class);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isSameAs(capturedLayoutParams.getValue());
  }

  @Test
  public void testReleaseAdapter() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);
    adapter.bindModel(asElement(listWithStyles), frameContext);

    adapter.releaseAdapter();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testGetVerticalGravity_noModel() {
    assertThat(adapter.getVerticalGravity(Gravity.CLIP_VERTICAL)).isEqualTo(Gravity.CLIP_VERTICAL);
  }

  @Test
  public void testGetStyleIdsStack() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addContents(DEFAULT_CONTENT)
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);

    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(LIST_STYLES);
  }

  @Test
  public void testGetModelFromElement() {
    ElementList model =
        ElementList.newBuilder()
            .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds("spacer"))
            .build();

    Element elementWithModel = Element.newBuilder().setElementList(model).build();
    assertThat(adapter.getModelFromElement(elementWithModel)).isSameAs(model);

    Element elementWithWrongModel =
        Element.newBuilder().setCustomElement(CustomElement.getDefaultInstance()).build();
    assertThatRunnable(() -> adapter.getModelFromElement(elementWithWrongModel))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing ElementList");

    Element emptyElement = Element.getDefaultInstance();
    assertThatRunnable(() -> adapter.getModelFromElement(emptyElement))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing ElementList");
  }

  @Test
  public void testSetLayoutParams() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .addContents(
                Content.newBuilder()
                    .setElement(
                        DEFAULT_ELEMENT
                            .toBuilder()
                            .setGravityHorizontal(GravityHorizontal.GRAVITY_CENTER)))
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);

    LayoutParams layoutParams =
        new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    adapter.setLayoutParams(layoutParams);

    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).gravity)
        .isEqualTo(Gravity.CENTER_HORIZONTAL);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).width)
        .isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).height)
        .isEqualTo(0);
  }

  @Test
  public void testSetLayoutParams_childWidthSet() {
    int childWidth = 5;

    ElementList listWithStyles =
        ElementList.newBuilder()
            .addContents(Content.newBuilder().setElement(IMAGE_ELEMENT))
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);
    StyleProvider childStyleProvider = mock(StyleProvider.class);
    ImageElementAdapter mockChildAdapter = mock(ImageElementAdapter.class);
    when(mockChildAdapter.getComputedWidthPx()).thenReturn(childWidth);
    when(mockChildAdapter.getElementStyle()).thenReturn(childStyleProvider);
    when(mockChildAdapter.getHorizontalGravity(anyInt())).thenReturn(Gravity.CENTER_HORIZONTAL);
    when(mockChildAdapter.getElementStyle().hasGravityHorizontal()).thenReturn(true);
    adapter.childAdapters.set(0, mockChildAdapter);

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    adapter.setLayoutParams(params);

    ArgumentCaptor<LinearLayout.LayoutParams> childLayoutParamsCaptor =
        ArgumentCaptor.forClass(LinearLayout.LayoutParams.class);
    verify(mockChildAdapter).setLayoutParams(childLayoutParamsCaptor.capture());

    LinearLayout.LayoutParams childLayoutParams = childLayoutParamsCaptor.getValue();
    assertThat(childLayoutParams.width).isEqualTo(childWidth);
    assertThat(childLayoutParams.gravity).isEqualTo(Gravity.CENTER_HORIZONTAL);

    verify(childStyleProvider).applyMargins(context, childLayoutParams);
  }

  @Test
  public void testSetLayoutParams_widthSetOnList() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .addContents(Content.newBuilder().setElement(IMAGE_ELEMENT))
            .build();

    adapter.createAdapter(asElement(listWithStyles), frameContext);

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.weight = 1;
    adapter.setLayoutParams(params);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).width)
        .isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).height)
        .isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  private static Element asElement(ElementList elementList) {
    return Element.newBuilder().setElementList(elementList).build();
  }
}
