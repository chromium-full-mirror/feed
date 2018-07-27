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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.DebugLogger.MessageType;
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

  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  private void setLayoutParamsOnCell(
      ElementAdapter<? extends View, ?> cellAdapter, GridCell cell, FrameContext frameContext) {
    // TODO: Support full-cell backgrounds and horizontal/vertical gravity for all
    // types of ElementAdapter in GridCell.

    // Default cell is weight = 1 and height = WRAP_CONTENT.
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
    params.weight = 1;

    GridCellWidth gridCellWidth = null;
    if (cell.hasWidth()) {
      gridCellWidth = cell.getWidth();
    } else if (cell.hasWidthBinding()) {
      gridCellWidth = frameContext.getGridCellWidthFromBinding(cell.getWidthBinding());
    }

    // If a width is specified on the proto, we expect it to be populated so we can use it.
    if (gridCellWidth != null) {
      switch (gridCellWidth.getWidthSpecCase()) {
        case CONTENT_WIDTH:
          switch (gridCellWidth.getContentWidth()) {
            case CONTENT_WIDTH:
              if (cellAdapter.getComputedWidthPx() != DIMENSION_NOT_SET) {
                params.width = cellAdapter.getComputedWidthPx();
              } else {
                params.width = LayoutParams.WRAP_CONTENT;
              }
              params.weight = 0;
              break;
            case INVALID_CONTENT_WIDTH:
            default:
              frameContext.reportError(
                  MessageType.WARNING,
                  String.format("Invalid content width: %s", gridCellWidth.getContentWidth()));
          }
          break;
        case DP:
          params.weight = 0;
          params.width = (int) LayoutUtils.dpToPx(gridCellWidth.getDp(), getContext());
          break;
        case WEIGHT:
          params.weight = gridCellWidth.getWeight();
          params.width = 0;
          break;
        case WIDTHSPEC_NOT_SET:
        default:
          frameContext.reportError(
              MessageType.WARNING,
              String.format("Invalid content width: %s", gridCellWidth.getContentWidth()));
      }
    }

    int height = cellAdapter.getComputedHeightPx();
    if (height != ElementAdapter.DIMENSION_NOT_SET) {
      params.height = height;
    }

    cellAdapter.getElementStyle().applyMargins(getContext(), params);

    params.gravity = ViewUtils.gravityVerticalToGravity(cellAdapter.getVerticalGravity());

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
