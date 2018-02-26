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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.ImageElementAdapter.Key;
import com.google.android.libraries.feed.piet.ImageElementAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.android.libraries.feed.piet.util.ViewUtils;
import com.google.search.now.ui.piet.BindingRefsProto.ImageBindingRef;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.ImagesProto.ImageSource;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link ImageElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImageElementAdapterTest {
  private static final int HEIGHT_DP = 123;
  private static final int WIDTH_DP = 321;
  private static final EdgeWidths PADDING =
      EdgeWidths.newBuilder().setBottom(1).setTop(2).setStart(3).setEnd(4).build();
  private static final Image DEFAULT_IMAGE =
      Image.newBuilder().addSources(ImageSource.newBuilder().setUrl("icanhas.chz")).build();
  private static final ImageElement DEFAULT_MODEL =
      ImageElement.newBuilder().setImage(DEFAULT_IMAGE).build();

  @Mock private ElementAdapterFactory adapterFactory;
  @Mock private FrameContext frameContext;
  @Mock private FrameContext frameContextWithStyle;
  @Mock private AssetProvider assetProvider;
  @Mock private StyleProvider styleProvider;

  @Captor ArgumentCaptor<Consumer<Drawable>> callbackCaptor;

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
    heightPx = (int) ViewUtils.dpToPx(HEIGHT_DP, context);
    widthPx = (int) ViewUtils.dpToPx(WIDTH_DP, context);
    AdapterParameters parameters = new AdapterParameters(context, null, null, null, adapterFactory);

    when(frameContext.bindNewStyle(any())).thenReturn(frameContextWithStyle);
    when(frameContext.getAssetProvider()).thenReturn(assetProvider);
    when(frameContext.getCurrentStyle()).thenReturn(DEFAULT_STYLE_PROVIDER);
    when(frameContextWithStyle.getAssetProvider()).thenReturn(assetProvider);
    when(frameContextWithStyle.getCurrentStyle()).thenReturn(styleProvider);
    when(styleProvider.getPadding()).thenReturn(PADDING);

    adapter = new KeySupplier().getAdapter(context, parameters);
  }

  @Test
  public void testCreate() {
    assertThat(adapter).isNotNull();
  }

  @Test
  public void testBindModel_image() {
    StyleIdsStack styles = StyleIdsStack.newBuilder().addStyleIds("stylecat").build();
    ImageElement model =
        ImageElement.newBuilder().setImage(DEFAULT_IMAGE).setStyleReferences(styles).build();

    adapter.bindModel(model, Element.getDefaultInstance(), frameContext);

    imageView = getImageView();
    assertDrawableSet(imageView);
    assertThat(imageView.getScaleType()).isEqualTo(ScaleType.CENTER_CROP);

    assertThat(adapter.getElementStyleIdsStack()).isEqualTo(styles);
  }

  @Test
  public void testBindModel_imageBinding() {
    ImageBindingRef imageBinding = ImageBindingRef.newBuilder().setBindingId("feline").build();
    ImageElement model = ImageElement.newBuilder().setImageBinding(imageBinding).build();
    when(frameContext.getImageFromBinding(imageBinding)).thenReturn(DEFAULT_IMAGE);

    adapter.bindModel(model, Element.getDefaultInstance(), frameContext);

    assertDrawableSet(getImageView());
  }

  @Test
  public void testBindModel_notFound() {
    ImageBindingRef imageBinding = ImageBindingRef.newBuilder().setBindingId("INVALID").build();
    ImageElement model = ImageElement.newBuilder().setImageBinding(imageBinding).build();
    when(frameContext.getImageFromBinding(imageBinding)).thenReturn(null);

    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.bindModel(model, Element.getDefaultInstance(), frameContext);
              }
            })
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Image binding not found");
  }

  @Test
  public void testBindModel_noContent() {
    ImageElement model = ImageElement.getDefaultInstance();

    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.bindModel(model, Element.getDefaultInstance(), frameContext);
              }
            })
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("Unsupported or missing content");
  }

  @Test
  public void testBindModel() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    assertThat(adapter.getModel()).isSameAs(DEFAULT_MODEL);

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);

    imageView = getImageView();
    assertDrawableSet(imageView);
    assertThat(imageView.getLayoutParams().height).isEqualTo(heightPx);
    assertThat(imageView.getLayoutParams().width).isEqualTo(widthPx);
    assertThat(((LayoutParams) imageView.getLayoutParams()).gravity)
        .isEqualTo(Gravity.CENTER_HORIZONTAL);

    assertThat(adapter.getComputedHeightPx())
        .isEqualTo(heightPx + PADDING.getTop() + PADDING.getBottom());
    assertThat(adapter.getComputedWidthPx())
        .isEqualTo(widthPx + PADDING.getStart() + PADDING.getEnd());
  }

  @Test
  public void testBindModel_again() {
    // Bind a model, then unbind it.
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    imageView = getImageView();
    Key key1 = adapter.getKey();
    adapter.unbindModel();

    // Bind a different model
    setStyle(WIDTH_DP, HEIGHT_DP);
    ImageElement model2 = ImageElement.newBuilder().setImage(Image.getDefaultInstance()).build();
    adapter.bindModel(model2, frameContext);
    verify(assetProvider).getImage(eq(Image.getDefaultInstance()), callbackCaptor.capture());

    Key key2 = adapter.getKey();
    assertThat(key1).isSameAs(key2);
    assertThat(adapter.getModel()).isSameAs(model2);
    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);

    Drawable drawable2 = new ColorDrawable(Color.RED);
    callbackCaptor.getValue().accept(drawable2);

    ImageView imageView2 = (ImageView) adapter.getBaseView().getChildAt(0);

    assertThat(imageView2).isNotSameAs(imageView);
    assertThat(imageView2.getDrawable()).isEqualTo(drawable2);
    assertThat(imageView2.getLayoutParams().height).isEqualTo(widthPx);
    assertThat(imageView2.getLayoutParams().width).isEqualTo(heightPx);
  }

  @Test
  public void testBindModel_emptyModel() {
    setStyle(null, null);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    assertThat(adapter.getModel()).isSameAs(DEFAULT_MODEL);

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);

    // Assert that width and height are set to the defaults
    imageView = getImageView();
    assertDrawableSet(imageView);
    assertThat(imageView.getLayoutParams().height).isEqualTo(LayoutParams.WRAP_CONTENT);
    assertThat(imageView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);

    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testBindModel_heightOnly() {
    setStyle(HEIGHT_DP, null);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getModel()).isEqualTo(DEFAULT_MODEL);

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);

    // Width defaults to MATCH_PARENT
    imageView = getImageView();
    assertThat(imageView.getLayoutParams().height).isEqualTo(heightPx);
    assertThat(imageView.getLayoutParams().width).isEqualTo(LayoutParams.MATCH_PARENT);

    assertThat(adapter.getComputedHeightPx())
        .isEqualTo(heightPx + PADDING.getTop() + PADDING.getBottom());
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testBindModel_widthOnly() {
    setStyle(null, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getModel()).isEqualTo(DEFAULT_MODEL);

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);

    // Image defaults to a square.
    imageView = getImageView();
    assertThat(imageView.getLayoutParams().height).isEqualTo(widthPx);
    assertThat(imageView.getLayoutParams().width).isEqualTo(widthPx);

    assertThat(adapter.getComputedHeightPx())
        .isEqualTo(widthPx + PADDING.getTop() + PADDING.getBottom());
    assertThat(adapter.getComputedWidthPx())
        .isEqualTo(widthPx + PADDING.getStart() + PADDING.getEnd());
  }

  @Test
  public void testBindModel_bindingTwiceThrowsException() {
    setStyle(HEIGHT_DP, WIDTH_DP);

    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                adapter.bindModel(DEFAULT_MODEL, frameContext);
              }
            })
        .throwsAnExceptionOfType(IllegalStateException.class)
        .that()
        .hasMessageThat()
        .contains("An image loading callback exists");
  }

  @Test
  public void testUnbind() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    adapter.unbindModel();

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);

    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testUnbind_emptyFrame() {
    adapter.unbindModel();

    assertThat(adapter.getView()).isNull();
  }

  @Test
  public void testUnbind_cancelsCallback() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    imageView = getImageView();

    adapter.unbindModel();
    verify(assetProvider).getImage(eq(DEFAULT_IMAGE), callbackCaptor.capture());
    callbackCaptor.getValue().accept(defaultDrawable);

    // The drawable was not set by the callback because the model had already been unbound.
    assertThat(imageView.getDrawable()).isEqualTo(null);
  }

  @Test
  public void testUnbind_cancelsCallback2() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    verify(assetProvider).getImage(eq(DEFAULT_IMAGE), callbackCaptor.capture());

    adapter.unbindModel();

    // Bind a new model before the callback has completed
    Image model2 = Image.newBuilder().setTintColor(5).build();
    adapter.bindModel(ImageElement.newBuilder().setImage(model2).build(), frameContext);

    // Callback now completes
    callbackCaptor.getValue().accept(defaultDrawable);

    // But has no effect
    imageView = getImageView();
    assertThat(imageView.getDrawable()).isEqualTo(null);

    // And now the callback for the second model completes
    Drawable drawable2 = new ColorDrawable(Color.RED);
    verify(assetProvider).getImage(eq(model2), callbackCaptor.capture());
    callbackCaptor.getValue().accept(drawable2);

    // The image has the second drawable as expected.
    assertThat(imageView.getDrawable()).isEqualTo(drawable2);
  }

  @Test
  public void testComputedDimensions_unbound() {
    assertThat(adapter.getComputedHeightPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
    assertThat(adapter.getComputedWidthPx()).isEqualTo(ElementAdapter.DIMENSION_NOT_SET);
  }

  @Test
  public void testComputedDimensions_bound() {
    setStyle(HEIGHT_DP, WIDTH_DP);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getComputedHeightPx())
        .isEqualTo(heightPx + PADDING.getTop() + PADDING.getBottom());
    assertThat(adapter.getComputedWidthPx())
        .isEqualTo(widthPx + PADDING.getStart() + PADDING.getEnd());
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
        .contains("Missing ImageElement");

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
        .contains("Missing ImageElement");
  }

  private void setStyle(@Nullable Integer height, @Nullable Integer width) {
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

  private ImageView getImageView() {
    assertThat(adapter.getBaseView().getChildAt(0)).isInstanceOf(ImageView.class);
    return (ImageView) adapter.getBaseView().getChildAt(0);
  }

  private void assertDrawableSet(ImageView imageView) {
    verify(assetProvider).getImage(eq(DEFAULT_IMAGE), callbackCaptor.capture());
    callbackCaptor.getValue().accept(defaultDrawable);
    assertThat(imageView.getDrawable()).isEqualTo(defaultDrawable);
    assertThat(imageView.getScaleType()).isEqualTo(ScaleType.CENTER_CROP);
  }
}
