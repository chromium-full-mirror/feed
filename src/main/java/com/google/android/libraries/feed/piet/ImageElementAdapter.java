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
import static com.google.android.libraries.feed.host.imageloader.ImageLoaderApi.DIMENSION_UNKNOWN;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.ui.DrawableScalingHelper;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.Visibility;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/** An {@link ElementAdapter} for {@code ImageElement} elements. */
class ImageElementAdapter extends ElementAdapter<ImageView, ImageElement> {
  private static final String TAG = "ImageElementAdapter";

  /*@Nullable*/ private LoadImageCallback currentlyLoadingImage = null;

  private ImageElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context), KeySupplier.SINGLETON_KEY);
  }

  @Override
  ImageElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasImageElement()) {
      throw new IllegalArgumentException(
          String.format("Missing ImageElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getImageElement();
  }

  @Override
  void onCreateAdapter(ImageElement model, Element baseElement, FrameContext frameContext) {
    StyleProvider style = getElementStyle();

    if (style.hasWidth() || style.hasHeight()) {
      Context context = getContext();

      if (style.hasWidth()) {
        widthPx = (int) LayoutUtils.dpToPx(style.getWidth(), context);
      } else {
        widthPx = DIMENSION_NOT_SET;
      }

      if (style.hasHeight()) {
        heightPx = (int) LayoutUtils.dpToPx(style.getHeight(), context);
      } else {
        // Defaults to a square when only the width is defined.
        heightPx = style.hasWidth() ? widthPx : DIMENSION_NOT_SET;
      }
    } else {
      widthPx = DIMENSION_NOT_SET;
      heightPx = DIMENSION_NOT_SET;
    }
  }

  @Override
  void onBindModel(ImageElement model, Element baseElement, FrameContext frameContext) {
    Image image;
    switch (model.getContentCase()) {
      case IMAGE:
        image = model.getImage();
        break;
      case IMAGE_BINDING:
        BindingValue binding = frameContext.getImageBindingValue(model.getImageBinding());
        if (!binding.hasImage()) {
          if (model.getImageBinding().getIsOptional()) {
            setVisibilityOnView(Visibility.GONE);
            return;
          } else {
            throw new IllegalArgumentException(
                String.format("Image binding %s had no content", binding.getBindingId()));
          }
        }
        image = binding.getImage();
        break;
      default:
        throw new IllegalStateException(
            String.format(
                "Unsupported or missing content in ImageElement: %s", model.getContentCase()));
    }

    if (getElementStyle().hasPreLoadFill()) {
      getBaseView().setImageDrawable(getElementStyle().createPreLoadFill());
    }

    checkState(currentlyLoadingImage == null, "An image loading callback exists; unbind first");
    LoadImageCallback loadImageCallback =
        new LoadImageCallback(getBaseView(), toAndroidScaleType(image.getScaleType()));
    currentlyLoadingImage = loadImageCallback;
    getParameters()
        .hostProviders
        .getAssetProvider()
        .getImage(
            image,
            convertDimensionForImageLoader(widthPx),
            convertDimensionForImageLoader(heightPx),
            loadImageCallback);
  }

  private int convertDimensionForImageLoader(int dimension) {
    if (dimension == DIMENSION_NOT_SET) {
      return DIMENSION_UNKNOWN;
    }

    return dimension;
  }

  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  private static ScaleType toAndroidScaleType(Image.ScaleType pietScaleType) {
    switch (pietScaleType) {
      case CENTER_CROP:
        return ScaleType.CENTER_CROP;
      case CENTER_INSIDE:
      case SCALE_TYPE_UNSPECIFIED:
        return ScaleType.FIT_CENTER;
      default:
        throw new IllegalArgumentException(
            String.format("Unsupported ScaleType: %s", pietScaleType));
    }
  }

  @Override
  void onUnbindModel() {
    if (currentlyLoadingImage != null) {
      currentlyLoadingImage.cancelled = true;
      currentlyLoadingImage = null;
    }
    ImageView imageView = getBaseView();
    if (imageView != null) {
      imageView.setImageDrawable(null);
    }
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  private static ImageView createView(Context context) {
    ImageView imageView = new ImageView(context);
    imageView.setCropToPadding(true);
    return imageView;
  }

  @VisibleForTesting
  class LoadImageCallback implements Consumer<Drawable> {

    private static final int FADE_IN_ANIMATION_TIME_MS = 300;

    private final ImageView imageView;
    private final ScaleType scaleType;
    private final long initialTime;

    boolean cancelled = false;
    /*@Nullable*/ private Drawable finalDrawable;

    LoadImageCallback(ImageView imageView, ScaleType scaleType) {
      this.imageView = imageView;
      this.scaleType = scaleType;
      this.initialTime = getParameters().clock.elapsedRealtime();
    }

    @Override
    public void accept(Drawable drawable) {
      if (cancelled) {
        return;
      }

      // Only perform image rescaling if both width/height are known as parameters.
      if (widthPx > 0 && heightPx > 0) {
        drawable =
            DrawableScalingHelper.maybeScaleDrawable(
                getContext(),
                getParameters().hostProviders.getAssetProvider(),
                drawable,
                widthPx,
                heightPx);
      }
      imageView.setScaleType(scaleType);

      this.finalDrawable = drawable;

      // If we are in the process of binding when we get the image, we should not fade in the
      // image as the image was cached.
      if (!shouldFadeInImage()) {
        imageView.setImageDrawable(drawable);
        // Invalidating the view as the view doesn't update if not manually updated here.
        getView().invalidate();
        return;
      }

      Drawable initialDrawable =
          imageView.getDrawable() != null
              ? imageView.getDrawable()
              : new ColorDrawable(Color.TRANSPARENT);

      Drawable[] layers = {initialDrawable, drawable};
      TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
      imageView.setImageDrawable(transitionDrawable);
      transitionDrawable.setCrossFadeEnabled(true);
      transitionDrawable.startTransition(FADE_IN_ANIMATION_TIME_MS);
      transitionDrawable.setCallback(
          new Callback() {
            @Override
            public void invalidateDrawable(Drawable who) {
              getView().invalidate();
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {}

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {}
          });
      getView()
          .postDelayed(
              () -> {
                if (!cancelled) {
                  // Allows GC of the initial drawable and the transition drawable. Additionally
                  // fixes the issue where the transition sometimes doesn't occur, which would
                  // result in blank images.
                  imageView.setImageDrawable(finalDrawable);
                  // Invalidating the view as the view doesn't update if not manually updated here.
                  getView().invalidate();
                }
              },
              FADE_IN_ANIMATION_TIME_MS);
    }

    private boolean shouldFadeInImage() {
      return (getParameters().clock.elapsedRealtime() - initialTime)
          > getParameters().hostProviders.getAssetProvider().getFadeImageThresholdMs();
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
