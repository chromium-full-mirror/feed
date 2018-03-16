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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.piet.CustomElementAdapter.KeySupplier;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.search.now.ui.piet.BindingRefsProto.CustomBindingRef;
import com.google.search.now.ui.piet.ElementsProto.CustomElement;
import com.google.search.now.ui.piet.ElementsProto.CustomElementData;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.TextElement;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests of the {@link CustomElementAdapter}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CustomElementAdapterTest {

  private static final CustomElementData DUMMY_DATA = CustomElementData.getDefaultInstance();
  private static final CustomElement DEFAULT_MODEL =
      CustomElement.newBuilder().setCustomElementData(DUMMY_DATA).build();

  @Mock private AdapterParameters adapterParameters;
  @Mock private FrameContext frameContext;
  @Mock private CustomElementProvider customElementProvider;

  private Context context;
  private View customView;
  private CustomElementAdapter adapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    customView = new View(context);
    when(frameContext.getCustomElementProvider()).thenReturn(customElementProvider);
    when(frameContext.bindNewStyle(any(StyleIdsStack.class))).thenReturn(frameContext);
    when(customElementProvider.createCustomElement(DUMMY_DATA)).thenReturn(customView);
    adapter = new KeySupplier().getAdapter(context, adapterParameters);
  }

  @Test
  public void testCreate() {
    assertThat(adapter).isNotNull();
  }

  @Test
  public void testCreateAdapter_initializes() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getView()).isNotNull();
    assertThat(adapter.getKey()).isNotNull();
  }

  @Test
  public void testCreateAdapter_ignoresSubsequentCalls() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    View adapterView = adapter.getView();
    RecyclerKey adapterKey = adapter.getKey();

    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getView()).isSameAs(adapterView);
    assertThat(adapter.getKey()).isSameAs(adapterKey);
  }

  @Test
  public void testBindModel_data() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0)).isEqualTo(customView);
  }

  @Test
  public void testBindModel_binding() {
    CustomBindingRef bindingRef = CustomBindingRef.newBuilder().setBindingId("CUSTOM!").build();
    when(frameContext.getCustomElementFromBinding(bindingRef)).thenReturn(DUMMY_DATA);

    CustomElement model = CustomElement.newBuilder().setCustomBinding(bindingRef).build();

    adapter.createAdapter(model, frameContext);
    adapter.bindModel(model, frameContext);

    assertThat(adapter.getBaseView().getChildAt(0)).isEqualTo(customView);
  }

  @Test
  public void testBindModel_noContent() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(CustomElement.getDefaultInstance(), frameContext);

    verifyZeroInteractions(customElementProvider);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
  }

  @Test
  public void testUnbindModel() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    adapter.unbindModel();

    verify(customElementProvider).releaseCustomView(customView);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
  }

  @Test
  public void testUnbindModel_noChildren() {
    adapter.createAdapter(DEFAULT_MODEL, frameContext);

    adapter.unbindModel();

    verifyZeroInteractions(customElementProvider);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
  }

  @Test
  public void testUnbindModel_multipleChildren() {
    View customView2 = new View(context);
    when(customElementProvider.createCustomElement(DUMMY_DATA))
        .thenReturn(customView)
        .thenReturn(customView2);

    adapter.createAdapter(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);
    adapter.bindModel(DEFAULT_MODEL, frameContext);

    adapter.unbindModel();

    verify(customElementProvider).releaseCustomView(customView);
    verify(customElementProvider).releaseCustomView(customView2);
    assertThat(adapter.getBaseView().getChildCount()).isEqualTo(0);
  }

  @Test
  public void testGetModelFromElement() {
    CustomElement model =
        CustomElement.newBuilder()
            .setStyleReferences(StyleIdsStack.newBuilder().addStyleIds("custom"))
            .build();

    Element elementWithModel = Element.newBuilder().setCustomElement(model).build();
    assertThat(adapter.getModelFromElement(elementWithModel)).isSameAs(model);

    Element elementWithWrongModel =
        Element.newBuilder().setTextElement(TextElement.getDefaultInstance()).build();
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
        .contains("Missing CustomElement");

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
        .contains("Missing CustomElement");
  }
}
