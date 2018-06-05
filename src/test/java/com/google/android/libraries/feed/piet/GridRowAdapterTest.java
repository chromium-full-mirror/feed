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
import static com.google.android.libraries.feed.piet.StyleProvider.DEFAULT_STYLE_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.piet.GridRowAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.GridCellWidthBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.BindingValue.Visibility;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.GridCell;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link GridRowAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class GridRowAdapterTest {

  private static final String GRID_STYLE_ID = "cybercat";
  private static final StyleIdsStack GRID_STYLES =
      StyleIdsStack.newBuilder().addStyleIds(GRID_STYLE_ID).build();
  private static final Element DEFAULT_ELEMENT =
      Element.newBuilder().setSpacerElement(SpacerElement.getDefaultInstance()).build();
  private static final Content DEFAULT_CONTENT =
      Content.newBuilder().setElement(DEFAULT_ELEMENT).build();
  private static final String BINDING_ID = "stripes";
  private static final ElementBindingRef ELEMENT_BINDING =
      ElementBindingRef.newBuilder().setBindingId(BINDING_ID).build();
  private static final Content BOUND_CONTENTS =
      Content.newBuilder().setBoundElement(ELEMENT_BINDING).build();
  private static final GridRow GRID_ROW_WITH_BOUND_CELL =
      GridRow.newBuilder().addCells(GridCell.newBuilder().setContent(BOUND_CONTENTS)).build();

  private Context context;
  private AdapterParameters adapterParameters;

  @Mock private ActionHandler actionHandler;
  @Mock private FrameContext frameContext;
  @Mock private StyleProvider styleProvider;

  private GridRowAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);

    adapterParameters = new AdapterParameters(context, Suppliers.of(null));
    adapter = new KeySupplier().getAdapter(context, adapterParameters);

    when(frameContext.makeStyleFor(StyleIdsStack.getDefaultInstance()))
        .thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.makeStyleFor(GRID_STYLES)).thenReturn(styleProvider);
    when(frameContext.bindNewStyle(any(StyleIdsStack.class))).thenReturn(frameContext);
    when(frameContext.getCurrentStyle()).thenReturn(styleProvider);
    when(frameContext.getActionHandler()).thenReturn(actionHandler);
    when(styleProvider.getPadding()).thenReturn(EdgeWidths.getDefaultInstance());
  }

  @Test
  public void testOnCreateAdapter_makesRow() {
    // We create an adapter for the inline content, but not for the bound content.
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .addCells(GridCell.newBuilder().setContent(BOUND_CONTENTS))
            .build();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getBaseline()).isEqualTo(-1);
    assertThat(adapter.childAdapters).hasSize(1);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
    LayoutParams cellLayoutParams =
        (LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(cellLayoutParams.width).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(cellLayoutParams.height)
        .isEqualTo(adapter.childAdapters.get(0).getComputedHeightPx());
  }

  @Test
  public void testOnCreateAdapter_missingContentIsException() {
    GridRow gridRow = GridRow.newBuilder().addCells(GridCell.getDefaultInstance()).build();

    assertThatRunnable(() -> adapter.createAdapter(gridRow, frameContext))
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Cell has no recognized content");
  }

  @Test
  public void testOnCreateAdapter_setsGridRowStyles() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .setStyleReferences(GRID_STYLES)
            .build();

    adapter.createAdapter(gridRow, frameContext);

    verify(frameContext).bindNewStyle(GRID_STYLES);
    verify(styleProvider).setElementStyles(context, frameContext, adapter.getBaseView());
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_widthDp() {
    int widthDp = 123;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(DEFAULT_CONTENT)
                    .setWidth(GridCellWidth.newBuilder().setDp(widthDp)))
            .build();

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.width).isEqualTo((int) ViewUtils.dpToPx(widthDp, context));
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_widthWeight() {
    int widthWeight = 321;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(DEFAULT_CONTENT)
                    .setWidth(GridCellWidth.newBuilder().setWeight(widthWeight)))
            .build();

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.weight).isEqualTo((float) widthWeight);
    assertThat(params.width).isEqualTo(0);
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_widthBinding() {
    GridCellWidthBindingRef widthBindingRef =
        GridCellWidthBindingRef.newBuilder().setBindingId("fatcat").build();
    int widthDp = 222;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder().setContent(DEFAULT_CONTENT).setWidthBinding(widthBindingRef))
            .build();
    when(frameContext.getGridCellWidthFromBinding(widthBindingRef))
        .thenReturn(GridCellWidth.newBuilder().setDp(widthDp).build());

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.width).isEqualTo((int) ViewUtils.dpToPx(widthDp, context));
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_widthOfChildAdapter() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setImageElement(
                                        ImageElement.newBuilder()
                                            .setImage(Image.getDefaultInstance())))))
            .build();

    when(frameContext.getAssetProvider()).thenReturn(mock(AssetProvider.class));
    when(styleProvider.hasWidth()).thenReturn(true);
    when(styleProvider.getWidth()).thenReturn(456);

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.width).isEqualTo((int) ViewUtils.dpToPx(456, context));
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_heightOfChildAdapter() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setImageElement(
                                        ImageElement.newBuilder()
                                            .setImage(Image.getDefaultInstance())))))
            .build();

    when(frameContext.getAssetProvider()).thenReturn(mock(AssetProvider.class));
    when(styleProvider.hasHeight()).thenReturn(true);
    when(styleProvider.getHeight()).thenReturn(123);

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.height).isEqualTo((int) ViewUtils.dpToPx(123, context));
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_heightOfChildAdapterNotDefined() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setImageElement(
                                        ImageElement.newBuilder()
                                            .setImage(Image.getDefaultInstance())))))
            .build();

    when(frameContext.getAssetProvider()).thenReturn(mock(AssetProvider.class));
    when(styleProvider.hasHeight()).thenReturn(false);
    when(styleProvider.getHeight()).thenReturn(ElementAdapter.DIMENSION_NOT_SET);

    adapter.createAdapter(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_margins() {
    GridRow gridRow =
        GridRow.newBuilder().addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT)).build();

    adapter.createAdapter(gridRow, frameContext);

    verify(styleProvider)
        .setMargins(
            context, (MarginLayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams());
  }

  @Test
  public void testOnCreateAdapter_setsLayoutParamsOnCell_verticalGravityTop() {
    GridRow gridRowTop =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(
                        Content.newBuilder()
                            .setElement(
                                DEFAULT_ELEMENT
                                    .toBuilder()
                                    .setGravityVertical(GravityVertical.GRAVITY_TOP))))
            .build();

    adapter.createAdapter(gridRowTop, frameContext);

    SpacerElementAdapter cellAdapter = (SpacerElementAdapter) adapter.childAdapters.get(0);
    LayoutParams params = (LinearLayout.LayoutParams) cellAdapter.getView().getLayoutParams();
    assertThat(params.gravity).isEqualTo(Gravity.TOP);
  }

  @Test
  public void testOnBindModel_recreatesBindingCells() {
    Element cellWithOneElement =
        Element.newBuilder()
            .setElementList(
                ElementList.newBuilder()
                    .addContents(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setSpacerElement(SpacerElement.getDefaultInstance()))))
            .build();
    Element cellWithTwoElements =
        Element.newBuilder()
            .setElementList(
                ElementList.newBuilder()
                    .addContents(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setSpacerElement(SpacerElement.getDefaultInstance())))
                    .addContents(
                        Content.newBuilder()
                            .setElement(
                                Element.newBuilder()
                                    .setSpacerElement(SpacerElement.getDefaultInstance()))))
            .build();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(cellWithOneElement).build());
    adapter.createAdapter(GRID_ROW_WITH_BOUND_CELL, frameContext);
    // The cell adapter has not been created yet
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(cellWithTwoElements).build());
    adapter.bindModel(GRID_ROW_WITH_BOUND_CELL, frameContext);
    // The cell adapter creates its one view on bind.
    assertThat(((LinearLayout) adapter.getBaseView().getChildAt(0)).getChildCount()).isEqualTo(2);

    adapter.unbindModel();
    // The cell adapter has been released.
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(cellWithOneElement).build());
    adapter.bindModel(GRID_ROW_WITH_BOUND_CELL, frameContext);
    // The cell adapter can bind to a different model.
    assertThat(((LinearLayout) adapter.getBaseView().getChildAt(0)).getChildCount()).isEqualTo(1);
  }

  @Test
  public void testOnBindModel_visibilityGone() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder().setBindingId(BINDING_ID).setElement(DEFAULT_ELEMENT).build());
    adapter.createAdapter(GRID_ROW_WITH_BOUND_CELL, frameContext);
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(BINDING_ID)
                .setVisibility(Visibility.GONE)
                .build());

    adapter.bindModel(GRID_ROW_WITH_BOUND_CELL, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_noContent() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder().setBindingId(BINDING_ID).setElement(DEFAULT_ELEMENT).build());
    adapter.createAdapter(GRID_ROW_WITH_BOUND_CELL, frameContext);
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setBindingId(BINDING_ID).build());

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_optionalAbsent() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder().setBindingId(BINDING_ID).setElement(DEFAULT_ELEMENT).build());
    adapter.createAdapter(GRID_ROW_WITH_BOUND_CELL, frameContext);

    ElementBindingRef optionalBinding = ELEMENT_BINDING.toBuilder().setIsOptional(true).build();
    GridRow optionalBindingRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setContent(Content.newBuilder().setBoundElement(optionalBinding)))
            .build();
    when(frameContext.getElementBindingValue(optionalBinding))
        .thenReturn(BindingValue.newBuilder().setBindingId(BINDING_ID).build());

    adapter.bindModel(optionalBindingRow, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testOnBindModel_setsVisibility() {
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder().setBindingId(BINDING_ID).setElement(DEFAULT_ELEMENT).build());
    adapter.createAdapter(GRID_ROW_WITH_BOUND_CELL, frameContext);
    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(BINDING_ID)
                .setVisibility(Visibility.INVISIBLE)
                .setElement(DEFAULT_ELEMENT)
                .build());

    adapter.bindModel(GRID_ROW_WITH_BOUND_CELL, frameContext);
    assertThat(adapter.getBaseView().getChildAt(0).getVisibility()).isEqualTo(View.INVISIBLE);
  }

  @Test
  public void testOnBindModel_throwsExceptionOnCellCountMismatch() {
    GridRow gridRowWithTwoElements =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .build();

    GridRow gridRowWithOneElement =
        GridRow.newBuilder().addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT)).build();

    adapter.createAdapter(gridRowWithTwoElements, frameContext);

    assertThatRunnable(() -> adapter.bindModel(gridRowWithOneElement, frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Internal error in adapters per content");
  }

  @Test
  public void testOnBindModel_setsStylesOnlyIfBindingIsDefined() {
    GridRow gridRowWithStyle =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .setStyleReferences(GRID_STYLES)
            .build();

    adapter.createAdapter(gridRowWithStyle, frameContext);
    verify(frameContext).bindNewStyle(GRID_STYLES);

    // When we bind a new model, the style does not change.
    StyleIdsStack otherStyles = StyleIdsStack.newBuilder().addStyleIds("ignored").build();
    GridRow gridRowWithOtherStyle =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .setStyleReferences(otherStyles)
            .build();

    adapter.bindModel(gridRowWithOtherStyle, frameContext);
    verify(frameContext, never()).bindNewStyle(otherStyles);

    // If we bind a model that has a style binding, then the style does get re-applied.
    StyleIdsStack styleWithBinding =
        StyleIdsStack.newBuilder()
            .setStyleBinding(StyleBindingRef.newBuilder().setBindingId("homewardbound"))
            .build();
    GridRow gridRowWithBoundStyle =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .setStyleReferences(styleWithBinding)
            .build();
    StyleProvider otherStyleProvider = mock(StyleProvider.class);
    when(frameContext.makeStyleFor(styleWithBinding)).thenReturn(otherStyleProvider);
    when(frameContext.getCurrentStyle()).thenReturn(otherStyleProvider);

    adapter.bindModel(gridRowWithBoundStyle, frameContext);
    verify(frameContext).bindNewStyle(styleWithBinding);
    verify(otherStyleProvider).setElementStyles(context, frameContext, adapter.getBaseView());
  }

  @Test
  public void testUnbindModel() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .addCells(
                GridCell.newBuilder()
                    .setContent(Content.newBuilder().setBoundElement(ELEMENT_BINDING)))
            .build();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createAdapter(gridRow, frameContext);
    adapter.bindModel(gridRow, frameContext);

    adapter.unbindModel();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.childAdapters).hasSize(1);

    // The inline adapter has been unbound.
    assertThat(adapter.childAdapters.get(0).getRawModel()).isNull();
  }

  @Test
  public void testReleaseAdapter() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setContent(DEFAULT_CONTENT))
            .addCells(
                GridCell.newBuilder()
                    .setContent(Content.newBuilder().setBoundElement(ELEMENT_BINDING)))
            .build();

    when(frameContext.getElementBindingValue(ELEMENT_BINDING))
        .thenReturn(BindingValue.newBuilder().setElement(DEFAULT_ELEMENT).build());

    adapter.createAdapter(gridRow, frameContext);
    adapter.bindModel(gridRow, frameContext);

    adapter.unbindModel();
    adapter.releaseAdapter();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
  }

  @Test
  public void testGetStyleIdsStack() {
    adapter.createAdapter(
        GridRow.newBuilder().setStyleReferences(GRID_STYLES).build(), frameContext);
    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(GRID_STYLES);
  }

  @Test
  public void testCreateViewGroup() {
    LinearLayout gridView = GridRowAdapter.createView(context);
    assertThat(gridView.getOrientation()).isEqualTo(LinearLayout.HORIZONTAL);
    assertThat(gridView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(gridView.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testGetModelFromElement() {
    GridRow model =
        GridRow.newBuilder()
            .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds("spacer"))
            .build();

    Element elementWithModel = Element.newBuilder().setGridRow(model).build();
    assertThat(adapter.getModelFromElement(elementWithModel)).isSameAs(model);

    Element elementWithWrongModel =
        Element.newBuilder().setCustomElement(CustomElement.getDefaultInstance()).build();
    assertThatRunnable(() -> adapter.getModelFromElement(elementWithWrongModel))
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Missing GridRow");

    Element emptyElement = Element.getDefaultInstance();
    assertThatRunnable(() -> adapter.getModelFromElement(emptyElement))
        .throwsAnExceptionOfType(IllegalArgumentException.class)
        .that()
        .hasMessageThat()
        .contains("Missing GridRow");
  }
}
