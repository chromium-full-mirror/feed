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

import static com.google.android.libraries.feed.piet.StyleProvider.DEFAULT_STYLE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.piet.DebugLogger.MessageType;
import com.google.android.libraries.feed.piet.TemplateBinder.TemplateAdapterModel;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.android.libraries.feed.piet.ui.RoundedCornerColorDrawable;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.ElementsProto.BindingContext;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ElementsProto.GravityVertical;
import com.google.search.now.ui.piet.ElementsProto.GridRow;
import com.google.search.now.ui.piet.ElementsProto.TemplateInvocation;
import com.google.search.now.ui.piet.PietAndroidSupport.ShardingControl;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Stylesheet;
import com.google.search.now.ui.piet.PietProto.Template;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link FrameAdapter}. */
@RunWith(RobolectricTestRunner.class)
public class FrameAdapterTest {
  private static final Content DEFAULT_CONTENT =
      Content.newBuilder()
          .setElement(
              Element.newBuilder()
                  .setElementList(
                      ElementList.newBuilder()
                          .addContents(
                              Content.newBuilder().setElement(Element.getDefaultInstance()))))
          .build();

  @Mock private AssetProvider assetProvider;
  @Mock private ElementAdapterFactory adapterFactory;
  @Mock private TemplateBinder templateBinder;
  @Mock private FrameContext frameContext;
  @Mock private DebugBehavior debugBehavior;
  @Mock private DebugLogger debugLogger;
  @Mock private StyleProvider styleProvider;
  @Mock private ElementAdapter<? extends View, ?> elementAdapter;
  @Mock private ElementAdapter<? extends View, ?> templateAdapter;
  @Mock private CustomElementProvider customElementProvider;
  @Mock private ActionHandler actionHandler;
  @Mock private HostProviders hostProviders;

  @Captor private ArgumentCaptor<LayoutParams> layoutParamsCaptor;

  private Context context;
  private List<PietSharedState> pietSharedStates;
  private AdapterParameters adapterParameters;

  private FrameAdapter frameAdapter;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    adapterParameters =
        new AdapterParameters(
            null,
            Suppliers.of(new FrameLayout(context)),
            new ParameterizedTextEvaluator(),
            adapterFactory,
            templateBinder,
            hostProviders);
    when(elementAdapter.getView()).thenReturn(new LinearLayout(context));
    when(templateAdapter.getView()).thenReturn(new LinearLayout(context));
    doReturn(elementAdapter)
        .when(adapterFactory)
        .createAdapterForElement(any(Element.class), eq(frameContext));
    doReturn(templateAdapter)
        .when(templateBinder)
        .createAndBindTemplateAdapter(any(TemplateAdapterModel.class), eq(frameContext));
    when(frameContext.getCurrentStyle()).thenReturn(styleProvider);
    when(hostProviders.getAssetProvider()).thenReturn(assetProvider);
    when(frameContext.reportError(eq(MessageType.ERROR), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              throw new RuntimeException((String) invocationOnMock.getArguments()[1]);
            });
    when(frameContext.getDebugLogger()).thenReturn(debugLogger);
    when(frameContext.getDebugBehavior()).thenReturn(DebugBehavior.VERBOSE);

    pietSharedStates = new ArrayList<>();
    frameAdapter =
        new FrameAdapter(context, adapterParameters, actionHandler, debugBehavior) {
          @Override
          FrameContext createFrameContext(Frame frame, List<PietSharedState> pietSharedStates) {
            return frameContext;
          }
        };
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
  public void testGetBoundAdaptersForContent() {
    Content content = DEFAULT_CONTENT;

    String templateId = "loaf";
    when(frameContext.getTemplate(templateId)).thenReturn(Template.getDefaultInstance());
    List<ElementAdapter<?, ?>> viewAdapters =
        frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).containsExactly(elementAdapter);

