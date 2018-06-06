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

import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.GridCell;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.ArrayList;
import java.util.List;

/** An {@link ElementContainerAdapter} which manages {@code GridRow} slices. */
class GridRowAdapter extends ElementContainerAdapter<LinearLayout, GridRow> {

  private static final String TAG = "GridRowAdapter";

  private GridRowAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context), KeySupplier.SINGLETON_KEY);
  }

  @Override
  GridRow getModelFromElement(Element baseElement) {
    if (!baseElement.hasGridRow()) {
      throw new IllegalArgumentException(
          String.format("Missing GridRow; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getGridRow();
  }

  @Override
  void onCreateAdapter(GridRow gridRow, Element baseElement, FrameContext frameContext) {
    super.onCreateAdapter(gridRow, baseElement, frameContext);

    int adapterIndex = 0;
    for (int contentIndex = 0; contentIndex < gridRow.getCellsCount(); contentIndex++) {
      GridCell cell = gridRow.getCells(contentIndex);
      for (int i = 0; i < adaptersPerContent[contentIndex]; i++) {
        setLayoutParamsOnCell(childAdapters.get(adapterIndex), cell, frameContext);
        adapterIndex++;
      }
    }
  }

  @Override
  List<Content> getContentsFromModel(GridRow model) {
    List<Content> contents = new ArrayList<>();
    for (GridCell cell : model.getCellsList()) {
      contents.add(cell.getContent());
    }
    return contents;
  }

  @Override
  void onBindModel(GridRow gridRow, Element baseElement, FrameContext frameContext) {
    super.onBindModel(gridRow, baseElement, frameContext);

    int adapterIndex = 0;
    checkState(
        gridRow.getCellsCount() == adaptersPerContent.length,
        "Mismatch between number of cells (%s) and adaptersPerContent (%s); problem in creation?",
        gridRow.getCellsCount(),
        adaptersPerContent.length);
    for (int contentIndex = 0; contentIndex < gridRow.getCellsCount(); contentIndex++) {
      GridCell cell = gridRow.getCells(contentIndex);
      switch (cell.getContent().getContentTypeCase()) {
        case BOUND_ELEMENT:
        case TEMPLATE_BINDING:
          for (int i = 0; i < adaptersPerContent[contentIndex]; i++) {
            setLayoutParamsOnCell(childAdapters.get(adapterIndex), cell, frameContext);
            adapterIndex++;
          }
          break;
        default:
          adapterIndex += adaptersPerContent[contentIndex];
      }
    }
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  private void setLayoutParamsOnCell(
      ElementAdapter<? extends View, ?> cellAdapter, GridCell cell, FrameContext frameContext) {
    // TODO: Support full-cell backgrounds and horizontal/vertical gravity for all
    // types of ElementAdapter in GridCell.

    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    GridCellWidth gridCellWidth = null;
    if (cell.hasWidth()) {
      gridCellWidth = cell.getWidth();
    } else if (cell.hasWidthBinding()) {
      gridCellWidth = frameContext.getGridCellWidthFromBinding(cell.getWidthBinding());
    }

    // If we have a requested width, use it; otherwise, WRAP_CONTENT.
    if (gridCellWidth != null) {
      if (gridCellWidth.hasWeight()) {
        params.weight = gridCellWidth.getWeight();
        params.width = 0;
      } else if (gridCellWidth.hasDp()) {
        params.width = (int) ViewUtils.dpToPx(gridCellWidth.getDp(), getContext());
      }
    } else if (cellAdapter.getComputedWidthPx() != DIMENSION_NOT_SET) {
      params.width = cellAdapter.getComputedWidthPx();
    }

    int height = cellAdapter.getComputedHeightPx();
    params.height = height == ElementAdapter.DIMENSION_NOT_SET ? params.height : height;

    cellAdapter.getElementStyle().setMargins(getContext(), params);

    // TODO: Switch this to use the ViewUtils method in the next CL.
    switch (cellAdapter.getVerticalGravity()) {
      case GRAVITY_BOTTOM:
        params.gravity = Gravity.BOTTOM;
        break;
      case GRAVITY_MIDDLE:
        params.gravity = Gravity.CENTER_VERTICAL;
        break;
      case GRAVITY_TOP:
        params.gravity = Gravity.TOP;
        break;
      default:
        params.gravity = Gravity.NO_GRAVITY;
    }

    cellAdapter.setLayoutParams(params);
  }


  @VisibleForTesting
  static LinearLayout createView(Context context) {
    LinearLayout viewGroup = new LinearLayout(context);
    viewGroup.setOrientation(LinearLayout.HORIZONTAL);
    viewGroup.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    viewGroup.setBaselineAligned(false);
    return viewGroup;
  }

  /** A {@link AdapterKeySupplier} for the {@link GridRowAdapter}. */
  static class KeySupplier extends SingletonKeySupplier<GridRowAdapter, GridRow> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public GridRowAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new GridRowAdapter(context, parameters);
    }
  }
}
