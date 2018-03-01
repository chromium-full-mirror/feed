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

import static com.google.android.libraries.feed.common.Validators.checkNotNull;
import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.FrameContext.ErrorType;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridCell;
import com.google.search.now.ui.piet.ElementsProto.GridCellWidth;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import javax.annotation.Nullable;

/** A {@link BoundViewAdapter} which manages {@code GridLine} slices. */
public class GridRowAdapter
    extends ViewGroupBoundViewAdapter<LinearLayout, ElementListAdapter, GridRow> {

  private static final String TAG = "GridRowAdapter";

  private GridRowAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected GridRow getModelFromElement(Element baseElement) {
    if (!baseElement.hasGridRow()) {
      throw new IllegalArgumentException(
          String.format("Missing GridRow; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getGridRow();
  }

  @Override
  public void onCreateAdapter(GridRow gridRow, Element baseElement, FrameContext frameContext) {
    initialCreate();

    // Set the padding on the LinearLayout representing the grid line.
    ViewGroup viewGroup = checkNotNull(getBaseView());
    frameContext
        .makeStyleFor(gridRow.getStyleReferences())
        .setElementStyles(getContext(), frameContext, viewGroup);

    // Populate the LinearLayout with cells.
    for (GridCell cell : gridRow.getCellsList()) {
      ElementListAdapter cellAdapter = getCellAdapter(cell, frameContext);
      if (cellAdapter != null) {
        viewGroup.addView(cellAdapter.getView());
        setLayoutParamsOnCell(cellAdapter, cell, frameContext);
      }
    }
  }

  @Override
  public void onBindModel(GridRow gridRow, Element baseElement, FrameContext frameContext) {
    // Populate the LinearLayout with cells.
    // TODO: When onCreateAdapter skips creating some adapters (ex. due to binding
    // issues),
    // this check will fail; if we support silently ignoring some elements in onCreateAdapter, we
    // should also support it here (or it should be an error both places).
    checkState(
        gridRow.getCellsCount() == childAdapters.size(),
        "Number of cells mismatch: got %s cells, had %s adapters",
        gridRow.getCellsCount(),
        childAdapters.size());
    for (int i = 0; i < gridRow.getCellsCount(); i++) {
      childAdapters
          .get(i)
          .bindModel(getElementListFromCell(gridRow.getCells(i), frameContext), frameContext);
    }

    // Reapply styles if we have style bindings.
    if (gridRow.getStyleReferences().hasStyleBinding()) {
      frameContext
          .makeStyleFor(gridRow.getStyleReferences())
          .setElementStyles(getContext(), frameContext, getBaseView());
    }
  }

  @Override
  protected StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  private void setLayoutParamsOnCell(
      ElementListAdapter cellAdapter, GridCell cell, FrameContext frameContext) {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    GridCellWidth gridCellWidth = null;
    if (cell.hasWidth()) {
      gridCellWidth = cell.getWidth();
    } else if (cell.hasWidthBinding()) {
      gridCellWidth = frameContext.getGridCellWidthFromBinding(cell.getWidthBinding());
    }

    if (gridCellWidth != null) {
      if (gridCellWidth.hasWeight()) {
        params.weight = gridCellWidth.getWeight();
        params.width = 0;
      } else if (gridCellWidth.hasDp()) {
        params.width = (int) ViewUtils.dpToPx(gridCellWidth.getDp(), getContext());
      }
    }

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

  @Nullable
  private ElementListAdapter getCellAdapter(GridCell cell, FrameContext frameContext) {
    ElementListAdapter adapter;
    if (cell.hasElementList()) {
      // TODO: get adapter from childAdapters if it exists.
      adapter = getAdapterForChildList(getElementListFromCell(cell, frameContext), frameContext);
    } else if (cell.hasElementListBinding()) {
      adapter = getAdapterFromBinding(cell, frameContext);
    } else {
      throw new IllegalArgumentException(
          String.format("Cell has no recognized content; got %s", cell.getCellContentCase()));
    }
    return adapter;
  }

  @Nullable
  private ElementListAdapter getAdapterFromBinding(GridCell cell, FrameContext frameContext) {
    ElementList list = getElementListFromCell(cell, frameContext);

    if (list == null) {
      Logger.e(
          TAG,
          frameContext.reportError(
              ErrorType.ERROR,
              String.format("Binding not found for Binding [%s]", cell.getElementListBinding())));
      return null;
    }

    return getAdapterForChildList(list, frameContext);
  }

  @Nullable
  private ElementList getElementListFromCell(GridCell cell, FrameContext frameContext) {
    switch (cell.getCellContentCase()) {
      case ELEMENT_LIST:
        return cell.getElementList();
      case ELEMENT_LIST_BINDING:
        return frameContext.getElementListFromBinding(cell.getElementListBinding());
      default:
        throw new IllegalArgumentException(
            String.format("Cell has no recognized content; has %s", cell.getCellContentCase()));
    }
  }

  /** Create an Adapter for the {@code ElementList}. */
  private ElementListAdapter getAdapterForChildList(ElementList list, FrameContext frameContext) {

    // Most GridCells will only contain a single element in their list. We could consider an
    // optimization where we omit the list and just insert the single element's view. But this has
    // a number of complexities with backgrounds, paddings, and debugging the layout in general, so
    // we omit this behavior for now.

    ElementListAdapter listAdapter =
        getParameters().adapterFactory.createElementListAdapter(list, frameContext);
    addChildAdapter(listAdapter);
    return listAdapter;
  }

  @Override
  protected LinearLayout createViewGroup() {
    LinearLayout viewGroup = new LinearLayout(getContext());
    viewGroup.setOrientation(LinearLayout.HORIZONTAL);
    viewGroup.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
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
