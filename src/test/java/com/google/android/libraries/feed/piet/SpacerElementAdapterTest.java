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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import com.google.android.libraries.feed.piet.SpacerElementAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ElementsProto.SpacerElement;
import com.google.search.now.ui.piet.StylesProto.EdgeWidths;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link SpacerElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class SpacerElementAdapterTest {
  @Mock private AdapterParameters adapterParameters;
  @Mock private FrameContext frameContext;
  @Mock private AssetProvider assetProvider;
  @Mock private StyleProvider mockStyleProvider;

  private Context context;

  private SpacerElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    when(frameContext.getAssetProvider()).thenReturn(assetProvider);
    adapter = new KeySupplier().getAdapter(context, adapterParameters);
  }

  @Test
  public void testCreate() {
    int paddingWidth = 2;
    SpacerElement spacerElement = SpacerElement.newBuilder().setHeight(1).build();
    EdgeWidths padding =
        EdgeWidths.newBuilder().setTop(paddingWidth).setBottom(paddingWidth).build();
    when(frameContext.makeStyleFor(any(StyleIdsStack.class))).thenReturn(mockStyleProvider);
    when(mockStyleProvider.getPadding()).thenReturn(padding);

    adapter.bindModel(spacerElement, frameContext);
    View v = adapter.getView();
    assertThat(v).isNotNull();
    verify(mockStyleProvider).setElementStyles(context, frameContext, v);
  }

  @Test
  public void testBindAndUnbindKeepsViewButChangesParams() {
    // Bind adapter and check height
    int padding = 2;
    StyleIdsStack styleStack1 = StyleIdsStack.newBuilder().addStyleIds("1").build();
    SpacerElement spacerElement =
        SpacerElement.newBuilder().setHeight(1).setStyleReferences(styleStack1).build();
    Style style =
        Style.newBuilder()
            .setPadding(EdgeWidths.newBuilder().setTop(padding).setBottom(padding).build())
            .build();
    StyleProvider styleProvider = new StyleProviderImpl(style);
    when(frameContext.makeStyleFor(styleStack1)).thenReturn(styleProvider);

    adapter.bindModel(spacerElement, frameContext);

    View v1 = adapter.getView();
    assertThat(v1.getLayoutParams().height).isEqualTo(5);

    // Unbind adapter
    adapter.unbindModel();

    // Bind adapter with different height and check that view is reused, but height is changes.
    int padding2 = 4;
    StyleIdsStack styleStack2 = StyleIdsStack.newBuilder().addStyleIds("2").build();
    SpacerElement spacerElement2 =
        SpacerElement.newBuilder().setHeight(2).setStyleReferences(styleStack2).build();
    Style style2 =
        Style.newBuilder()
            .setPadding(EdgeWidths.newBuilder().setTop(padding2).setBottom(padding2).build())
            .build();
    StyleProvider styleProvider2 = new StyleProviderImpl(style2);
    when(frameContext.makeStyleFor(styleStack2)).thenReturn(styleProvider2);

    adapter.bindModel(spacerElement2, frameContext);

    View v2 = adapter.getView();
    assertThat(v2.getLayoutParams().height).isEqualTo(10);

    assertThat(v2).isSameAs(v1);
  }
}
