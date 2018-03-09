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
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.host.common.Callback;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import javax.annotation.Nullable;

/** An {@link ElementAdapter} for {@code ImageElement} elements. */
public class ImageElementAdapter extends ElementAdapter<FrameLayout, ImageElement> {
  private static final String TAG = "ImageElementAdapter";

  private ImageView imageView = null;
  @Nullable private LoadImageCallback currentlyLoadingImage = null;

  private ImageElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected ImageElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasImageElement()) {
      throw new IllegalArgumentException(
          String.format("Missing ImageElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getImageElement();
  }

  @Override
  public void onCreateAdapter(ImageElement model, Element baseElement, FrameContext frameContext) {
    initialBind();

    StyleProvider style = getElementStyle();

    FrameLayout.LayoutParams layoutParams =
        ((FrameLayout.LayoutParams) imageView.getLayoutParams());
    if (style.hasWidth() || style.hasHeight()) {
      Context context = getContext();

      int width;
      if (style.hasWidth()) {
        width = (int) ViewUtils.dpToPx(style.getWidth(), context);
        widthPx = width + getPaddingWidthPx(context, style);
      } else {
        width = LayoutParams.MATCH_PARENT;
        widthPx = DIMENSION_NOT_SET;
      }

      int height;
      if (style.hasHeight()) {
        height = (int) ViewUtils.dpToPx(style.getHeight(), context);
        heightPx = height + getPaddingHeightPx(context, style);
      } else {
        // Defaults to a square when only the width is defined.
        height = width;
        heightPx =
            style.hasWidth() ? width + getPaddingHeightPx(context, style) : DIMENSION_NOT_SET;
      }

      layoutParams.width = width;
      layoutParams.height = height;
    } else {
      layoutParams.width = LayoutParams.MATCH_PARENT;
      layoutParams.height = LayoutParams.WRAP_CONTENT;
      widthPx = DIMENSION_NOT_SET;
      heightPx = DIMENSION_NOT_SET;
    }
    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;

    style.setElementStyles(getContext(), getFrameContext(), getBaseView());
  }

  @Override
  public void onBindModel(ImageElement model, Element baseElement, FrameContext frameContext) {
    Image image;
    switch (model.getContentCase()) {
      case IMAGE:
        image = model.getImage();
        break;
      case IMAGE_BINDING:
        image = frameContext.getImageFromBinding(model.getImageBinding());
        if (image == null) {
          throw new IllegalStateException(
              String.format("Image binding not found for %s", model.getImageBinding()));
        }
        break;
      default:
        throw new IllegalStateException(
            String.format(
                "Unsupported or missing content in ImageElement: %s", model.getContentCase()));
    }
    checkState(currentlyLoadingImage == null, "An image loading callback exists; unbind first");
    currentlyLoadingImage = new LoadImageCallback(imageView);
    getFrameContext().getAssetProvider().getImage(image, currentlyLoadingImage);

    if (model.getStyleReferences().hasStyleBinding()) {
      getElementStyle().setElementStyles(getContext(), getFrameContext(), getBaseView());
    }
  }

  private void initialBind() {
    if (getBaseView() != null) {
      return;
    }
    FrameLayout frameLayout = new FrameLayout(getContext());
    initializeAdapter(frameLayout, KeySupplier.SINGLETON_KEY);
    initializeImageView();
  }

  // TODO: After the AssetProvider returns Drawable instead of ImageView, make the base
  // view be ImageView and get rid of the FrameLayout.
  private void initializeImageView() {
    imageView = new ImageView(getContext());
    FrameLayout frameLayout = getBaseView();
    if (frameLayout != null) {
      frameLayout.addView(imageView);
    }
  }

  @Override
  protected void onUnbindModel() {
    if (currentlyLoadingImage != null) {
      currentlyLoadingImage.cancelled = true;
      currentlyLoadingImage = null;
    }
    if (imageView != null) {
      imageView.setImageDrawable(null);
    }
    widthPx = DIMENSION_NOT_SET;
    heightPx = DIMENSION_NOT_SET;
  }

  @Override
  protected void onReleaseAdapter() {}

  @Override
  protected StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  private int getPaddingWidthPx(Context context, StyleProvider style) {
    return (int)
        ViewUtils.dpToPx(style.getPadding().getStart() + style.getPadding().getEnd(), context);
  }

  private int getPaddingHeightPx(Context context, StyleProvider style) {
    return (int)
        ViewUtils.dpToPx(style.getPadding().getTop() + style.getPadding().getBottom(), context);
  }

  private static class LoadImageCallback implements Callback<Drawable> {
    private final ImageView imageView;
    boolean cancelled = false;

    LoadImageCallback(ImageView imageView) {
      this.imageView = imageView;
    }

    @Override
    public void handleResult(Drawable drawable) {
      if (!cancelled) {
        imageView.setImageDrawable(drawable);
        imageView.setScaleType(ScaleType.CENTER_CROP);
      }
    }
  }

  static class KeySupplier extends SingletonKeySupplier<ImageElementAdapter, ImageElement> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public ImageElementAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new ImageElementAdapter(context, parameters);
    }
  }
}
