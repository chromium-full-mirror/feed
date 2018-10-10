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

package com.google.android.libraries.feed.sharedstream.piet;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.graphics.drawable.Drawable;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.testing.FakeClock;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.ImagesProto.ImageSource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link PietAssetProvider}. */
@RunWith(RobolectricTestRunner.class)
public class PietAssetProviderTest {

  private static final int DEFAULT_CORNER_RADIUS = 10;
  private static final int WIDTH_PX = 50;
  private static final int HEIGHT_PX = 150;
  private static final int FADE_THRESHOLD_MS = 333;

  @Mock private CardConfiguration cardConfiguration;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private Configuration config;

  private FakeClock clock;
  private PietAssetProvider pietAssetProvider;

  @Before
  public void setUp() {
    initMocks(this);

    when(cardConfiguration.getDefaultCornerRadius()).thenReturn(DEFAULT_CORNER_RADIUS);
    when(config.getValueOrDefault(eq(ConfigKey.FADE_IMAGE_THRESHOLD_MS), anyInt()))
        .thenReturn(FADE_THRESHOLD_MS);

    clock = new FakeClock();
    clock.set(TimeUnit.MINUTES.toMillis(1));
    pietAssetProvider = new PietAssetProvider(imageLoaderApi, cardConfiguration, clock, config);
  }

  @Test
  public void testGetImage() {
    List<String> urls = Arrays.asList("url0", "url1", "url2");
    Image.Builder imageBuilder = Image.newBuilder();
    for (String url : urls) {
      imageBuilder.addSources(ImageSource.newBuilder().setUrl(url).build());
    }

    Consumer<Drawable> consumer =
        value -> {
          // Do nothing.
        };

    pietAssetProvider.getImage(imageBuilder.build(), WIDTH_PX, HEIGHT_PX, consumer);
    verify(imageLoaderApi).loadDrawable(urls, WIDTH_PX, HEIGHT_PX, consumer);
  }

  @Test
  public void testGetRelativeElapsedString() {
    assertThat(pietAssetProvider.getRelativeElapsedString(TimeUnit.MINUTES.toMillis(1)))
        .isEqualTo("1 minute ago");
  }

  @Test
  public void testGetDefaultCornerRadius() {
    assertThat(pietAssetProvider.getDefaultCornerRadius()).isEqualTo(DEFAULT_CORNER_RADIUS);
  }

  @Test
  public void testGetFadeThreshold() {
    assertThat(pietAssetProvider.getFadeImageThresholdMs()).isEqualTo(FADE_THRESHOLD_MS);
  }
}
