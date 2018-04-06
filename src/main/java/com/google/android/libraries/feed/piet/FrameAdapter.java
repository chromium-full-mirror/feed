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
import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.FrameContext.ErrorType;
import com.google.search.now.ui.piet.ElementsProto.Slice;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Frame;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An adapter which manages {@link Frame} instances. Frames will contain one or more slices. This
 * class has additional public methods to support host access to the primary view of the frame
 * before the model is bound to the frame. A frame is basically a vertical LinearLayout of slice
 * Views which are created by {@link ElementAdapter}. This Adapter is not created through a Factory
 * and is managed by the host.
 */
public class FrameAdapter {

  private static final String TAG = "FrameAdapter";

  private final FrameModelBinder frameModelBinder;

  private final Set<ElementAdapter<?, ?>> childAdapters;

  private final Context context;
  private final AdapterParameters parameters;
  @Nullable private LinearLayout view = null;

  public FrameAdapter(
      Context context, AdapterParameters parameters, FrameModelBinder frameModelBinder) {
    this.context = context;
    this.parameters = parameters;
    this.frameModelBinder = frameModelBinder;
    childAdapters = new HashSet<>();
  }

  /**
   * This version of bind will support the {@link ShardingControl}. Sharding allows only part of the
   * frame to be rendered. When sharding is used, a frame is one or more LinearLayout containing a
   * subset of the full set of slices defined for the frame.
   */
  // TODO: Need to implement support for sharding
  public void bindModel(Frame frame, @Nullable ShardingControl shardingControl) {
    long startTime = System.nanoTime();
    FrameContext frameContext = frameModelBinder.bindFrame(frame);
    initialBind(parameters.parentViewSupplier.get());
    LinearLayout frameView = checkNotNull(view);

    try {
      for (Slice slice : frame.getSlicesList()) {
        // For Slice we will create the lower level slice instead to remove the extra
        // level.
        ElementAdapter<?, ?> adapter = getBoundAdapterForSlice(slice, frameContext);
        if (adapter == null) {
          continue;
        }
        childAdapters.add(adapter);
        setLayoutParamsOnChild(adapter);
        frameView.addView(adapter.getView());
      }

      StyleProvider style = frameContext.getCurrentStyle();
      Drawable bg = frameContext.createBackground(style, context);
      frameView.setBackground(bg);
    } catch (RuntimeException e) {
      // TODO: Remove this once error reporting is fully implemented.
      Logger.e(TAG, e, "Catch top level exception");
      frameContext.reportError(ErrorType.ERROR, "Top Level Exception was caught - see logcat");
    }
    startTime = System.nanoTime() - startTime;
    // TODO: We should be targeting < 15ms and warn at 10ms?
    //   Until we get a chance to do the performance tuning, leave this at 30ms to prevent
    //   warnings on large GridRows based frames.
    if (startTime / 1000000 > 30) {
      Logger.w(
          TAG,
          frameContext.reportError(
              ErrorType.WARNING,
              String.format("Slow Bind (%s) time: %s ps", frame.getTag(), startTime / 1000)));
    }
    // If there were errors add an error slice to the frame
    View v = frameContext.getErrorView(context);
    if (v != null) {
      frameView.addView(v);
    }
  }

  public void unbindModel() {
    LinearLayout view = checkNotNull(this.view);
    for (ElementAdapter<?, ?> child : childAdapters) {
      parameters.elementAdapterFactory.releaseAdapter(child);
    }
    childAdapters.clear();
    view.removeAllViews();
  }

  private void setLayoutParamsOnChild(ElementAdapter<?, ?> childAdapter) {
    int width = childAdapter.getComputedWidthPx();
    width = width == ElementAdapter.DIMENSION_NOT_SET ? LayoutParams.MATCH_PARENT : width;
    int height = childAdapter.getComputedHeightPx();
    height = height == ElementAdapter.DIMENSION_NOT_SET ? LayoutParams.WRAP_CONTENT : height;

    childAdapter.setLayoutParams(new FrameLayout.LayoutParams(width, height));
  }

  /**
   * Return the LinearLayout managed by this FrameAdapter. This method can be used to gain access to
   * this view before {@code bindModel} is called.
   */
  public LinearLayout getFrameContainer() {
    initialBind(parameters.parentViewSupplier.get());
    return checkNotNull(view);
  }

  @VisibleForTesting
  @Nullable
  ElementAdapter<?, ?> getBoundAdapterForSlice(Slice slice, FrameContext frameContext) {
    switch (slice.getSliceInstanceCase()) {
      case INLINE_SLICE:
        ElementListAdapter inlineSliceAdapter =
            parameters.elementAdapterFactory.createElementListAdapter(
                slice.getInlineSlice(), frameContext);
        inlineSliceAdapter.createAdapter(slice.getInlineSlice(), frameContext);
        inlineSliceAdapter.bindModel(slice.getInlineSlice(), frameContext);
        return inlineSliceAdapter;
      case TEMPLATE_SLICE:
        TemplateInvocationAdapter templateAdapter =
            parameters.elementAdapterFactory.createTemplateAdapter(
                slice.getTemplateSlice(), frameContext);
        templateAdapter.createAdapter(slice.getTemplateSlice(), frameContext);
        templateAdapter.bindModel(slice.getTemplateSlice(), frameContext);
        return templateAdapter;
      default:
        Logger.wtf(TAG, "Unsupported Slice type");
        return null;
    }
  }

  @VisibleForTesting
  AdapterParameters getParameters() {
    return this.parameters;
  }

  @VisibleForTesting
  @Nullable
  LinearLayout getView() {
    return this.view;
  }

  private void initialBind(@Nullable ViewGroup parent) {
    if (view != null) {
      return;
    }
    this.view = createView(parent);
  }

  private LinearLayout createView(@Nullable ViewGroup parent) {
    LinearLayout linearLayout = new LinearLayout(context);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    ViewGroup.LayoutParams layoutParams;
    if (parent != null && parent.getLayoutParams() != null) {
      layoutParams = new LinearLayout.LayoutParams(parent.getLayoutParams());
      layoutParams.width = LayoutParams.MATCH_PARENT;
      layoutParams.height = LayoutParams.WRAP_CONTENT;
    } else {
      layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }
    linearLayout.setLayoutParams(layoutParams);
    return linearLayout;
  }
}
