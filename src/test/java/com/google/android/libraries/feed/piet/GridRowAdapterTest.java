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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.GridRowAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.Producers;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.BindingRefsProto.ElementListBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.GridCellWidthBindingRef;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.GridCell;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link GridRowAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class GridRowAdapterTest {

  private static final String GRID_STYLE_ID = "cybercat";
  private static final StyleIdsStack GRID_STYLES =
      StyleIdsStack.newBuilder().addStyleIds(GRID_STYLE_ID).build();
  private static final ElementList DEFAULT_CONTENTS =
      ElementList.newBuilder()
          .addElements(Element.newBuilder().setSpacerElement(SpacerElement.getDefaultInstance()))
          .build();

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

    adapterParameters = new AdapterParameters(context, Producers.of(null), actionHandler);
    adapter = new KeySupplier().getAdapter(context, adapterParameters);

    when(frameContext.makeStyleFor(StyleIdsStack.getDefaultInstance()))
        .thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContext.makeStyleFor(GRID_STYLES)).thenReturn(styleProvider);
    when(frameContext.bindNewStyle(any(StyleIdsStack.class))).thenReturn(frameContext);
    when(frameContext.getCurrentStyle()).thenReturn(DEFAULT_STYLE_PROVIDER);
  }

  @Test
  public void testOnBindModel_makesRow() {
    // check that when we have n elements, we have n views
    ElementListBindingRef listBinding =
        ElementListBindingRef.newBuilder().setBindingId("meow").build();
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setElementList(DEFAULT_CONTENTS))
            .addCells(GridCell.newBuilder().setElementListBinding(listBinding))
            .build();

    when(frameContext.getElementListFromBinding(listBinding)).thenReturn(DEFAULT_CONTENTS);

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(2);
    assertThat(adapter.childAdapters).hasSize(2);
  }

  @Test
  public void testOnBindModel_missingBindingIgnoresCell() {
    ElementListBindingRef listBinding =
        ElementListBindingRef.newBuilder().setBindingId("meow").build();
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setElementList(DEFAULT_CONTENTS))
            .addCells(GridCell.newBuilder().setElementListBinding(listBinding))
            .build();

    when(frameContext.getElementListFromBinding(listBinding)).thenReturn(null);

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.childAdapters).hasSize(1);
  }

  @Test
  public void testOnBindModel_setsGridRowStyles() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setElementList(DEFAULT_CONTENTS))
            .setStyleReferences(GRID_STYLES)
            .build();

    adapter.bindModel(gridRow, frameContext);

    verify(styleProvider).setElementStyles(context, frameContext, adapter.getBaseView());
  }

  @Test
  public void testOnBindModel_setsLayoutParamsOnCell_widthDp() {
    int widthDp = 123;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setElementList(DEFAULT_CONTENTS)
                    .setWidth(GridCellWidth.newBuilder().setDp(widthDp)))
            .build();

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.width).isEqualTo((int) ViewUtils.dpToPx(widthDp, context));
  }

  @Test
  public void testOnBindModel_setsLayoutParamsOnCell_widthWeight() {
    int widthWeight = 321;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setElementList(DEFAULT_CONTENTS)
                    .setWidth(GridCellWidth.newBuilder().setWeight(widthWeight)))
            .build();

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.weight).isEqualTo((float) widthWeight);
    assertThat(params.width).isEqualTo(0);
  }

  @Test
  public void testOnBindModel_setsLayoutParamsOnCell_widthBinding() {
    GridCellWidthBindingRef widthBindingRef =
        GridCellWidthBindingRef.newBuilder().setBindingId("fatcat").build();
    int widthDp = 222;
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setElementList(DEFAULT_CONTENTS)
                    .setWidthBinding(widthBindingRef))
            .build();
    when(frameContext.getGridCellWidthFromBinding(widthBindingRef))
        .thenReturn(GridCellWidth.newBuilder().setDp(widthDp).build());

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.width).isEqualTo((int) ViewUtils.dpToPx(widthDp, context));
  }

  @Test
  public void testOnBindModel_setsLayoutParamsOnCell_verticalGravity() {
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(
                GridCell.newBuilder()
                    .setElementList(
                        DEFAULT_CONTENTS
                            .toBuilder()
                            .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)))
            .build();

    adapter.bindModel(gridRow, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0).getLayoutParams())
        .isInstanceOf(LinearLayout.LayoutParams.class);
    LayoutParams params =
        (LinearLayout.LayoutParams) adapter.getBaseView().getChildAt(0).getLayoutParams();
    assertThat(params.gravity).isEqualTo(Gravity.CENTER_VERTICAL);
  }

  @Test
  public void testGetStyleIdsStack() {
    adapter.bindModel(GridRow.newBuilder().setStyleReferences(GRID_STYLES).build(), frameContext);
    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(GRID_STYLES);
  }

  @Test
  public void testCreateViewGroup() {
    LinearLayout gridView = adapter.createViewGroup();
    assertThat(gridView.getOrientation()).isEqualTo(LinearLayout.HORIZONTAL);
    assertThat(gridView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(gridView.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testUnbindModel() {
    ElementListBindingRef listBinding =
        ElementListBindingRef.newBuilder().setBindingId("meow").build();
    GridRow gridRow =
        GridRow.newBuilder()
            .addCells(GridCell.newBuilder().setElementList(DEFAULT_CONTENTS))
            .addCells(GridCell.newBuilder().setElementListBinding(listBinding))
            .build();

    when(frameContext.getElementListFromBinding(listBinding)).thenReturn(DEFAULT_CONTENTS);

    adapter.bindModel(gridRow, frameContext);

    adapter.unbindModel();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
    assertThat(adapter.childAdapters).isEmpty();
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
        .contains("Missing GridRow");

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
        .contains("Missing GridRow");
  }
}
