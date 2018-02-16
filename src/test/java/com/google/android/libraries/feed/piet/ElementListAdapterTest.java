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
import static com.google.android.libraries.feed.piet.StyleProviderImpl.DEFAULT_STYLE_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.ElementListAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.Producers;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityHorizontal;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link ElementListAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ElementListAdapterTest {

  private static final String LIST_STYLE_ID = "manycats";
  private static final StyleIdsStack LIST_STYLES =
      StyleIdsStack.newBuilder().addStyleIds(LIST_STYLE_ID).build();

  private static final Element ELEMENT_1 =
      Element.newBuilder().setSpacerElement(SpacerElement.getDefaultInstance()).build();

  @Mock private ActionHandler actionHandler;
  @Mock private FrameContext frameContext;
  @Mock private FrameContext childFrameContext;
  @Mock private StyleProvider styleProvider;
  @Mock private StyleProvider childStyleProvider;

  private Context context;
  private AdapterParameters adapterParameters;

  private ElementListAdapter adapter;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);

    adapterParameters = new AdapterParameters(context, Producers.of(null), actionHandler);
    adapter = new KeySupplier().getAdapter(context, adapterParameters);

    when(frameContext.bindNewStyle(StyleIdsStack.getDefaultInstance()))
        .thenReturn(childFrameContext);
    when(childFrameContext.makeStyleFor(any(StyleIdsStack.class)))
        .thenReturn(DEFAULT_STYLE_PROVIDER);
    when(childFrameContext.getCurrentStyle()).thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.makeStyleFor(StyleIdsStack.getDefaultInstance()))
        .thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.makeStyleFor(LIST_STYLES)).thenReturn(styleProvider);
    when(frameContext.getCurrentStyle()).thenReturn(styleProvider);
  }

  @Test
  public void testOnBindModel_makesList() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addElements(ELEMENT_1)
            .addElements(ELEMENT_1)
            .addElements(ELEMENT_1)
            .build();

    adapter.bindModel(listWithStyles, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(3);
    assertThat(adapter.getBaseView().getOrientation()).isEqualTo(LinearLayout.VERTICAL);
  }

  @Test
  public void testOnBindModel_setsStyles() {
    ElementList listWithStyles =
        ElementList.newBuilder().setStyleReferences(LIST_STYLES).addElements(ELEMENT_1).build();

    adapter.bindModel(listWithStyles, frameContext);

    verify(styleProvider).setElementStyles(context, frameContext, adapter.getView());
  }

  @Test
  public void testOnBindModel_setsActions() {
    Action action = Action.getDefaultInstance();
    ElementList listWithActions =
        ElementList.newBuilder()
            .addElements(ELEMENT_1)
            .setActions(Actions.newBuilder().setOnClick(action))
            .build();

    adapter.bindModel(listWithActions, frameContext);
    adapter.getView().callOnClick();

    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
    verify(actionHandler).handleAction(eq(action), any(Frame.class), any(String.class));
  }

  @Test
  public void testOnBindModel_setsBoundActions() {
    Action action = Action.getDefaultInstance();
    ActionsBindingRef actionsBindingRef =
        ActionsBindingRef.newBuilder().setBindingId("catastrophe").build();
    ElementList listWithActions =
        ElementList.newBuilder()
            .addElements(ELEMENT_1)
            .setActionsBinding(actionsBindingRef)
            .build();
    when(frameContext.getActionsFromBinding(actionsBindingRef))
        .thenReturn(Actions.newBuilder().setOnClick(action).build());

    adapter.bindModel(listWithActions, frameContext);
    adapter.getView().callOnClick();

    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
    verify(actionHandler).handleAction(eq(action), any(Frame.class), any(String.class));
  }

  @Test
  public void testBind_ElementDoesntOverrideListActions() {
    ElementList baseListElement =
        ElementList.newBuilder()
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();
    Element listElementWithActions = Element.newBuilder().setElementList(baseListElement).build();

    ElementListAdapter listAdapter =
        new ElementListAdapter.KeySupplier().getAdapter(context, adapterParameters);

    listAdapter.bindModel(baseListElement, listElementWithActions, frameContext);
    listAdapter.getView().callOnClick();

    assertThat(listAdapter.getView().hasOnClickListeners()).isTrue();
    verify(actionHandler)
        .handleAction(eq(Action.getDefaultInstance()), any(Frame.class), any(String.class));
  }

  @Test
  public void testBind_fallsBackToElementActions() {
    ElementList baseListElement = ElementList.getDefaultInstance();
    Element listElementWithActions =
        Element.newBuilder()
            .setElementList(baseListElement)
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();

    ElementListAdapter listAdapter =
        new ElementListAdapter.KeySupplier().getAdapter(context, adapterParameters);

    listAdapter.bindModel(baseListElement, listElementWithActions, frameContext);
    listAdapter.getView().callOnClick();

    assertThat(listAdapter.getView().hasOnClickListeners()).isTrue();
    verify(actionHandler)
        .handleAction(eq(Action.getDefaultInstance()), any(Frame.class), any(String.class));
  }

  @Test
  public void testUnbindModel_unsetsActions() {
    ElementList listWithActions =
        ElementList.newBuilder()
            .addElements(ELEMENT_1)
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();

    adapter.bindModel(listWithActions, frameContext);
    View view = adapter.getView();
    adapter.unbindModel();

    assertThat(view.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testOnBindModel_setsMargins() {
    String marginsStyleId = "spacecat";
    StyleIdsStack marginsStyles = StyleIdsStack.newBuilder().addStyleIds(marginsStyleId).build();
    Element elementWithMargins =
        Element.newBuilder()
            .setSpacerElement(SpacerElement.newBuilder().setStyleReferences(marginsStyles))
            .build();
    ElementList listWithStyles = ElementList.newBuilder().addElements(elementWithMargins).build();
    when(frameContext.makeStyleFor(marginsStyles)).thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.bindNewStyle(marginsStyles)).thenReturn(childFrameContext);
    when(childFrameContext.getCurrentStyle()).thenReturn(childStyleProvider);

    adapter.bindModel(listWithStyles, frameContext);

    // Assert that setMargins is called on the child's layout params
    ArgumentCaptor<MarginLayoutParams> capturedLayoutParams =
        ArgumentCaptor.forClass(MarginLayoutParams.class);
    verify(childStyleProvider).setMargins(eq(context), capturedLayoutParams.capture());

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(MarginLayoutParams.class);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isSameAs(capturedLayoutParams.getValue());
  }

  @Test
  public void testOnBindModel_setsGravity() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .addElements(
                ELEMENT_1.toBuilder().setGravityHorizontal(GravityHorizontal.GRAVITY_CENTER))
            .build();

    adapter.bindModel(listWithStyles, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    assertThat(((LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams()).gravity)
        .isEqualTo(Gravity.CENTER_HORIZONTAL);
  }

  @Test
  public void testUnbindModel() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setStyleReferences(LIST_STYLES)
            .addElements(ELEMENT_1)
            .addElements(ELEMENT_1)
            .addElements(ELEMENT_1)
            .build();

    adapter.bindModel(listWithStyles, frameContext);

    adapter.unbindModel();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testGetVerticalGravity() {
    ElementList listWithStyles =
        ElementList.newBuilder()
            .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
            .addElements(ELEMENT_1)
            .build();

    adapter.bindModel(listWithStyles, frameContext);

    assertThat(adapter.getVerticalGravity()).isEqualTo(GravityVertical.GRAVITY_MIDDLE);
  }

  @Test
  public void testGetStyleIdsStack() {
    ElementList listWithStyles =
        ElementList.newBuilder().setStyleReferences(LIST_STYLES).addElements(ELEMENT_1).build();

    adapter.bindModel(listWithStyles, frameContext);

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

    Element elementWithListBinding =
        Element.newBuilder()
            .setElementListBinding(ElementListBindingRef.newBuilder().setBindingId("binding"))
            .build();
    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.getModelFromElement(elementWithListBinding);
              }
            })
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Missing ElementList");

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
        .contains("Missing ElementList");

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
        .contains("Missing ElementList");
  }
}
