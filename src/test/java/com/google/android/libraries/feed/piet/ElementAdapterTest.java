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
import static com.google.android.libraries.feed.piet.ElementAdapter.DIMENSION_NOT_SET;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.ActionHandler.ActionType;
import com.google.android.libraries.feed.testing.shadows.ExtendedShadowView;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.ActionsProto.VisibilityAction;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.VisibilityBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityHorizontal;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.Visibility;
import com.google.search.now.ui.piet.ElementsProto.VisibilityState;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests of the {@link ElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class ElementAdapterTest {
  private static final int WIDTH = 123;
  private static final int HEIGHT = 456;

  @Mock private FrameContext frameContext;
  @Mock private HostProviders hostProviders;
  @Mock private ActionHandler actionHandler;
  @Mock private StyleProvider styleProvider;

  private Context context;
  private AdapterParameters parameters;
  private View view;

  private TestElementAdapter adapter;

  @Before
  public void setUp() {
    initMocks(this);
    context = RuntimeEnvironment.application;
    parameters = new AdapterParameters(context, Suppliers.of((ViewGroup) null), hostProviders);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(styleProvider);
    when(frameContext.getActionHandler()).thenReturn(actionHandler);
    view = new View(context);
    adapter = new TestElementAdapter(context, parameters, view);
  }

  @Test
  public void testGetters() {
    Element defaultElement =
        Element.newBuilder()
            .setVed("VED")
            .setGravityHorizontal(GravityHorizontal.GRAVITY_CENTER)
            .build();
    Frame frame = Frame.newBuilder().setTag("FRAME").build();
    when(frameContext.getFrame()).thenReturn(frame);

    adapter.createAdapter(defaultElement, defaultElement, frameContext);

    assertThat(adapter.getContext()).isSameAs(context);
    assertThat(adapter.getModel()).isSameAs(defaultElement);
    assertThat(adapter.getRawModel()).isSameAs(defaultElement);
    assertThat(adapter.getHorizontalGravity()).isEqualTo(GravityHorizontal.GRAVITY_CENTER);
    assertThat(adapter.getParameters()).isSameAs(parameters);
    assertThat(adapter.getTemplatedStringEvaluator()).isSameAs(parameters.templatedStringEvaluator);
    assertThat(adapter.getElementStyle()).isSameAs(styleProvider);
    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(StyleIdsStack.getDefaultInstance());
  }

  @Test
  public void testSetLayoutParams() {
    adapter.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));

    assertThat(view.getLayoutParams().width).isEqualTo(WIDTH);
    assertThat(view.getLayoutParams().height).isEqualTo(HEIGHT);
  }

  @Test
  public void testSetLayoutParams_overlay() {
    Element elementWithOverlays =
        Element.newBuilder()
            .addOverlays(
                Content.newBuilder()
                    .setElement(
                        Element.newBuilder().setElementList(ElementList.getDefaultInstance())))
            .build();

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    adapter.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));

    assertThat(adapter.getBaseView()).isNotEqualTo(adapter.getView());
    assertThat(adapter.getView().getLayoutParams().width).isEqualTo(WIDTH);
    assertThat(adapter.getView().getLayoutParams().height).isEqualTo(HEIGHT);
    assertThat(adapter.getBaseView().getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(adapter.getBaseView().getLayoutParams().height).isEqualTo(LayoutParams.MATCH_PARENT);

    adapter.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

    assertThat(adapter.getBaseView()).isNotEqualTo(adapter.getView());
    assertThat(adapter.getView().getLayoutParams().width).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(adapter.getView().getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(adapter.getBaseView().getLayoutParams().width).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(adapter.getBaseView().getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testGetComputedDimensions_defaults() {
    assertThat(adapter.getComputedWidthPx()).isEqualTo(DIMENSION_NOT_SET);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(DIMENSION_NOT_SET);
  }

  @Test
  public void testGetComputedDimensions_explicit() {
    adapter.setDims(WIDTH, HEIGHT);

    assertThat(adapter.getComputedWidthPx()).isEqualTo(WIDTH);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(HEIGHT);
  }

  @Test
  public void testCreateAdapter_callsOnCreateAdapter() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.createAdapter(defaultElement, defaultElement, frameContext);

    assertThat(adapter.created).isTrue();
  }

  @Test
  public void testCreateAdapter_extractsModelFromElement() {
    Element element = Element.getDefaultInstance();

    adapter.createAdapter(element, frameContext);

    assertThat(adapter.getModel()).isEqualTo(TestElementAdapter.DEFAULT_MODEL);
  }

  @Test
  public void testCreateAdapter_createsOverlays() {
    Element elementWithOverlays =
        Element.newBuilder()
            .addOverlays(
                Content.newBuilder()
                    .setElement(
                        Element.newBuilder().setElementList(ElementList.getDefaultInstance())))
            .build();

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat(adapter.getView()).isNotEqualTo(adapter.getBaseView());
    assertThat(adapter.getView()).isInstanceOf(FrameLayout.class);
    assertThat(adapter.overlayAdapter.childAdapters).isNotEmpty();
  }

  @Test
  public void testCreateAdapter_setsOverlayLayoutParams() {
    Content overlayContent =
        Content.newBuilder()
            .setElement(Element.newBuilder().setElementList(ElementList.getDefaultInstance()))
            .build();
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();

    ElementAdapterFactory mockFactory = mock(ElementAdapterFactory.class);
    StackedElementAdapter mockOverlayAdapter = mock(StackedElementAdapter.class);
    View overlayView = new View(context);
    when(mockFactory.createStackedElementAdapter(
            elementWithOverlays.getOverlaysList(), frameContext))
        .thenReturn(mockOverlayAdapter);
    when(mockOverlayAdapter.getView()).thenReturn(overlayView);
    when(mockOverlayAdapter.getElementStyle()).thenReturn(styleProvider);

    parameters =
        new AdapterParameters(
            context,
            Suppliers.of((ViewGroup) null),
            hostProviders,
            new ParameterizedTextEvaluator(),
            mockFactory,
            mock(TemplateBinder.class));
    adapter = new TestElementAdapter(context, parameters, view);

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat((((FrameLayout) adapter.getView()).getChildAt(1))).isSameAs(overlayView);
    ArgumentCaptor<LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(LayoutParams.class);
    verify(mockOverlayAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(LayoutParams.MATCH_PARENT);
  }

  @Test
  public void testCreateAdapter_doesNothingForBoundOverlays() {
    ElementBindingRef bindingRef = ElementBindingRef.newBuilder().setBindingId("overlay").build();
    BindingValue bindingValue =
        BindingValue.newBuilder()
            .setElement(Element.newBuilder().setElementList(ElementList.getDefaultInstance()))
            .build();
    Content overlayContent = Content.newBuilder().setBoundElement(bindingRef).build();
    when(frameContext.getElementBindingValue(bindingRef)).thenReturn(bindingValue);
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    // No overlays have been created yet, but adapter exists
    assertThat(adapter.overlayAdapter.childAdapters).isEmpty();
  }

  @Test
  public void testCreateAdapter_appliesVisibility() {
    Element defaultElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.INVISIBLE))
            .build();

    adapter.createAdapter(defaultElement, frameContext);

    assertThat(adapter.getView().getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testCreateAdapter_doesNothingWithVisibilityGone() {
    Element defaultElement =
        Element.newBuilder()
            .setVisibilityState(VisibilityState.newBuilder().setDefaultVisibility(Visibility.GONE))
            .build();

    adapter.createAdapter(defaultElement, frameContext);

    assertThat(adapter.created).isFalse();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void testCreateAdapter_ignoresBoundVisibility() {
    String visibilityBindingId = "invisible";
    VisibilityBindingRef visibilityBinding =
        VisibilityBindingRef.newBuilder().setBindingId(visibilityBindingId).build();
    Element defaultElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder()
                    .setDefaultVisibility(Visibility.VISIBLE)
                    .setOverridingBoundVisibility(visibilityBinding))
            .build();
    when(frameContext.getVisibilityBindingValue(visibilityBinding))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(visibilityBindingId)
                .setVisibility(Visibility.GONE)
                .build());

    adapter.createAdapter(defaultElement, frameContext);

    assertThat(adapter.created).isTrue();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void testCreateAdapter_setsDimensions() {
    int width = 10;
    int height = 20;

    when(styleProvider.hasWidth()).thenReturn(true);
    when(styleProvider.getWidth()).thenReturn(width);
    when(styleProvider.hasHeight()).thenReturn(true);
    when(styleProvider.getHeight()).thenReturn(height);

    adapter.createAdapter(Element.getDefaultInstance(), frameContext);

    assertThat(adapter.getComputedWidthPx()).isEqualTo(width);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(height);
  }

  @Test
  public void testCreateAdapter_resetsUnsetDimensions() {
    // Set some dimensions first
    int width = 10;
    int height = 20;

    when(styleProvider.hasWidth()).thenReturn(true);
    when(styleProvider.getWidth()).thenReturn(width);
    when(styleProvider.hasHeight()).thenReturn(true);
    when(styleProvider.getHeight()).thenReturn(height);

    adapter.createAdapter(Element.getDefaultInstance(), frameContext);
    adapter.releaseAdapter();

    // Recreate with new style that unsets the dimensions
    when(styleProvider.hasWidth()).thenReturn(false);
    when(styleProvider.getWidth()).thenReturn(999);
    when(styleProvider.hasHeight()).thenReturn(false);
    when(styleProvider.getHeight()).thenReturn(999);

    adapter.createAdapter(Element.getDefaultInstance(), frameContext);

    assertThat(adapter.getComputedWidthPx()).isEqualTo(DIMENSION_NOT_SET);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(DIMENSION_NOT_SET);
  }

  @Test
  public void testCreateAdapter_dimensionsDontOverrideSubclass() {
    int width = 10;
    int height = 20;

    final int subclassWidth = 30;
    final int subclassHeight = 40;

    adapter =
        new TestElementAdapter(context, parameters, view) {
          @Override
          protected void onCreateAdapter(
              Object model, Element baseElement, FrameContext frameContext) {
            widthPx = subclassWidth;
            heightPx = subclassHeight;
          }
        };

    when(styleProvider.hasWidth()).thenReturn(true);
    when(styleProvider.getWidth()).thenReturn(width);
    when(styleProvider.hasHeight()).thenReturn(true);
    when(styleProvider.getHeight()).thenReturn(height);

    adapter.createAdapter(Element.getDefaultInstance(), frameContext);

    assertThat(adapter.getComputedWidthPx()).isEqualTo(subclassWidth);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(subclassHeight);
  }

  @Test
  public void testBindModel_callsOnBindModel() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.bindModel(defaultElement, defaultElement, frameContext);

    assertThat(adapter.bound).isTrue();
  }

  @Test
  public void testBindModel_extractsModelFromElement() {
    Element element = Element.getDefaultInstance();

    adapter.bindModel(element, frameContext);

    assertThat(adapter.getModel()).isEqualTo(TestElementAdapter.DEFAULT_MODEL);
  }

  @Test
  public void testBindModel_bindsOverlays() {
    Content overlayContent =
        Content.newBuilder()
            .setElement(
                Element.newBuilder()
                    .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
                    .setElementList(ElementList.getDefaultInstance()))
            .build();
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);
    assertThat(adapter.overlayAdapter).isNotNull();

    StackedElementAdapter mockOverlayAdapter = mock(StackedElementAdapter.class);
    adapter.overlayAdapter = mockOverlayAdapter;

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);
    verify(mockOverlayAdapter)
        .bindModel(
            elementWithOverlays.getOverlaysList(), Element.getDefaultInstance(), frameContext);
  }

  @Test
  public void testBindModel_bindsBoundOverlays() {
    ElementBindingRef bindingRef = ElementBindingRef.newBuilder().setBindingId("overlay").build();
    BindingValue bindingValue =
        BindingValue.newBuilder()
            .setElement(Element.newBuilder().setElementList(ElementList.getDefaultInstance()))
            .build();
    Content overlayContent = Content.newBuilder().setBoundElement(bindingRef).build();
    when(frameContext.getElementBindingValue(bindingRef)).thenReturn(bindingValue);
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();
    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat(adapter.overlayAdapter.childAdapters.get(0)).isInstanceOf(ElementListAdapter.class);
  }

  @Test
  public void testBindModel_setsActions() {
    Actions actions = Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()).build();
    Element elementWithActions = Element.newBuilder().setActions(actions).build();

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
    assertThat(adapter.actions).isSameAs(actions);
  }

  @Test
  public void testBindModel_setsActionsWithBinding() {
    Actions actions = Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()).build();
    ActionsBindingRef actionsBinding =
        ActionsBindingRef.newBuilder().setBindingId("ACTION!").build();
    Element elementWithActions = Element.newBuilder().setActionsBinding(actionsBinding).build();
    when(frameContext.getActionsFromBinding(actionsBinding)).thenReturn(actions);

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
    assertThat(adapter.actions).isSameAs(actions);
  }

  @Test
  public void testBindModel_unsetsActions() {
    Actions actions = Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()).build();
    Element elementWithActions = Element.newBuilder().setActions(actions).build();
    Element elementWithoutActions = Element.getDefaultInstance();

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
    assertThat(adapter.actions).isSameAs(actions);

    adapter.bindModel(elementWithoutActions, elementWithoutActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isFalse();
    assertThat(adapter.actions).isSameAs(Actions.getDefaultInstance());
  }

  @Test
  public void testBindModel_failsWithIncompatibleOverlays() {
    Content overlayContent =
        Content.newBuilder()
            .setElement(
                Element.newBuilder()
                    .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
                    .setElementList(ElementList.getDefaultInstance()))
            .build();
    Element elementWithOverlays =
        Element.newBuilder().addOverlays(overlayContent).addOverlays(overlayContent).build();
    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);

    Element elementWithFewerOverlays = Element.newBuilder().addOverlays(overlayContent).build();

    assertThatRunnable(
            () ->
                adapter.bindModel(elementWithFewerOverlays, elementWithFewerOverlays, frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Internal error in adapters per content");
  }

  @Test
  public void testBindModel_setsVisibility() {
    Element visibleElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.VISIBLE))
            .build();
    adapter.createAdapter(visibleElement, frameContext);
    Element invisibleElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.INVISIBLE))
            .build();

    adapter.bindModel(invisibleElement, frameContext);

    assertThat(adapter.bound).isTrue();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testBindModel_doesNothingWhenVisibilityIsGone() {
    Element visibleElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.VISIBLE))
            .build();
    adapter.createAdapter(visibleElement, frameContext);
    Element goneElement =
        Element.newBuilder()
            .setVisibilityState(VisibilityState.newBuilder().setDefaultVisibility(Visibility.GONE))
            .build();

    adapter.bindModel(goneElement, frameContext);

    assertThat(adapter.bound).isFalse();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void testBindModel_recreatesWhenVisibilityWasGone() {
    Element goneElement =
        Element.newBuilder()
            .setVisibilityState(VisibilityState.newBuilder().setDefaultVisibility(Visibility.GONE))
            .build();
    adapter.createAdapter(goneElement, frameContext);
    Element invisibleElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.INVISIBLE))
            .build();

    adapter.bindModel(invisibleElement, frameContext);

    assertThat(adapter.created).isTrue();
    assertThat(adapter.bound).isTrue();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testBindModel_doesntRecreateWhenWasCreatedButVisibilityGone() {
    // Create a real adapter for a visible element.
    Element visibileElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.VISIBLE))
            .build();
    adapter.createAdapter(visibileElement, frameContext);

    // Bind a gone element, and unbind it so the visibility is GONE.
    Element goneElement =
        Element.newBuilder()
            .setVisibilityState(VisibilityState.newBuilder().setDefaultVisibility(Visibility.GONE))
            .build();
    adapter.bindModel(goneElement, frameContext);
    adapter.unbindModel();

    assertThat(adapter.created).isTrue();
    adapter.created = false; // set this so we can test that it doesn't get recreated

    // Bind a real element, and ensure that the adapter is not recreated.
    Element invisibleElement =
        Element.newBuilder()
            .setVisibilityState(
                VisibilityState.newBuilder().setDefaultVisibility(Visibility.INVISIBLE))
            .build();

    adapter.bindModel(invisibleElement, frameContext);

    assertThat(adapter.created).isFalse();
    assertThat(adapter.bound).isTrue();
    assertThat(adapter.getView().getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testUnbindModel_callsOnUnbindModel() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.createAdapter(defaultElement, defaultElement, frameContext);
    adapter.bindModel(defaultElement, defaultElement, frameContext);
    assertThat(adapter.bound).isTrue();

    adapter.unbindModel();
    assertThat(adapter.bound).isFalse();
  }

  @Test
  public void testUnbindModel_unsetsModel() {
    Element element = Element.getDefaultInstance();

    adapter.createAdapter(element, element, frameContext);
    adapter.bindModel(element, element, frameContext);
    assertThat(adapter.getModel()).isEqualTo(element);

    adapter.unbindModel();
    assertThat(adapter.getRawModel()).isNull();
  }

  @Test
  public void testUnbindModel_unsetsActions() {
    Element elementWithOverlaysAndActions =
        Element.newBuilder()
            .addOverlays(
                Content.newBuilder()
                    .setElement(
                        Element.newBuilder()
                            .setActions(
                                Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()))
                            .setElementList(ElementList.getDefaultInstance())))
            .setActions(Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()))
            .build();

    adapter.createAdapter(
        elementWithOverlaysAndActions, elementWithOverlaysAndActions, frameContext);
    adapter.bindModel(elementWithOverlaysAndActions, elementWithOverlaysAndActions, frameContext);
    View adapterView = adapter.getBaseView();
    View overlayView = adapter.getView();

    adapter.unbindModel();

    assertThat(adapterView.hasOnClickListeners()).isFalse();
    assertThat(overlayView.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testUnbindModel_unbindsOverlayAdapters() {
    Content overlayContent =
        Content.newBuilder()
            .setElement(
                Element.newBuilder()
                    .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
                    .setElementList(ElementList.getDefaultInstance()))
            .build();
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();
    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);
    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);
    assertThat(adapter.overlayAdapter.getRawModel()).isNotNull();

    adapter.unbindModel();
    assertThat(adapter.overlayAdapter.getRawModel()).isNull();
  }

  @Test
  public void testUnbindModel_releasesBoundOverlays() {
    Element overlayElement =
        Element.newBuilder()
            .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
            .setElementList(ElementList.getDefaultInstance())
            .build();
    String overlayBindingId = "overlay";
    ElementBindingRef overlayBindingRef =
        ElementBindingRef.newBuilder().setBindingId(overlayBindingId).build();
    BindingValue overlayBindingValue =
        BindingValue.newBuilder().setBindingId(overlayBindingId).setElement(overlayElement).build();
    when(frameContext.getElementBindingValue(overlayBindingRef)).thenReturn(overlayBindingValue);
    Content overlayContent = Content.newBuilder().setBoundElement(overlayBindingRef).build();
    Element elementWithOverlays = Element.newBuilder().addOverlays(overlayContent).build();
    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);
    assertThat(adapter.overlayAdapter).isNotNull();

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);
    assertThat(adapter.overlayAdapter).isNotNull();
    StackedElementAdapter overlayAdapter = adapter.overlayAdapter;
    assertThat(overlayAdapter.childAdapters).hasSize(1);
    assertThat(overlayAdapter.childAdapters.get(0)).isInstanceOf(ElementListAdapter.class);
    ElementListAdapter mockOverlayAdapter = mock(ElementListAdapter.class);
    overlayAdapter.childAdapters.set(0, mockOverlayAdapter);

    adapter.unbindModel();
    assertThat(adapter.overlayAdapter).isNotNull();
    assertThat(overlayAdapter.childAdapters).isEmpty();
    verify(mockOverlayAdapter).releaseAdapter();
  }

  @Test
  public void testReleaseAdapter_callsOnReleaseAdapter() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.createAdapter(defaultElement, defaultElement, frameContext);
    assertThat(adapter.created).isTrue();

    adapter.releaseAdapter();
    assertThat(adapter.created).isFalse();
  }

  @Test
  public void testReleaseAdapter_removesOverlays() {
    Element elementWithOverlays =
        Element.newBuilder()
            .addOverlays(
                Content.newBuilder()
                    .setElement(
                        Element.newBuilder().setElementList(ElementList.getDefaultInstance())))
            .build();

    adapter.createAdapter(elementWithOverlays, elementWithOverlays, frameContext);
    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);
    View adapterView = adapter.getBaseView();
    FrameLayout overlayView = (FrameLayout) adapter.getView();

    StackedElementAdapter mockOverlayAdapter = mock(StackedElementAdapter.class);
    when(mockOverlayAdapter.getKey()).thenReturn(SingletonKeySupplier.SINGLETON_KEY);
    adapter.overlayAdapter = mockOverlayAdapter;

    adapter.releaseAdapter();

    assertThat(adapter.getView()).isSameAs(adapterView);
    assertThat(adapter.getView()).isSameAs(adapter.getBaseView());
    assertThat(overlayView.getChildCount()).isEqualTo(0);
    verify(mockOverlayAdapter).releaseAdapter();
    assertThat(adapter.overlayAdapter).isNull();
  }

  @Test
  public void testReleaseAdapter_resetsVisibility() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.createAdapter(defaultElement, defaultElement, frameContext);
    adapter.setVisibility(Visibility.INVISIBLE);
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.INVISIBLE);

    adapter.releaseAdapter();
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void testSetVisibility() {
    adapter.createAdapter(Element.getDefaultInstance(), Element.getDefaultInstance(), frameContext);
    adapter.getBaseView().setVisibility(View.GONE);
    adapter.setVisibility(Visibility.VISIBLE);
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.VISIBLE);
    adapter.setVisibility(Visibility.INVISIBLE);
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.INVISIBLE);
    adapter.setVisibility(Visibility.GONE);
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.GONE);
    adapter.setVisibility(Visibility.VISIBILITY_UNSPECIFIED);
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Config(shadows = {ExtendedShadowView.class})
  @Test
  public void testGetOnFullViewActions() {
    Frame frame = Frame.newBuilder().setTag("FRAME").build();
    when(frameContext.getFrame()).thenReturn(frame);
    View viewport = new View(context);

    // No actions defined
    adapter.createAdapter(Element.getDefaultInstance(), frameContext);
    adapter.bindModel(Element.getDefaultInstance(), frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.releaseAdapter();

    // Actions defined, but not fullview actions
    Element elementWithNoViewActions =
        Element.newBuilder()
            .setActions(Actions.newBuilder().setOnClickAction(Action.newBuilder().build()))
            .build();
    adapter.createAdapter(elementWithNoViewActions, frameContext);
    adapter.bindModel(elementWithNoViewActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.unbindModel();
    adapter.releaseAdapter();

    // Actions defined, but not fully visible
    ExtendedShadowView viewShadow = Shadow.extract(adapter.getView());
    viewShadow.setAttachedToWindow(false);
    Actions fullViewActions =
        Actions.newBuilder()
            .addOnViewActions(VisibilityAction.newBuilder().setAction(Action.newBuilder().build()))
            .build();
    Element elementWithActions = Element.newBuilder().setActions(fullViewActions).build();
    adapter.createAdapter(elementWithActions, frameContext);
    adapter.bindModel(elementWithActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.unbindModel();
    adapter.releaseAdapter();

    // Actions defined, and fully visible
    ExtendedShadowView viewportShadow = Shadow.extract(viewport);
    viewportShadow.setLocationInWindow(0, 0);
    viewportShadow.setHeight(100);
    viewportShadow.setWidth(100);
    viewShadow.setLocationInWindow(10, 10);
    viewShadow.setHeight(50);
    viewShadow.setWidth(50);
    viewShadow.setAttachedToWindow(true);

    adapter.createAdapter(elementWithActions, frameContext);
    adapter.bindModel(elementWithActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verify(actionHandler)
        .handleAction(
            same(fullViewActions.getOnViewActions(0).getAction()),
            eq(ActionType.VIEW),
            eq(frame),
            eq(view),
            eq(null));
  }

  @Config(shadows = {ExtendedShadowView.class})
  @Test
  public void testGetOnPartialViewActions() {
    Frame frame = Frame.newBuilder().setTag("FRAME").build();
    when(frameContext.getFrame()).thenReturn(frame);
    View viewport = new View(context);

    // No actions defined
    adapter.createAdapter(Element.getDefaultInstance(), frameContext);
    adapter.bindModel(Element.getDefaultInstance(), frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.releaseAdapter();

    // Actions defined, but not partial view actions
    Element elementWithNoViewActions =
        Element.newBuilder()
            .setActions(Actions.newBuilder().setOnClickAction(Action.newBuilder().build()))
            .build();
    adapter.createAdapter(elementWithNoViewActions, frameContext);
    adapter.bindModel(elementWithNoViewActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.unbindModel();
    adapter.releaseAdapter();

    // Actions defined, but not attached to window
    ExtendedShadowView viewShadow = Shadow.extract(adapter.getView());
    viewShadow.setAttachedToWindow(false);
    Actions partialViewActions =
        Actions.newBuilder()
            .addOnViewActions(
                VisibilityAction.newBuilder()
                    .setAction(Action.newBuilder().build())
                    .setProportionVisible(0.01f))
            .build();
    Element elementWithActions = Element.newBuilder().setActions(partialViewActions).build();
    adapter.createAdapter(elementWithActions, frameContext);
    adapter.bindModel(elementWithActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verifyZeroInteractions(actionHandler);
    adapter.unbindModel();
    adapter.releaseAdapter();

    // Actions defined, and partially visible
    ExtendedShadowView viewportShadow = Shadow.extract(viewport);
    viewportShadow.setLocationInWindow(0, 0);
    viewportShadow.setHeight(100);
    viewportShadow.setWidth(100);
    viewShadow.setLocationInWindow(10, 10);
    viewShadow.setHeight(200);
    viewShadow.setWidth(200);
    viewShadow.setAttachedToWindow(true);

    adapter.createAdapter(elementWithActions, frameContext);
    adapter.bindModel(elementWithActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verify(actionHandler)
        .handleAction(
            same(partialViewActions.getOnViewActions(0).getAction()),
            eq(ActionType.VIEW),
            eq(frame),
            eq(view),
            eq(null));

    // Actions defined, and fully visible
    viewShadow.setHeight(50);
    viewShadow.setWidth(50);

    adapter.createAdapter(elementWithActions, frameContext);
    adapter.bindModel(elementWithActions, frameContext);
    adapter.triggerViewActions(viewport, frameContext);
    verify(actionHandler, times(2))
        .handleAction(
            same(partialViewActions.getOnViewActions(0).getAction()),
            eq(ActionType.VIEW),
            eq(frame),
            eq(view),
            eq(null));
  }

  // Dummy implementation
  static class TestElementAdapter extends ElementAdapter<View, Object> {
    static final String DEFAULT_MODEL = "MODEL";

    boolean created = false;
    boolean bound = false;

    View adapterView;

    TestElementAdapter(Context context, AdapterParameters parameters, View adapterView) {
      super(context, parameters, adapterView);
      this.adapterView = adapterView;
    }

    @Override
    protected Object getModelFromElement(Element baseElement) {
      return DEFAULT_MODEL;
    }

    @Override
    protected void onCreateAdapter(Object model, Element baseElement, FrameContext frameContext) {
      created = true;
    }

    @Override
    protected void onBindModel(Object model, Element baseElement, FrameContext frameContext) {
      bound = true;
    }

    @Override
    protected void onUnbindModel() {
      bound = false;
    }

    @Override
    protected void onReleaseAdapter() {
      created = false;
    }

    public void setDims(int width, int height) {
      widthPx = width;
      heightPx = height;
    }
  }
}
