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

import static com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier.SINGLETON_KEY;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ParameterizedTextEvaluator;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import com.google.search.now.ui.piet.TextProto.ParameterizedText;
import org.robolectric.RobolectricTestRunner;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link TextElementAdapter} instance of the {@link AdapterFactory}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class TextElementAdapterFactoryTest {
  private static final String TEXT_LINE_CONTENT = "Content";

  @Mock private FrameContext frameContext;

  private AdapterParameters adapterParameters;
  private Context context;

  private AdapterFactory<TextElementAdapter, TextElement> textElementFactory;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    adapterParameters =
        new AdapterParameters(null, null, null, new ParameterizedTextEvaluator(), null);
    textElementFactory =
        new AdapterFactory<>(context, adapterParameters, new TextElementAdapter.KeySupplier());
  }

  @Test
  public void testKeySupplier() {
    TextElementAdapter.KeySupplier keySupplier = new TextElementAdapter.KeySupplier();
    assertThat(keySupplier.getAdapterTag()).isEqualTo("TextElementAdapter");
    assertThat(keySupplier.getAdapter(context, adapterParameters)).isNotNull();
    assertThat(keySupplier).isNotInstanceOf(SingletonKeySupplier.class);
    assertThat(keySupplier.getKey(frameContext, getBaseTextElement(null)))
        .isNotSameAs(SINGLETON_KEY);
    // TODO: When we finish Font support we need to test the getKey()
  }

  @Test
  public void testBaseCreate() {
    TextElement textElement = getBaseTextElement(null);
    TextElementAdapter textElementAdapter = textElementFactory.get(textElement, frameContext);
    assertThat(textElementAdapter).isNotNull();
    View view = textElementAdapter.getView();
    assertThat(view).isInstanceOf(TextView.class);
    TextView textView = textElementAdapter.getBaseView();
    assertThat(textView).isNotNull();
    assertThat(textElementAdapter.getModel()).isNotNull();
    assertThat(textView.getText()).isEqualTo(TEXT_LINE_CONTENT);
  }

  @Test
  public void testRelease() {
    TextElement textElement = getBaseTextElement(null);
    TextElementAdapter textElementAdapter = textElementFactory.get(textElement, frameContext);
    View view = textElementAdapter.getView();
    assertThat(view).isInstanceOf(TextView.class);
    TextView textView = textElementAdapter.getBaseView();
    assertThat(textView).isNotNull();

    textElementFactory.release(textElementAdapter);
    assertThat(textElementAdapter.getRawModel()).isNull();
    assertThat(textElementAdapter.getView()).isNotNull();

    // Verify we get the same item when we create it again
    TextElementAdapter textElementAdapter2 = textElementFactory.get(textElement, frameContext);
    assertThat(textElementAdapter).isEqualTo(textElementAdapter2);
    assertThat(textView).isEqualTo(textElementAdapter2.getView());
    assertThat(textElementAdapter.getRawModel()).isNotNull();
    assertThat(textElementAdapter.getView()).isNotNull();
  }

  private TextElement getBaseTextElement(@Nullable StyleProvider styleProvider) {
    StyleProvider sp =
        styleProvider != null ? styleProvider : StyleProviderImpl.DEFAULT_STYLE_PROVIDER;
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(sp);
    when(frameContext.makeStyleFor(any(StyleIdsStack.class), anyBoolean())).thenReturn(sp);
    when(frameContext.getCurrentStyle()).thenReturn(sp);

    TextElement.Builder textElement = TextElement.newBuilder();
    ParameterizedText text = ParameterizedText.newBuilder().setText(TEXT_LINE_CONTENT).build();
    textElement.setParameterizedText(text);
    return textElement.build();
  }
}
