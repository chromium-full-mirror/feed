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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.piet.AdapterFactory.AdapterKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/** A {@link BoundViewAdapter} which manages {@code SpacerElement} slices. */
public class SpacerElementAdapter
    extends ElementAdapter<View, SpacerElement, SpacerElementAdapter.Key> {
  private static final String TAG = "SpacerElementAdapter";
  private static final SpacerElementAdapter.Key SINGLETON_KEY = new SpacerElementAdapter.Key();

  @VisibleForTesting
  SpacerElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected SpacerElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasSpacerElement()) {
      throw new IllegalArgumentException(
          String.format("Missing SpacerElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getSpacerElement();
  }

  @Override
  public void onBindModel(SpacerElement model, Element baseElement, FrameContext frameContext) {
    initialBind(frameContext, model);
    View view = checkNotNull(getBaseView());

    StyleProvider spacerStyle = frameContext.makeStyleFor(model.getStyleReferences());
    EdgeWidths padding = spacerStyle.getPadding();

    int height = padding.getTop() + padding.getBottom() + model.getHeight();
    height = (int) ViewUtils.dpToPx(height, getContext());

    LayoutParams params = view.getLayoutParams();
    if (params != null) {
      params.height = height;
    } else {
      params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
    }
    view.setLayoutParams(params);
    spacerStyle.setElementStyles(getContext(), frameContext, view);
  }

  @Override
  protected StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  /**
   * This is a subclass of View which can draw a spacer within the View. Consider it an empty view
   * which can have a height & Styles. This can be used to create empty areas, dividers, horizontal
   * rules & blocks of color.
   */
  private static class DividerView extends View {
    private final ColorDrawable spacerDrawable;
    private final int heightInPx;
    private final int topPaddingInPx;

    public DividerView(Context context, int color, int height, EdgeWidths padding) {
      super(context);
      spacerDrawable = new ColorDrawable(color);
      heightInPx = (int) ViewUtils.dpToPx(height, context);
      this.topPaddingInPx = (int) ViewUtils.dpToPx(padding.getTop(), context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      if (heightInPx > 0) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int measuredWidth = getMeasuredWidth();
        spacerDrawable.setBounds(
            paddingLeft, topPaddingInPx, measuredWidth - paddingRight, topPaddingInPx + heightInPx);
        spacerDrawable.draw(canvas);
      }
    }
  }

  private void initialBind(FrameContext frameContext, SpacerElement model) {
    if (getBaseView() != null) {
      return;
    }
    StyleProvider styleProvider = frameContext.makeStyleFor(model.getStyleReferences());
    View v =
        new DividerView(
            getContext(), styleProvider.getColor(), model.getHeight(), styleProvider.getPadding());
    initializeAdapter(v, SINGLETON_KEY);
  }

  static class KeySupplier
      implements AdapterKeySupplier<SpacerElementAdapter, SpacerElement, SpacerElementAdapter.Key> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public SpacerElementAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new SpacerElementAdapter(context, parameters);
    }

    @Override
    public SpacerElementAdapter.Key getKey(FrameContext frameContext, SpacerElement model) {
      return SINGLETON_KEY;
    }

    @Override
    public boolean isSingletonKey() {
      return false;
    }
  }

  static class Key {}
}
