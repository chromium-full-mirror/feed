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

import static com.google.android.libraries.feed.piet.StyleProviderImpl.DEFAULT_STYLE_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ParameterizedTextEvaluator;
import com.google.android.libraries.feed.piet.util.Producers;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityHorizontal;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
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

/** Tests of the {@link ElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ElementAdapterTest {
  private static final int WIDTH = 123;
  private static final int HEIGHT = 456;

  @Mock private FrameContext frameContext;
  @Mock private ActionHandler actionHandler;
  @Mock private StyleProvider styleProvider;

  private Context context;
  private AdapterParameters parameters;
  private View view;

  private TestElementAdapter adapter;

  @Before
  public void setUp() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    parameters = new AdapterParameters(context, Producers.of((ViewGroup) null), actionHandler);
    when(frameContext.bindNewStyle(any(StyleIdsStack.class))).thenReturn(frameContext);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.getCurrentStyle()).thenReturn(styleProvider);
    adapter = new TestElementAdapter(context, parameters);
    view = new RelativeLayout(context);
    adapter.initializeAdapter(view, null);
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

    adapter.bindModel(defaultElement, defaultElement, frameContext);

    assertThat(adapter.getContext()).isSameAs(context);
    assertThat(adapter.getModel()).isSameAs(defaultElement);
    assertThat(adapter.getRawModel()).isSameAs(defaultElement);
    assertThat(adapter.getHorizontalGravity()).isEqualTo(GravityHorizontal.GRAVITY_CENTER);
    assertThat(adapter.getFrame()).isEqualTo(frame);
    assertThat(adapter.getFrameContext()).isSameAs(frameContext);
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
  public void testSetLayoutParams_nullView() {
    adapter.initializeAdapter(null, null);

    // Should not throw NPE.
    adapter.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));
  }

  @Test
  public void testGetComputedDimensions_defaults() {
    assertThat(adapter.getComputedWidthPx()).isEqualTo(BoundViewAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(BoundViewAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testGetComputedDimensions_explicit() {
    adapter.setDims(WIDTH, HEIGHT);

    assertThat(adapter.getComputedWidthPx()).isEqualTo(WIDTH);
    assertThat(adapter.getComputedHeightPx()).isEqualTo(HEIGHT);
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
  public void testBindModel_setsOverlays() {
    Element elementWithOverlays =
        Element.newBuilder().addOverlayElements(ElementList.getDefaultInstance()).build();

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat(adapter.getView()).isNotEqualTo(adapter.getBaseView());
    assertThat(adapter.getView()).isInstanceOf(FrameLayout.class);
    // Check gravity on the overlay view
    assertThat(
            ((FrameLayout.LayoutParams)
                    ((FrameLayout) adapter.getView()).getChildAt(1).getLayoutParams())
                .gravity)
        .isEqualTo(Gravity.NO_GRAVITY);
  }

  @Test
  public void testBindModel_setsOverlaysWithGravity() {
    Element elementWithOverlays =
        Element.newBuilder()
            .addOverlayElements(
                ElementList.newBuilder().setGravityVertical(GravityVertical.GRAVITY_MIDDLE))
            .build();

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat(adapter.getView()).isNotEqualTo(adapter.getBaseView());
    assertThat(adapter.getView()).isInstanceOf(FrameLayout.class);
    // Check gravity on the overlay view
    assertThat(
            ((FrameLayout.LayoutParams)
                    ((FrameLayout) adapter.getView()).getChildAt(1).getLayoutParams())
                .gravity)
        .isEqualTo(Gravity.CENTER_VERTICAL);
  }

  @Test
  public void testBindModel_setsOverlayLayoutParams() {
    Element elementWithOverlays =
        Element.newBuilder().addOverlayElements(ElementList.getDefaultInstance()).build();

    ElementAdapterFactory mockFactory = mock(ElementAdapterFactory.class);
    ElementListAdapter mockOverlayAdapter = mock(ElementListAdapter.class);
    View overlayView = new View(context);
    when(mockFactory.createElementListAdapter(ElementList.getDefaultInstance(), frameContext))
        .thenReturn(mockOverlayAdapter);
    when(mockOverlayAdapter.getView()).thenReturn(overlayView);
    when(mockOverlayAdapter.getVerticalGravity()).thenReturn(GravityVertical.GRAVITY_TOP);
    when(mockOverlayAdapter.getElementStyle()).thenReturn(styleProvider);

    parameters =
        new AdapterParameters(
            context,
            Producers.of((ViewGroup) null),
            actionHandler,
            new ParameterizedTextEvaluator(),
            mockFactory);
    adapter = new TestElementAdapter(context, parameters);
    view = new RelativeLayout(context);
    adapter.initializeAdapter(view, null);

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);

    assertThat((((FrameLayout) adapter.getView()).getChildAt(1))).isSameAs(overlayView);
    ArgumentCaptor<LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(LayoutParams.class);
    verify(mockOverlayAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    verify(styleProvider).setMargins(context, (MarginLayoutParams) layoutParamsCaptor.getValue());
  }

  @Test
  public void testBindModel_setsActions() {
    Element elementWithActions =
        Element.newBuilder()
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
  }

  @Test
  public void testBindModel_setsActionsWithBinding() {
    ActionsBindingRef actionsBinding =
        ActionsBindingRef.newBuilder().setBindingId("ACTION!").build();
    Element elementWithActions = Element.newBuilder().setActionsBinding(actionsBinding).build();
    when(frameContext.getActionsFromBinding(actionsBinding))
        .thenReturn(Actions.newBuilder().setOnClick(Action.getDefaultInstance()).build());

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();
  }

  @Test
  public void testBindModel_unsetsActions() {
    Element elementWithActions =
        Element.newBuilder()
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();
    Element elementWithoutActions = Element.getDefaultInstance();

    adapter.bindModel(elementWithActions, elementWithActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isTrue();

    adapter.bindModel(elementWithoutActions, elementWithoutActions, frameContext);
    assertThat(adapter.getView().hasOnClickListeners()).isFalse();
  }

  @Test
  public void testUnbindModel_callsOnUnbindModel() {
    Element defaultElement = Element.getDefaultInstance();

    adapter.bindModel(defaultElement, defaultElement, frameContext);
    assertThat(adapter.bound).isTrue();

    adapter.unbindModel();
    assertThat(adapter.bound).isFalse();
  }

  @Test
  public void testUnbindModel_unsetsModel() {
    Element element = Element.getDefaultInstance();

    adapter.bindModel(element, element, frameContext);
    assertThat(adapter.getModel()).isEqualTo(element);

    adapter.unbindModel();
    assertThat(adapter.getRawModel()).isNull();
  }

  @Test
  public void testUnbindModel_removesOverlays() {
    Element elementWithOverlays =
        Element.newBuilder().addOverlayElements(ElementList.getDefaultInstance()).build();

    adapter.bindModel(elementWithOverlays, elementWithOverlays, frameContext);
    View adapterView = adapter.getBaseView();
    FrameLayout overlayView = (FrameLayout) adapter.getView();
    adapter.unbindModel();

    assertThat(adapter.getView()).isSameAs(adapterView);
    assertThat(adapter.getView()).isSameAs(adapter.getBaseView());
    assertThat(overlayView.getChildCount()).isEqualTo(0);
  }

  @Test
  public void testUnbindModel_unsetsActions() {
    Element elementWithOverlaysAndActions =
        Element.newBuilder()
            .addOverlayElements(
                ElementList.newBuilder()
                    .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance())))
            .setActions(Actions.newBuilder().setOnClick(Action.getDefaultInstance()))
            .build();

    adapter.bindModel(elementWithOverlaysAndActions, elementWithOverlaysAndActions, frameContext);
    View adapterView = adapter.getBaseView();
    View overlayView = adapter.getView();

    adapter.unbindModel();

    assertThat(adapterView.hasOnClickListeners()).isFalse();
    assertThat(overlayView.hasOnClickListeners()).isFalse();
  }

  // Dummy implementation
  static class TestElementAdapter extends ElementAdapter<View, Object> {
    static final String DEFAULT_MODEL = "MODEL";

    boolean bound = false;

    TestElementAdapter(Context context, AdapterParameters parameters) {
      super(context, parameters);
    }

    @Override
    protected Object getModelFromElement(Element baseElement) {
      return DEFAULT_MODEL;
    }

    @Override
    protected void onBindModel(Object model, Element baseElement, FrameContext frameContext) {
      bound = true;
    }

    @Override
    protected void onUnbindModel() {
      bound = false;
    }

    public void setDims(int width, int height) {
      widthPx = width;
      heightPx = height;
    }
  }
}
