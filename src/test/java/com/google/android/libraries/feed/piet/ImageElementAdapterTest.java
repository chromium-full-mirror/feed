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
import static com.google.android.libraries.feed.host.imageloader.ImageLoaderApi.DIMENSION_UNKNOWN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.testing.FakeClock;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.ImageElementAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.search.now.ui.piet.BindingRefsProto.ImageBindingRef;
import com.google.search.now.ui.piet.BindingRefsProto.StyleBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.Visibility;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.ImagesProto.ImageSource;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners.Corners;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link ImageElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class ImageElementAdapterTest {
  private static final int HEIGHT_DP = 123;
  private static final int WIDTH_DP = 321;
  private static final EdgeWidths PADDING =
      EdgeWidths.newBuilder().setBottom(1).setTop(2).setStart(3).setEnd(4).build();
  private static final RoundedCorners CORNERS =
      RoundedCorners.newBuilder().setBitmask(Corners.BOTTOM_START_VALUE).setRadius(34).build();
  private static final Image DEFAULT_IMAGE =
      Image.newBuilder().addSources(ImageSource.newBuilder().setUrl("icanhas.chz")).build();
  private static final Element DEFAULT_MODEL =
      asElement(ImageElement.newBuilder().setImage(DEFAULT_IMAGE).build());

  @Mock private ElementAdapterFactory adapterFactory;
  @Mock private TemplateBinder templateBinder;
  @Mock private FrameContext frameContext;
  @Mock private AssetProvider assetProvider;
  @Mock private StyleProvider styleProvider;
  @Mock private HostProviders hostProviders;

  @Captor ArgumentCaptor<Consumer<Drawable>> consumerArgumentCaptor;

  private Context context;
  private int heightPx;
  private int widthPx;
  private ImageView imageView;
  private final Drawable defaultDrawable = new ColorDrawable(Color.BLUE);

  private ImageElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    heightPx = (int) LayoutUtils.dpToPx(HEIGHT_DP, context);
    widthPx = (int) LayoutUtils.dpToPx(WIDTH_DP, context);
    AdapterParameters parameters =
        new AdapterParameters(
            context, null, hostProviders, null, adapterFactory, templateBinder, new FakeClock());

    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(styleProvider);
    when(hostProviders.getAssetProvider()).thenReturn(assetProvider);
    when(styleProvider.getPadding()).thenReturn(PADDING);
    when(styleProvider.hasRoundedCorners()).thenReturn(true);
    when(styleProvider.getRoundedCorners()).thenReturn(CORNERS);
    when(styleProvider.getRoundedCornerRadius(context)).thenReturn(CORNERS.getRadius());

    adapter = new KeySupplier().getAdapter(context, parameters);
  }

  @Test
  public void testCreate() {
    assertThat(adapter).isNotNull();
  }

  @Test
  public void testCreateAdapter() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    assertThat(adapter.getModel()).isSameAs(DEFAULT_MODEL.getImageElement());

    assertThat(adapter.getView()).isNotNull();

    assertThat(adapter.getComputedHeightPx()).isEqualTo(heightPx);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(widthPx);
    assertThat(adapter.getBaseView().getCropToPadding()).isTrue();
    verify(styleProvider).applyElementStyles(context, adapter.getBaseView());
  }

  @Test
  public void testCreateAdapter_noDimensionsSet() {
    setStyle(null, null);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getModel()).isSameAs(DEFAULT_MODEL.getImageElement());

    assertThat(adapter.getView()).isNotNull();

    // Assert that width and height are set to the defaults
    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testCreateAdapter_heightOnly() {
    setStyle(HEIGHT_DP, null);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getModel()).isEqualTo(DEFAULT_MODEL.getImageElement());

    assertThat(adapter.getView()).isNotNull();

    // Width defaults to MATCH_PARENT
    assertThat(adapter.getComputedHeightPx()).isEqualTo(heightPx);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testCreateAdapter_widthOnly() {
    setStyle(null, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getModel()).isEqualTo(DEFAULT_MODEL.getImageElement());

    assertThat(adapter.getView()).isNotNull();

    // Image defaults to a square.
    assertThat(adapter.getComputedHeightPx()).isEqualTo(widthPx);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(widthPx);
  }

  @Test
  public void testCreateAdapter_noContent() {
    Element model = asElement(ImageElement.getDefaultInstance());

    adapter.createAdapter(model, frameContext);

    assertThatRunnable(() -> adapter.bindModel(model, frameContext))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Unsupported or missing content");
  }

  @Test
  public void testBindModel_image() {
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    Element model =
        asElement(
            ImageElement.newBuilder().setImage(DEFAULT_IMAGE).setStyleReferences(styles).build());

    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    imageView = adapter.getBaseView();
    assertDrawableSet(imageView);

    assertThat(adapter.getModel()).isSameAs(model.getImageElement());
    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(styles);
  }

  @Test
  public void testBindModel_bitmapImageScales() {
    int styleWidth = 10;
    int styleHeight = 15;
    setStyle(styleHeight, styleWidth);
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    Element model =
        asElement(
            ImageElement.newBuilder().setImage(DEFAULT_IMAGE).setStyleReferences(styles).build());

    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(true);
    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    // Fire the callback
    Bitmap bitmap = Bitmap.createBitmap(100, 150, Config.ALPHA_8);
    BitmapDrawable specifiedBitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
    verify(assetProvider)
        .getImage(
            eq(DEFAULT_IMAGE), eq(styleWidth), eq(styleHeight), consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(specifiedBitmapDrawable);

    imageView = adapter.getBaseView();

    assertThat(imageView.getDrawable()).isInstanceOf(BitmapDrawable.class);

    Bitmap createdBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
    assertThat(createdBitmap.getWidth()).isEqualTo(styleWidth);
    assertThat(createdBitmap.getHeight()).isEqualTo(styleHeight);
  }

  @Test
  public void testBindModel_bitmapSameSizeDoesNotScale() {
    int styleWidth = 100;
    int styleHeight = 150;
    setStyle(styleHeight, styleWidth);
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    Element model =
        asElement(
            ImageElement.newBuilder().setImage(DEFAULT_IMAGE).setStyleReferences(styles).build());

    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(true);
    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    // Fire the callback
    Bitmap bitmap = Bitmap.createBitmap(styleWidth, styleHeight, Config.ALPHA_8);
    BitmapDrawable specifiedBitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
    verify(assetProvider)
        .getImage(
            eq(DEFAULT_IMAGE), eq(styleWidth), eq(styleHeight), consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(specifiedBitmapDrawable);

    imageView = adapter.getBaseView();

    assertThat(imageView.getDrawable()).isSameAs(specifiedBitmapDrawable);
  }

  @Test
  public void testBindModel_bitmapImageDoesNotScaleDueToHostConfig() {
    int styleWidth = 10;
    int styleHeight = 15;
    int originalBitmapWidth = 100;
    int originalBitmapHeight = 150;
    setStyle(styleHeight, styleWidth);
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    Element model =
        asElement(
            ImageElement.newBuilder().setImage(DEFAULT_IMAGE).setStyleReferences(styles).build());

    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(false);
    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    // Fire the callback
    Bitmap bitmap = Bitmap.createBitmap(originalBitmapWidth, originalBitmapHeight, Config.ALPHA_8);
    BitmapDrawable specifiedBitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
    verify(assetProvider)
        .getImage(
            eq(DEFAULT_IMAGE), eq(styleWidth), eq(styleHeight), consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(specifiedBitmapDrawable);

    imageView = adapter.getBaseView();

    assertThat(imageView.getDrawable()).isInstanceOf(BitmapDrawable.class);

    Bitmap createdBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
    assertThat(createdBitmap.getWidth()).isEqualTo(originalBitmapWidth);
    assertThat(createdBitmap.getHeight()).isEqualTo(originalBitmapHeight);
  }

  @Test
  public void testBindModel_imageBinding() {
    ImageBindingRef imageBinding = ImageBindingRef.newBuilder().setBindingId("feline").build();
    Element model = asElement(ImageElement.newBuilder().setImageBinding(imageBinding).build());
    when(frameContext.getImageBindingValue(imageBinding))
        .thenReturn(BindingValue.newBuilder().setImage(DEFAULT_IMAGE).build());

    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    assertDrawableSet(adapter.getBaseView());
    assertThat(adapter.getModel()).isSameAs(model.getImageElement());
  }

  @Test
  public void testBindModel_optionalAbsent() {
    String bindingRef = "foto";
    ImageBindingRef imageBindingRef =
        ImageBindingRef.newBuilder().setBindingId(bindingRef).setIsOptional(true).build();
    Element imageBindingElement =
        asElement(ImageElement.newBuilder().setImageBinding(imageBindingRef).build());
    adapter.createAdapter(
        asElement(ImageElement.newBuilder().setImage(Image.getDefaultInstance()).build()),
        frameContext);
    when(frameContext.getImageBindingValue(imageBindingRef))
        .thenReturn(BindingValue.getDefaultInstance());

    adapter.bindModel(imageBindingElement, frameContext);
    assertThat(adapter.getBaseView().getDrawable()).isNull();
    assertThat(adapter.getBaseView().getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void testBindModel_noContentInBindingValue() {
    String bindingRef = "foto";
    ImageBindingRef imageBindingRef = ImageBindingRef.newBuilder().setBindingId(bindingRef).build();
    Element imageBindingElement =
        asElement(ImageElement.newBuilder().setImageBinding(imageBindingRef).build());
    adapter.createAdapter(
        asElement(ImageElement.newBuilder().setImage(Image.getDefaultInstance()).build()),
        frameContext);
    when(frameContext.getImageBindingValue(imageBindingRef))
        .thenReturn(
            BindingValue.newBuilder()
                .setBindingId(bindingRef)
                .setVisibility(Visibility.VISIBLE)
                .clearImage()
                .build());

    assertThatRunnable(() -> adapter.bindModel(imageBindingElement, frameContext))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Image binding foto had no content");
  }

  @Test
  public void testBindModel_setsScaleType() {
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    Image imageWithScaleType =
        DEFAULT_IMAGE.toBuilder().setScaleType(Image.ScaleType.CENTER_CROP).build();
    Element model =
        asElement(
            ImageElement.newBuilder()
                .setImage(imageWithScaleType)
                .setStyleReferences(styles)
                .build());

    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    // Fire the callback
    verify(assetProvider)
        .getImage(
            eq(imageWithScaleType),
            eq(DIMENSION_UNKNOWN),
            eq(DIMENSION_UNKNOWN),
            consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(defaultDrawable);

    imageView = adapter.getBaseView();
    assertThat(imageView.getScaleType()).isEqualTo(ScaleType.CENTER_CROP);
  }

  @Test
  public void testBindModel_again() {
    // Bind a model, then unbind it.
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    imageView = adapter.getBaseView();
    RecyclerKey key1 = adapter.getKey();
    adapter.unbindModel();

    // Bind a different model
    Element model2 =
        asElement(ImageElement.newBuilder().setImage(Image.getDefaultInstance()).build());
    adapter.bindModel(model2, frameContext);
    verify(assetProvider)
        .getImage(
            eq(Image.getDefaultInstance()),
            eq(WIDTH_DP),
            eq(HEIGHT_DP),
            consumerArgumentCaptor.capture());

    RecyclerKey key2 = adapter.getKey();
    assertThat(key1).isSameAs(key2);
    assertThat(adapter.getModel()).isSameAs(model2.getImageElement());
    assertThat(adapter.getView()).isNotNull();

    Drawable drawable2 = new ColorDrawable(Color.RED);
    consumerArgumentCaptor.getValue().accept(drawable2);

    ImageView imageView2 = adapter.getBaseView();

    assertThat(imageView2).isSameAs(imageView);
    assertThat(imageView2.getDrawable()).isEqualTo(drawable2);
  }

  @Test
  public void testBindModel_bindingTwiceThrowsException() {
    setStyle(HEIGHT_DP, WIDTH_DP);

    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThatRunnable(() -> adapter.bindModel(DEFAULT_MODEL, frameContext))
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("An image loading callback exists");
  }

  @Test
  public void testBindModel_setsStylesOnlyIfBindingIsDefined() {
    // Create an adapter with a default style
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    verify(styleProvider).applyElementStyles(context, adapter.getBaseView());

    // Styles do not change when a different model is bound
    StyleIdsStack otherStyle = StyleIdsStack.newBuilder().addStyleIds("ignored").build();
    Element imageWithOtherStyle =
        asElement(
            ImageElement.newBuilder()
                .setStyleReferences(otherStyle)
                .setImage(Image.getDefaultInstance())
                .build());
    adapter.bindModel(imageWithOtherStyle, frameContext);
    adapter.unbindModel();

    verify(frameContext, never()).makeStyleFor(otherStyle);

    // Styles do change when a model with a style binding is bound
    StyleIdsStack boundStyle =
        StyleIdsStack.newBuilder()
            .setStyleBinding(StyleBindingRef.newBuilder().setBindingId("tuna"))
            .build();
    Element imageWithBoundStyle =
        asElement(
            ImageElement.newBuilder()
                .setStyleReferences(boundStyle)
                .setImage(Image.getDefaultInstance())
                .build());
    adapter.bindModel(imageWithBoundStyle, frameContext);

    verify(frameContext).makeStyleFor(boundStyle);

    verify(styleProvider, times(2)).applyElementStyles(context, adapter.getBaseView());
  }

  @Test
  public void testBindModel_preLoadFill() {
    Drawable preLoadFillDrawable = new ColorDrawable(Color.RED);

    // Set up the StyleProvider mock
    when(styleProvider.createPreLoadFill()).thenReturn(preLoadFillDrawable);
    when(styleProvider.hasPreLoadFill()).thenReturn(true);

    // Bind and expect the pre-load fill to be set
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    assertThat(adapter.getBaseView().getDrawable()).isSameAs(preLoadFillDrawable);

    // Load drawable andreplace pre-load fill
    verify(assetProvider)
        .getImage(
            eq(DEFAULT_IMAGE),
            eq(DIMENSION_UNKNOWN),
            eq(DIMENSION_UNKNOWN),
            consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(defaultDrawable);
    assertThat(adapter.getBaseView().getDrawable()).isSameAs(defaultDrawable);
  }

  @Test
  public void testUnbind() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    adapter.unbindModel();

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getDrawable()).isNull();

    assertThat(adapter.getComputedHeightPx()).isEqualTo(heightPx);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(widthPx);
  }

  @Test
  public void testUnbind_cancelsCallback() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    imageView = adapter.getBaseView();

    adapter.unbindModel();
    verify(assetProvider)
        .getImage(eq(DEFAULT_IMAGE), eq(WIDTH_DP), eq(HEIGHT_DP), consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(defaultDrawable);

    // The drawable was not set by the callback because the model had already been unbound.
    assertThat(imageView.getDrawable()).isEqualTo(null);
  }

  @Test
  public void testUnbind_cancelsCallback2() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    verify(assetProvider)
        .getImage(eq(DEFAULT_IMAGE), eq(WIDTH_DP), eq(HEIGHT_DP), consumerArgumentCaptor.capture());

    adapter.unbindModel();

    // Bind a new model before the callback has completed
    Image model2 = Image.newBuilder().setTintColor(5).build();
    adapter.bindModel(asElement(ImageElement.newBuilder().setImage(model2).build()), frameContext);

    // Callback now completes
    consumerArgumentCaptor.getValue().accept(defaultDrawable);

    // But has no effect
    imageView = adapter.getBaseView();
    assertThat(imageView.getDrawable()).isEqualTo(null);

    // And now the callback for the second model completes
    Drawable drawable2 = new ColorDrawable(Color.RED);
    verify(assetProvider)
        .getImage(eq(model2), eq(WIDTH_DP), eq(HEIGHT_DP), consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(drawable2);

    // The image has the second drawable as expected.
    assertThat(imageView.getDrawable()).isEqualTo(drawable2);
  }

  @Test
  public void testReleaseAdapter_resetsDims() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    adapter.unbindModel();
    adapter.releaseAdapter();

    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testComputedDimensions_unbound() {
    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testComputedDimensions_bound() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getComputedHeightPx()).isEqualTo(heightPx);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(widthPx);
  }

  @Test
  public void testGetModelFromElement() {
    ImageElement model =
        ImageElement.newBuilder()
            .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds("image"))
            .build();

    Element elementWithModel = Element.newBuilder().setImageElement(model).build();
    assertThat(adapter.getModelFromElement(elementWithModel)).isSameAs(model);

    Element elementWithWrongModel =
        Element.newBuilder().setCustomElement(CustomElement.getDefaultInstance()).build();
    assertThatRunnable(() -> adapter.getModelFromElement(elementWithWrongModel))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing ImageElement");

    Element emptyElement = Element.getDefaultInstance();
    assertThatRunnable(() -> adapter.getModelFromElement(emptyElement))
        .throwsAnExceptionOfType(PietFatalException.class)
        .that()
        .hasMessageThat()
        .contains("Missing ImageElement");
  }

  private void setStyle(/*@Nullable*/ Integer height, /*@Nullable*/ Integer width) {
    if (height != null) {
      when(styleProvider.hasHeight()).thenReturn(true);
      when(styleProvider.getHeight()).thenReturn(height);
    } else {
      when(styleProvider.hasHeight()).thenReturn(false);
      when(styleProvider.getHeight()).thenReturn(0);
    }
    if (width != null) {
      when(styleProvider.hasWidth()).thenReturn(true);
      when(styleProvider.getWidth()).thenReturn(width);
    } else {
      when(styleProvider.hasWidth()).thenReturn(false);
      when(styleProvider.getWidth()).thenReturn(0);
    }
  }

  private void assertDrawableSet(ImageView imageView) {
    verify(assetProvider)
        .getImage(
            eq(DEFAULT_IMAGE),
            eq(DIMENSION_UNKNOWN),
            eq(DIMENSION_UNKNOWN),
            consumerArgumentCaptor.capture());
    consumerArgumentCaptor.getValue().accept(defaultDrawable);
    assertThat(imageView.getDrawable()).isEqualTo(defaultDrawable);
    assertThat(imageView.getScaleType()).isEqualTo(ScaleType.FIT_CENTER);
  }

  private static Element asElement(ImageElement imageElement) {
    return Element.newBuilder().setImageElement(imageElement).build();
  }
}
