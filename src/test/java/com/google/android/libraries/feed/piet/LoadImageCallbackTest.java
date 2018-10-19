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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.testing.FakeClock;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.ui.RoundedCornerWrapperView;
import com.google.search.now.ui.piet.RoundedCornersProto.RoundedCorners;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

/** Tests of the {@link LoadImageCallback}. */
@RunWith(RobolectricTestRunner.class)
public class LoadImageCallbackTest {

  private static final int FADE_IMAGE_THRESHOLD_MS = 682;
  private static final int WIDTH_PX = 10;
  private static final int HEIGHT_PX = 20;

  @Mock private HostProviders hostProviders;
  @Mock private AssetProvider assetProvider;
  @Mock private FrameContext frameContext;

  private final Drawable initialDrawable = new ColorDrawable(Color.RED);
  private final Drawable finalDrawable = new ColorDrawable(Color.BLUE);
  private final FakeClock clock = new FakeClock();
  private AdapterParameters parameters;
  private Context context;

  @Before
  public void setup() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();

    when(hostProviders.getAssetProvider()).thenReturn(assetProvider);
    when(assetProvider.getFadeImageThresholdMs()).thenReturn(FADE_IMAGE_THRESHOLD_MS);
    when(frameContext.getFrameView()).thenReturn(new FrameLayout(context));