    content =
        Content.newBuilder()
            .setTemplateInvocation(TemplateInvocation.newBuilder().setTemplateId(templateId))
            .build();
    viewAdapters = frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).isEmpty();

    content =
        Content.newBuilder()
            .setTemplateInvocation(
                TemplateInvocation.newBuilder()
                    .setTemplateId(templateId)
                    .addBindingContexts(BindingContext.getDefaultInstance()))
            .build();
    viewAdapters = frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).containsExactly(templateAdapter);

    content =
        Content.newBuilder()
            .setTemplateInvocation(
                TemplateInvocation.newBuilder()
                    .setTemplateId(templateId)
                    .addBindingContexts(
                        BindingContext.newBuilder()
                            .addBindingValues(BindingValue.newBuilder().setBindingId("1")))
                    .addBindingContexts(
                        BindingContext.newBuilder()
                            .addBindingValues(BindingValue.newBuilder().setBindingId("2")))
                    .addBindingContexts(
                        BindingContext.newBuilder()
                            .addBindingValues(BindingValue.newBuilder().setBindingId("3"))))
            .build();
    viewAdapters = frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).containsExactly(templateAdapter, templateAdapter, templateAdapter);
    verify(templateBinder)
        .createAndBindTemplateAdapter(
            new TemplateAdapterModel(
                Template.getDefaultInstance(),
                content.getTemplateInvocation().getBindingContexts(0)),
            frameContext);
    verify(templateBinder)
        .createAndBindTemplateAdapter(
            new TemplateAdapterModel(
                Template.getDefaultInstance(),
                content.getTemplateInvocation().getBindingContexts(1)),
            frameContext);
    verify(templateBinder)
        .createAndBindTemplateAdapter(
            new TemplateAdapterModel(
                Template.getDefaultInstance(),
                content.getTemplateInvocation().getBindingContexts(2)),
            frameContext);

    verify(frameContext, never()).reportError(anyInt(), anyString());
  }

  /** This test sets up all real objects to ensure that real adapters are bound and unbound, etc. */
  @Test
  public void testBindAndUnbind_respectsAdapterLifecycle() {
    String templateId = "template";
    ElementList defaultList =
        ElementList.newBuilder()
            .addContents(
                Content.newBuilder()
                    .setElement(
                        Element.newBuilder().setElementList(ElementList.getDefaultInstance())))
            .addContents(
                Content.newBuilder()
                    .setElement(Element.newBuilder().setGridRow(GridRow.getDefaultInstance())))
            .build();
    AdapterParameters adapterParameters =
        new AdapterParameters(context, Suppliers.of(new LinearLayout(context)), hostProviders);
    PietSharedState pietSharedState =
        PietSharedState.newBuilder()
            .addTemplates(
                Template.newBuilder()
                    .setTemplateId(templateId)
                    .setElement(Element.newBuilder().setElementList(defaultList)))
            .build();
    pietSharedStates.add(pietSharedState);
    FrameContext frameContext =
        FrameContext.createFrameContext(
            Frame.newBuilder().setStylesheet(Stylesheet.getDefaultInstance()).build(),
            DEFAULT_STYLE,
            pietSharedStates,
            debugBehavior,
            new DebugLogger(),
            actionHandler,
            new HostBindingProvider());

    frameAdapter = new FrameAdapter(context, adapterParameters, actionHandler, debugBehavior);

    Content content =
        Content.newBuilder().setElement(Element.newBuilder().setElementList(defaultList)).build();

    List<ElementAdapter<?, ?>> viewAdapters =
        frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).hasSize(1);
    ElementAdapter<?, ?> viewAdapter = viewAdapters.get(0);
    assertThat(viewAdapter.getModel()).isEqualTo(content.getElement().getElementList());
    frameAdapter.bindModel(Frame.newBuilder().addContents(content).build(), null, pietSharedStates);
    assertThat(frameContext.getDebugLogger().getMessages(MessageType.ERROR)).isEmpty();
    frameAdapter.unbindModel();

    content =
        Content.newBuilder()
            .setTemplateInvocation(
                TemplateInvocation.newBuilder()
                    .setTemplateId(templateId)
                    .addBindingContexts(BindingContext.getDefaultInstance()))
            .build();
    viewAdapters = frameAdapter.getBoundAdaptersForContent(content, frameContext);
    assertThat(viewAdapters).hasSize(1);
    viewAdapter = viewAdapters.get(0);
    assertThat(viewAdapter.getModel()).isEqualTo(defaultList);
    frameAdapter.bindModel(Frame.newBuilder().addContents(content).build(), null, pietSharedStates);
    assertThat(frameContext.getDebugLogger().getMessages(MessageType.ERROR)).isEmpty();
    frameAdapter.unbindModel();
  }

  @Test
  public void testBindModel_defaultDimensions() {
    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();
    when(elementAdapter.getComputedWidthPx()).thenReturn(ElementAdapter.DIMENSION_NOT_SET);
    when(elementAdapter.getComputedHeightPx()).thenReturn(ElementAdapter.DIMENSION_NOT_SET);

    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);

    verify(elementAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(LayoutParams.MATCH_PARENT);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(LayoutParams.WRAP_CONTENT);
  }

  @Test
  public void testBindModel_explicitDimensions() {
    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();
    int width = 123;
    int height = 456;
    when(elementAdapter.getComputedWidthPx()).thenReturn(width);
    when(elementAdapter.getComputedHeightPx()).thenReturn(height);

    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);

    verify(elementAdapter).setLayoutParams(layoutParamsCaptor.capture());
    assertThat(layoutParamsCaptor.getValue().width).isEqualTo(width);
    assertThat(layoutParamsCaptor.getValue().height).isEqualTo(height);
  }

  @Test
  public void testAdapterWithActions() {
    String templateId = "lights-camera";
    String actionBindingId = "action";
    ActionsBindingRef actionBinding =
        ActionsBindingRef.newBuilder().setBindingId(actionBindingId).build();
    Template templateWithActions =
        Template.newBuilder()
            .setTemplateId(templateId)
            .setElement(
                Element.newBuilder()
                    .setActionsBinding(actionBinding)
                    .setElementList(ElementList.getDefaultInstance()))
            .build();
    BindingValue actionBindingValue =
        BindingValue.newBuilder()
            .setBindingId(actionBindingId)
            .setActions(Actions.newBuilder().setOnClickAction(Action.getDefaultInstance()))
            .build();
    Content content =
        Content.newBuilder()
            .setTemplateInvocation(
                TemplateInvocation.newBuilder()
                    .setTemplateId(templateId)
                    .addBindingContexts(
                        BindingContext.newBuilder().addBindingValues(actionBindingValue)))
            .build();
    Frame frame = Frame.newBuilder().addContents(content).build();
    PietSharedState pietSharedState =
        PietSharedState.newBuilder().addTemplates(templateWithActions).build();

    pietSharedStates.add(pietSharedState);

    AdapterParameters parameters =
        new AdapterParameters(context, Suppliers.of(null), hostProviders);
    FrameAdapter frameAdapter = new FrameAdapter(context, parameters, actionHandler, debugBehavior);

    frameAdapter.bindModel(frame, null, pietSharedStates);

    assertThat(frameAdapter.getFrameContainer().getChildCount()).isEqualTo(1);
    assertThat(frameAdapter.getFrameContainer().getChildAt(0).hasOnClickListeners()).isTrue();
  }

  @Test
  public void testBackgroundColor() {
    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    styleProvider.createBackground(context, assetProvider);
  }

  @Test
  public void testUnsetBackgroundIfNotDefined() {
    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();

    // Set background
    when(styleProvider.createBackground(context, assetProvider))
        .thenReturn(new RoundedCornerColorDrawable(12345));
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    assertThat(frameAdapter.getView().getBackground()).isNotNull();
    frameAdapter.unbindModel();

    // Re-bind and check that background is unset
    when(styleProvider.createBackground(context, assetProvider)).thenReturn(null);
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    assertThat(frameAdapter.getView().getBackground()).isNull();
  }

  @Test
  public void testUnsetBackgroundIfCreateBackgroundFails() {
    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();

    // Set background
    when(styleProvider.createBackground(context, assetProvider))
        .thenReturn(new RoundedCornerColorDrawable(12345));
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    assertThat(frameAdapter.getView().getBackground()).isNotNull();
    frameAdapter.unbindModel();

    // Re-bind and check that background is unset
    when(styleProvider.createBackground(context, assetProvider)).thenReturn(null);
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);
    assertThat(frameAdapter.getView().getBackground()).isNull();
  }

  @Test
  public void testRecycling_inlineSlice() {
    Element element =
        Element.newBuilder()
            .setGravityVertical(GravityVertical.GRAVITY_MIDDLE)
            .setElementList(ElementList.getDefaultInstance())
            .build();
    Frame inlineSliceFrame =
        Frame.newBuilder().addContents(Content.newBuilder().setElement(element)).build();
    frameAdapter.bindModel(inlineSliceFrame, (ShardingControl) null, pietSharedStates);
    verify(adapterFactory).createAdapterForElement(element, frameContext);
    verify(elementAdapter).bindModel(element, frameContext);

    frameAdapter.unbindModel();
    verify(adapterFactory).releaseAdapter(elementAdapter);
  }

  @Test
  public void testRecycling_templateSlice() {
    String templateId = "bread";
    Template template = Template.newBuilder().setTemplateId(templateId).build();
    when(frameContext.getTemplate(templateId)).thenReturn(template);
    BindingContext bindingContext =
        BindingContext.newBuilder()
            .addBindingValues(BindingValue.newBuilder().setBindingId("grain"))
            .build();
    TemplateInvocation templateSlice =
        TemplateInvocation.newBuilder()
            .setTemplateId(templateId)
            .addBindingContexts(bindingContext)
            .build();
    Frame templateSliceFrame =
        Frame.newBuilder()
            .addContents(Content.newBuilder().setTemplateInvocation(templateSlice))
            .build();
    frameAdapter.bindModel(templateSliceFrame, (ShardingControl) null, pietSharedStates);
    TemplateAdapterModel model = new TemplateAdapterModel(template, bindingContext);
    verify(templateBinder).createAndBindTemplateAdapter(model, frameContext);

    frameAdapter.unbindModel();
    verify(adapterFactory).releaseAdapter(templateAdapter);
  }

  @Test
  public void testErrorViewReporting() {
    View warningView = new View(context);
    View errorView = new View(context);
    when(debugLogger.getReportView(MessageType.WARNING, context)).thenReturn(warningView);
    when(debugLogger.getReportView(MessageType.ERROR, context)).thenReturn(errorView);

    Frame frame = Frame.newBuilder().addContents(DEFAULT_CONTENT).build();

    // Errors silenced by debug behavior
    when(frameContext.getDebugBehavior()).thenReturn(DebugBehavior.SILENT);
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);
    verify(debugLogger, never()).getReportView(anyInt(), any());

    frameAdapter.unbindModel();

    // Errors displayed in extra views
    when(frameContext.getDebugBehavior()).thenReturn(DebugBehavior.VERBOSE);
    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(3);
    assertThat(frameAdapter.getView().getChildAt(1)).isSameAs(errorView);
    assertThat(frameAdapter.getView().getChildAt(2)).isSameAs(warningView);

    frameAdapter.unbindModel();

    // No errors
    when(debugLogger.getReportView(MessageType.WARNING, context)).thenReturn(null);
    when(debugLogger.getReportView(MessageType.ERROR, context)).thenReturn(null);

    frameAdapter.bindModel(frame, (ShardingControl) null, pietSharedStates);

    assertThat(frameAdapter.getView().getChildCount()).isEqualTo(1);
  }
}
