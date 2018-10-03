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
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.search.now.ui.piet.BindingRefsProto.ElementBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ImagesProto.Image;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link StackedElementAdapter} */
@RunWith(RobolectricTestRunner.class)
public class StackedElementAdapterTest {
  private static final Element DUMMY_BASE_ELEMENT = Element.getDefaultInstance();
  private static final Content INLINE_CONTENT =
      Content.newBuilder()
          .setElement(Element.newBuilder().setElementList(ElementList.getDefaultInstance()))
          .build();
  private static final Element BOUND_ELEMENT =
      Element.newBuilder().setGridRow(GridRow.getDefaultInstance()).build();
  private static final String ELEMENT_BINDING_ID = "RowBinding";
  private static final ElementBindingRef ELEMENT_BINDING =
      ElementBindingRef.newBuilder().setBindingId(ELEMENT_BINDING_ID).build();
  private static final BindingValue ELEMENT_BINDING_VALUE =
      BindingValue.newBuilder().setBindingId(ELEMENT_BINDING_ID).setElement(BOUND_ELEMENT).build();
  private static final Content BOUND_CONTENT =
      Content.newBuilder().setBoundElement(ELEMENT_BINDING).build();

  @Mock private FrameContext frameContext;
  @Mock private AssetProvider assetProvider;
  @Mock private StyleProvider mockStyleProvider;
  @Mock private HostProviders mockHostProviders;

  private Context context;
  private AdapterParameters adapterParameters;

  private StackedElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);

    when(frameContext.getElementBindingValue(ELEMENT_BINDING)).thenReturn(ELEMENT_BINDING_VALUE);
    when(frameContext.makeStyleFor(any())).thenReturn(mockStyleProvider);
    when(mockHostProviders.getAssetProvider()).thenReturn(assetProvider);

    adapterParameters = new AdapterParameters(context, Suppliers.of(null), mockHostProviders);

    adapter = new StackedElementAdapter.KeySupplier().getAdapter(context, adapterParameters);
  }

  @Test
  public void testCreatesInlineAdaptersInOnCreate() {
    List<Content> contents = asList(INLINE_CONTENT, BOUND_CONTENT);

    adapter.createAdapter(contents, DUMMY_BASE_ELEMENT, frameContext);

    assertThat(adapter.childAdapters).hasSize(1);
    assertThat(adapter.childAdapters.get(0)).isInstanceOf(ElementListAdapter.class);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(1);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
  }

  @Test
  public void testCreatesBoundAdaptersInOnBind() {
    List<Content> contents = asList(INLINE_CONTENT, BOUND_CONTENT);

    adapter.createAdapter(contents, DUMMY_BASE_ELEMENT, frameContext);
    adapter.bindModel(contents, DUMMY_BASE_ELEMENT, frameContext);

    assertThat(adapter.childAdapters).hasSize(2);
    assertThat(adapter.childAdapters.get(0)).isInstanceOf(ElementListAdapter.class);
    assertThat(adapter.childAdapters.get(1)).isInstanceOf(GridRowAdapter.class);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(2);
    assertThat(adapter.getBaseView().getChildAt(0))
        .isSameAs(adapter.childAdapters.get(0).getView());
    assertThat(adapter.getBaseView().getChildAt(1))
        .isSameAs(adapter.childAdapters.get(1).getView());
  }

  @Test
  public void testSetsLayoutParamsOnChildForInlineAdapters() {
    Content imageContent =
        Content.newBuilder()
            .setElement(
                Element.newBuilder()
                    .setImageElement(
                        ImageElement.newBuilder().setImage(Image.getDefaultInstance())))
            .build();
    List<Content> contents = Collections.singletonList(imageContent);

    when(mockStyleProvider.hasWidth()).thenReturn(true);
    when(mockStyleProvider.hasHeight()).thenReturn(true);
    when(mockStyleProvider.getWidth()).thenReturn(123);
    when(mockStyleProvider.getHeight()).thenReturn(456);

    adapter.createAdapter(contents, DUMMY_BASE_ELEMENT, frameContext);
    adapter.bindModel(contents, DUMMY_BASE_ELEMENT, frameContext);

    LayoutParams inlineAdapterLayoutParams =
        adapter.childAdapters.get(0).getView().getLayoutParams();

    assertThat(inlineAdapterLayoutParams.width).isEqualTo(123);
    assertThat(inlineAdapterLayoutParams.height).isEqualTo(456);
  }

  @Test
  public void testSetsLayoutParamsOnChildForBoundAdapters() {
    ElementBindingRef imageBinding = ElementBindingRef.newBuilder().setBindingId("image").build();
    BindingValue imageBindingValue =
        BindingValue.newBuilder()
            .setElement(
                Element.newBuilder()
                    .setImageElement(
                        ImageElement.newBuilder().setImage(Image.getDefaultInstance())))
            .build();
    when(frameContext.getElementBindingValue(imageBinding)).thenReturn(imageBindingValue);
    List<Content> contents =
        Collections.singletonList(Content.newBuilder().setBoundElement(imageBinding).build());

    when(mockStyleProvider.hasWidth()).thenReturn(true);
    when(mockStyleProvider.hasHeight()).thenReturn(true);
    when(mockStyleProvider.getWidth()).thenReturn(123);
    when(mockStyleProvider.getHeight()).thenReturn(456);

    adapter.createAdapter(contents, DUMMY_BASE_ELEMENT, frameContext);
    adapter.bindModel(contents, DUMMY_BASE_ELEMENT, frameContext);

    LayoutParams boundAdapterLayoutParams =
        adapter.childAdapters.get(0).getView().getLayoutParams();

    assertThat(boundAdapterLayoutParams.width).isEqualTo(123);
    assertThat(boundAdapterLayoutParams.height).isEqualTo(456);
  }

  @Test
  public void testSetsMarginsOnChild() {
    List<Content> contents = Collections.singletonList(INLINE_CONTENT);

    adapter.createAdapter(contents, DUMMY_BASE_ELEMENT, frameContext);
    adapter.bindModel(contents, DUMMY_BASE_ELEMENT, frameContext);

    MarginLayoutParams childAdapterLayoutParams =
        (MarginLayoutParams) adapter.childAdapters.get(0).getView().getLayoutParams();

    verify(mockStyleProvider).applyMargins(context, childAdapterLayoutParams);
  }

  @Test
  public void testGetContentsFromModelReturnsModel() {
    List<Content> contents = asList(Content.newBuilder().build(), INLINE_CONTENT, BOUND_CONTENT);

    assertThat(adapter.getContentsFromModel(contents)).isSameAs(contents);
  }

  @Test
  public void testGetModelFromElementFails() {
    assertThatRunnable(() -> adapter.getModelFromElement(Element.getDefaultInstance()))
        .throwsAnExceptionOfType(UnsupportedOperationException.class)
        .that()
        .hasMessageThat()
        .contains("StackedElementAdapters don't have a base Element");
  }
}
