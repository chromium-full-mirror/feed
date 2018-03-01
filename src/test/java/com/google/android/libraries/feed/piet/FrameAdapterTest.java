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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.ui.RoundedCornerColorDrawable;
import com.google.android.libraries.feed.piet.util.FrameContext;
import com.google.android.libraries.feed.piet.util.ParameterizedTextEvaluator;
import com.google.android.libraries.feed.piet.util.Producers;
import com.google.android.libraries.feed.piet.util.StyleProvider;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.Slice;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Frame;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link FrameAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FrameAdapterTest {
  @Mock private AssetProvider assetProvider;
  @Mock private ElementAdapterFactory adapterFactory;
  @Mock private FrameContext frameContext;
  @Mock private FrameModelBinder frameModelBinder;
  @Mock private StyleProvider styleProvider;
  @Mock private ElementListAdapter elementListAdapter;
  @Mock private TemplateInvocationAdapter templateAdapter;

  @Captor private ArgumentCaptor<LayoutParams> layoutParamsCaptor;

  private Context context;
  private AdapterParameters adapterParameters;

  private FrameAdapter frameAdapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    adapterParameters =
        new AdapterParameters(
            null,
            Producers.of(new FrameLayout(context)),
            null,
            new ParameterizedTextEvaluator(),
            adapterFactory);
    when(elementListAdapter.getView()).thenReturn(new LinearLayout(context));
    when(adapterFactory.createElementListAdapter(any(ElementList.class), eq(frameContext)))
        .thenReturn(elementListAdapter);
    when(adapterFactory.createTemplateAdapter(any(TemplateInvocation.class), eq(frameContext)))
        .thenReturn(templateAdapter);
    when(frameContext.getCurrentStyle()).thenReturn(styleProvider);
    when(frameContext.getAssetProvider()).thenReturn(assetProvider);
    when(frameModelBinder.bindFrame(any(Frame.class))).thenReturn(frameContext);

    frameAdapter = new FrameAdapter(context, adapterParameters, frameModelBinder);
  }

  @Test
  public void testCreate() {
    LinearLayout linearLayout = frameAdapter.getFrameContainer();
    assertThat(linearLayout).isNotNull();
    LayoutParams layoutParams = linearLayout.getLayoutParams();
    assertThat(layoutParams).isNotNull();
    assertThat(layoutParams.width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(layoutParams.height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testGetBoundAdapterForSlice() {
    Slice slice = getBaseSlice();

    ElementAdapter<?, ?> viewAdapter = frameAdapter.getBoundAdapterForSlice(slice, frameContext);
    assertThat(viewAdapter).isSameAs(elementListAdapter);

    slice = Slice.newBuilder().setTemplateSlice(TemplateInvocation.getDefaultInstance()).build();
    viewAdapter = frameAdapter.getBoundAdapterForSlice(slice, frameContext);
    assertThat(viewAdapter).isSameAs(templateAdapter);
  }

  @Test
  public void testBindModel_defaultDimensions() {
    Slice slice = getBaseSlice();
    Frame frame = Frame.newBuilder().addSlices(slice).build();
    when(elementListAdapter.getComputedWidthPx()).thenReturn(BoundViewAdapter.DIMENSION_NOT_SET);
    when(elementListAdapter.getComputedHeightPx()).thenReturn(BoundViewAdapter.DIMENSION_NOT_SET);

    frameAdapter.bindModel(frame, (ShardingControl) null);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);

    verify(elementListAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testBindModel_explicitDimensions() {
    Slice slice = getBaseSlice();
    Frame frame = Frame.newBuilder().addSlices(slice).build();
    int width = 123;
    int height = 456;
    when(elementListAdapter.getComputedWidthPx()).thenReturn(width);
    when(elementListAdapter.getComputedHeightPx()).thenReturn(height);

    frameAdapter.bindModel(frame, (ShardingControl) null);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);

    verify(elementListAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(width);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(height);
  }

  @Test
  public void testBackgroundColor() {
    Slice slice = getBaseSlice();
    Frame frame = Frame.newBuilder().addSlices(slice).build();
    frameAdapter.bindModel(frame, (ShardingControl) null);
    verify(frameContext).createBackground(styleProvider, context);
  }

  @Test
  public void testUnsetBackgroundIfNotDefined() {
    Slice slice = getBaseSlice();
    Frame frame = Frame.newBuilder().addSlices(slice).build();

    // Set background
    when(frameContext.createBackground(styleProvider, context))
        .thenReturn(new RoundedCornerColorDrawable(12345, 67));
    frameAdapter.bindModel(frame, (ShardingControl) null);
    assertThat(frameAdapter.getView().getBackground()).isNotNull();
    frameAdapter.unbindModel();

    // Re-bind and check that background is unset
    when(frameContext.createBackground(styleProvider, context)).thenReturn(null);
    frameAdapter.bindModel(frame, (ShardingControl) null);
    assertThat(frameAdapter.getView().getBackground()).isNull();
  }

  @Test
  public void testUnsetBackgroundIfCreateBackgroundFails() {
    Slice slice = getBaseSlice();
    Frame frame = Frame.newBuilder().addSlices(slice).build();

    // Set background
    when(frameContext.createBackground(styleProvider, context))
        .thenReturn(new RoundedCornerColorDrawable(12345, 67));
    frameAdapter.bindModel(frame, (ShardingControl) null);
    assertThat(frameAdapter.getView().getBackground()).isNotNull();
    frameAdapter.unbindModel();

    // Re-bind and check that background is unset
    when(frameContext.createBackground(styleProvider, context)).thenReturn(null);
    frameAdapter.bindModel(frame, (ShardingControl) null);
    assertThat(frameAdapter.getView().getBackground()).isNull();
  }

  @Test
  public void testRecycling_inlineSlice() {
    ElementList inlineSlice =
        ElementList.newBuilder().setGravityVertical(GravityVertical.GRAVITY_MIDDLE).build();
    Frame inlineSliceFrame =
        Frame.newBuilder().addSlices(Slice.newBuilder().setInlineSlice(inlineSlice)).build();
    frameAdapter.bindModel(inlineSliceFrame, (ShardingControl) null);
    verify(adapterFactory).createElementListAdapter(inlineSlice, frameContext);
    verify(elementListAdapter).bindModel(inlineSlice, frameContext);

    frameAdapter.unbindModel();
    verify(adapterFactory).releaseAdapter(elementListAdapter);
  }

  @Test
  public void testRecycling_templateSlice() {
    TemplateInvocation templateSlice =
        TemplateInvocation.newBuilder().setTemplateId("bread").build();
    Frame templateSliceFrame =
        Frame.newBuilder().addSlices(Slice.newBuilder().setTemplateSlice(templateSlice)).build();
    frameAdapter.bindModel(templateSliceFrame, (ShardingControl) null);
    verify(adapterFactory).createTemplateAdapter(templateSlice, frameContext);
    verify(templateAdapter).bindModel(templateSlice, frameContext);

    frameAdapter.unbindModel();
    verify(adapterFactory).releaseAdapter(templateAdapter);
  }

  private static Slice getBaseSlice() {
    return Slice.newBuilder()
        .setInlineSlice(ElementList.newBuilder().addElements(Element.getDefaultInstance()))
        .build();
  }
}