    parameters =
        new AdapterParameters(
            context,
            null,
            hostProviders,
            null,
            mock(ElementAdapterFactory.class),
            mock(TemplateBinder.class),
            clock);
  }

  @Test
  public void testInitialDrawable_fromImageView() {
    ImageView imageView = new ImageView(context);
    imageView.setImageDrawable(initialDrawable);

    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);

    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    assertThat(imageView.getDrawable()).isInstanceOf(TransitionDrawable.class);

    TransitionDrawable drawable = (TransitionDrawable) imageView.getDrawable();

    assertThat(drawable.getDrawable(0)).isSameAs(initialDrawable);
    assertThat(drawable.getDrawable(1)).isSameAs(finalDrawable);
  }

  @Test
  public void testInitialDrawable_transparent() {
    ImageView imageView = new ImageView(context);

    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);

    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    assertThat(imageView.getDrawable()).isInstanceOf(TransitionDrawable.class);

    TransitionDrawable drawable = (TransitionDrawable) imageView.getDrawable();

    assertThat(((ColorDrawable) drawable.getDrawable(0)).getColor()).isEqualTo(Color.TRANSPARENT);
  }

  @Test
  public void testQuickLoad_doesntFade() {
    ImageView imageView = new ImageView(context);

    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);

    clock.advance(FADE_IMAGE_THRESHOLD_MS - 1);
    callback.accept(finalDrawable);

    assertThat(imageView.getDrawable()).isSameAs(finalDrawable);
  }

  @Test
  public void testStoresCallback_removeOnCancel() {
    LoadImageCallback callback =
        new LoadImageCallback(
            new ImageView(context),
            ScaleType.CENTER,
            WIDTH_PX,
            HEIGHT_PX,
            context,
            parameters,
            frameContext);
    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    // The callback needs to be stored so that it isn't garbage collected, which is possible as
    // Drawable stores the callback in a WeakReference.
    assertThat(callback.transitionCallback).isNotNull();

    callback.cancel();

    // Cancelling should null out the callback so it can be garbage collected.
    assertThat(callback.transitionCallback).isNull();
  }

  @Test
  public void testDelayedTask() {
    ImageView imageView = new ImageView(context);
    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);
    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    // Starts as transition drawable.
    assertThat(imageView.getDrawable()).isInstanceOf(TransitionDrawable.class);

    ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

    // After the delayed task is run, the drawable is set to the finalDrawable instead of the
    // TransitionDrawable to allow for garbage collection of the TransitionDrawable and
    // initialDrawable.
    assertThat(imageView.getDrawable()).isSameAs(finalDrawable);

    // Clears out the callback to allow it to be garbage collected.
    assertThat(callback.transitionCallback).isNull();
  }

  @Test
  public void testScalingImage() {
    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(true);

    Bitmap bitmap = Bitmap.createBitmap(123, 321, Config.ALPHA_8);

    ImageView imageView = new ImageView(context);

    new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext)
        .accept(new BitmapDrawable(context.getResources(), bitmap));
    BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
    assertThat(bitmapDrawable.getBitmap().getWidth()).isEqualTo(WIDTH_PX);
    assertThat(bitmapDrawable.getBitmap().getHeight()).isEqualTo(HEIGHT_PX);
  }

  @Test
  public void testScalingImage_onlyScaleIfHostSupports() {
    when(assetProvider.shouldPietResizeBitmaps()).thenReturn(false);

    Bitmap bitmap = Bitmap.createBitmap(123, 321, Config.ALPHA_8);

    ImageView imageView = new ImageView(context);

    new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext)
        .accept(new BitmapDrawable(context.getResources(), bitmap));
    BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
    assertThat(bitmapDrawable.getBitmap().getWidth()).isEqualTo(123);
    assertThat(bitmapDrawable.getBitmap().getHeight()).isEqualTo(321);
  }

  @Test
  public void testSetsScaleType() {
    ImageView imageView = new ImageView(context);
    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);
    callback.accept(finalDrawable);

    assertThat(imageView.getScaleType()).isEqualTo(ScaleType.CENTER);
  }

  @Test
  public void testInvalidatesBelowRoundedCornerFrameLayout() {
    FrameLayout frameLayout1 = new FrameLayout(context);
    RoundedCornerWrapperView roundedCornerWrapperView =
        new RoundedCornerWrapperView(context, RoundedCorners.newBuilder().setRadius(16).build());
    FrameLayout frameLayout2 = new FrameLayout(context);

    ImageView imageView = new ImageView(context);
    frameLayout1.addView(roundedCornerWrapperView);
    roundedCornerWrapperView.addView(frameLayout2);
    frameLayout2.addView(imageView);

    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);

    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    // The drawable in question here doesn't matter.
    callback.transitionCallback.invalidateDrawable(new ColorDrawable(Color.BLUE));

    assertThat(shadowOf(frameLayout1).wasInvalidated()).isFalse();
    assertThat(shadowOf(roundedCornerWrapperView).wasInvalidated()).isTrue();
    assertThat(shadowOf(frameLayout2).wasInvalidated()).isTrue();
    assertThat(shadowOf(imageView).wasInvalidated()).isTrue();
  }

  @Test
  public void testInvalidatesBelowRoundedCornerFrameLayout_notAboveFrameView() {
    RoundedCornerWrapperView roundedCornerAboveFrameView =
        new RoundedCornerWrapperView(context, RoundedCorners.newBuilder().setRadius(34).build());
    FrameLayout frameView = new FrameLayout(context);
    RoundedCornerWrapperView roundedCornerBelowFrameView =
        new RoundedCornerWrapperView(context, RoundedCorners.newBuilder().setRadius(32).build());
    ImageView imageView = new ImageView(context);

    roundedCornerAboveFrameView.addView(frameView);
    frameView.addView(roundedCornerBelowFrameView);
    roundedCornerBelowFrameView.addView(imageView);
    when(frameContext.getFrameView()).thenReturn(frameView);

    LoadImageCallback callback =
        new LoadImageCallback(
            imageView, ScaleType.CENTER, WIDTH_PX, HEIGHT_PX, context, parameters, frameContext);

    clock.advance(FADE_IMAGE_THRESHOLD_MS + 1);
    callback.accept(finalDrawable);

    // The drawable in question here doesn't matter.
    callback.transitionCallback.invalidateDrawable(new ColorDrawable(Color.BLUE));

    assertThat(shadowOf(roundedCornerAboveFrameView).wasInvalidated()).isFalse();
    assertThat(shadowOf(frameView).wasInvalidated()).isFalse();
    assertThat(shadowOf(roundedCornerBelowFrameView).wasInvalidated()).isTrue();
    assertThat(shadowOf(imageView).wasInvalidated()).isTrue();
  }
}
