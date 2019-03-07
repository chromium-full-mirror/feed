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

package com.google.android.libraries.feed.basicstream.internal.viewholders;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.actionparser.ActionSource;
import com.google.android.libraries.feed.api.actionparser.ActionSourceConverter;
import com.google.android.libraries.feed.basicstream.internal.actions.StreamActionApiImpl;
import com.google.android.libraries.feed.basicstream.internal.viewholders.PietViewHolder.PietViewActionScrollListener;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.stream.CardConfiguration;
import com.google.android.libraries.feed.piet.FrameAdapter;
import com.google.android.libraries.feed.piet.PietManager;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.ActionHandler.ActionType;
import com.google.android.libraries.feed.piet.host.EventLogger;
import com.google.android.libraries.feed.sharedstream.logging.LoggingListener;
import com.google.android.libraries.feed.sharedstream.logging.VisibilityMonitor;
import com.google.android.libraries.feed.sharedstream.piet.PietEventLogger;
import com.google.android.libraries.feed.sharedstream.scroll.StreamScrollMonitor;
import com.google.android.libraries.feed.testing.host.stream.FakeCardConfiguration;
import com.google.search.now.ui.action.FeedActionPayloadProto.FeedActionPayload;
import com.google.search.now.ui.action.FeedActionProto.FeedAction;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ErrorsProto.ErrorCode;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link PietViewHolder}. */
@RunWith(RobolectricTestRunner.class)
public class PietViewHolderTest {
  private static final Action ACTION = Action.getDefaultInstance();
  private static final Frame FRAME = Frame.newBuilder().build();
  private static final FeedActionPayload SWIPE_ACTION =
      FeedActionPayload.newBuilder()
          .setExtension(FeedAction.feedActionExtension, FeedAction.getDefaultInstance())
          .build();
  private static final String VE_LOGGING_TOKEN = "token";

  private CardConfiguration cardConfiguration;
  @Mock private FrameAdapter frameAdapter;
  @Mock private ActionParser actionParser;
  @Mock private PietManager pietManager;
  @Mock private StreamActionApiImpl streamActionApi;
  @Mock private LoggingListener loggingListener;
  @Mock private VisibilityMonitor visibilityMonitor;
  @Mock private StreamScrollMonitor streamScrollMonitor;
  @Mock private BasicLoggingApi basicLoggingApi;

  private ActionHandler actionHandler;
  private Configuration configuration;
  private PietViewHolder pietViewHolder;
  private FrameLayout frameLayout;
  private View view;
  private View viewport;
  private final List<PietSharedState> pietSharedStates = new ArrayList<>();
  private Context context;

  @Before
  public void setUp() {
    initMocks(this);
    cardConfiguration = new FakeCardConfiguration();
    context = Robolectric.buildActivity(Activity.class).get();
    frameLayout = new FrameLayout(context);
    frameLayout.setLayoutParams(new MarginLayoutParams(100, 100));
    view = new TextView(context);
    viewport = new FrameLayout(context);
    when(pietManager.createPietFrameAdapter(
            Mockito.<Supplier<ViewGroup>>any(),
            any(ActionHandler.class),
            any(EventLogger.class),
            eq(context)))
        .thenReturn(frameAdapter);
    when(frameAdapter.getFrameContainer()).thenReturn(new LinearLayout(context));

    configuration = new Configuration.Builder().build();
    pietViewHolder =
        new PietViewHolder(
            cardConfiguration,
            frameLayout,
            pietManager,
            streamScrollMonitor,
            viewport,
            context,
            configuration,
            new PietEventLogger(basicLoggingApi)) {
          @Override
          VisibilityMonitor createVisibilityMonitor(View view, Configuration configuration) {
            return visibilityMonitor;
          }
        };

    ArgumentCaptor<ActionHandler> captor = ArgumentCaptor.forClass(ActionHandler.class);
    verify(pietManager)
        .createPietFrameAdapter(
            Mockito.<Supplier<ViewGroup>>any(),
            captor.capture(),
            any(EventLogger.class),
            any(Context.class));
    actionHandler = captor.getValue();
  }

  @Test
  public void testCardViewSetup() {
    assertThat(frameLayout.getId()).isEqualTo(R.id.feed_content_card);
  }

  @Test
  public void testBind_clearsPadding() {
    frameLayout.setPadding(1, 2, 3, 4);

    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    assertThat(frameLayout.getPaddingLeft()).isEqualTo(0);
    assertThat(frameLayout.getPaddingRight()).isEqualTo(0);
    assertThat(frameLayout.getPaddingTop()).isEqualTo(0);
    assertThat(frameLayout.getPaddingBottom()).isEqualTo(0);
  }

  @Test
  public void testBind_setsBackground() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    assertThat(((ColorDrawable) frameLayout.getBackground()).getColor())
        .isEqualTo(((ColorDrawable) cardConfiguration.getCardBackground()).getColor());
  }

  @Test
  public void testBind_setsMargins() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    MarginLayoutParams marginLayoutParams =
        (MarginLayoutParams) pietViewHolder.itemView.getLayoutParams();
    assertThat(marginLayoutParams.bottomMargin).isEqualTo(cardConfiguration.getCardBottomMargin());
    assertThat(marginLayoutParams.leftMargin).isEqualTo(cardConfiguration.getCardStartMargin());
    assertThat(marginLayoutParams.rightMargin).isEqualTo(cardConfiguration.getCardEndMargin());
  }

  @Test
  public void testBind_bindsModel() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    verify(frameAdapter)
        .bindModel(Frame.getDefaultInstance(), 0, /* shardingControl= */ null, pietSharedStates);
  }

  @Test
  public void testBind_onlyBindsOnce() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    verify(frameAdapter)
        .bindModel(Frame.getDefaultInstance(), 0, /* shardingControl= */ null, pietSharedStates);
    verify(frameAdapter).getFrameContainer();

    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    verifyNoMoreInteractions(frameAdapter);
  }

  @Test
  public void testBind_setsListener() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    verify(visibilityMonitor).setListener(loggingListener);
  }

  @Test
  public void testBind_setsScrollListener() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    verify(streamScrollMonitor).addScrollListener(any(PietViewActionScrollListener.class));
  }

  @Test
  public void testUnbind() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    pietViewHolder.unbind();
    InOrder inOrder = Mockito.inOrder(frameAdapter);

    inOrder
        .verify(frameAdapter)
        .bindModel(Frame.getDefaultInstance(), 0, /* shardingControl= */ null, pietSharedStates);
    inOrder.verify(frameAdapter).unbindModel();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testUnbind_setsListenerToNull() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    pietViewHolder.unbind();

    verify(visibilityMonitor).setListener(null);
  }

  @Test
  public void testBind_unsetsScrollListener() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    pietViewHolder.unbind();

    ArgumentCaptor<PietViewActionScrollListener> captor =
        ArgumentCaptor.forClass(PietViewActionScrollListener.class);
    verify(streamScrollMonitor).addScrollListener(captor.capture());
    verify(streamScrollMonitor).removeScrollListener(captor.getValue());
  }

  @Test
  public void testSwipe_cantSwipeWithDefaultInstance() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    assertThat(pietViewHolder.canSwipe()).isFalse();
  }

  @Test
  public void testSwipe_canSwipeWithNonDefaultInstance() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        SWIPE_ACTION,
        loggingListener,
        actionParser);

    assertThat(pietViewHolder.canSwipe()).isTrue();
  }

  @Test
  public void testSwipeToDismissPerformed() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        SWIPE_ACTION,
        loggingListener,
        actionParser);

    pietViewHolder.onSwiped();

    verify(loggingListener).onContentSwiped();
    verify(actionParser)
        .parseFeedActionPayload(
            SWIPE_ACTION, streamActionApi, pietViewHolder.itemView, ActionSource.SWIPE);
  }

  @Test
  public void testActionHandler_logsClickOnClickAction() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    actionHandler.handleAction(ACTION, ActionType.CLICK, FRAME, view, VE_LOGGING_TOKEN);

    verify(loggingListener).onContentClicked();
    verify(actionParser)
        .parseAction(ACTION, streamActionApi, view, VE_LOGGING_TOKEN, ActionSource.CLICK);
  }

  @Test
  public void testActionHandler_doesNotLogClickOnLongClickAction() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    actionHandler.handleAction(ACTION, ActionType.LONG_CLICK, FRAME, view, VE_LOGGING_TOKEN);

    verify(loggingListener, never()).onContentClicked();
    verify(actionParser)
        .parseAction(ACTION, streamActionApi, view, VE_LOGGING_TOKEN, ActionSource.LONG_CLICK);
  }

  @Test
  public void testActionHandler_doesNotLogClickOnViewAction() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    actionHandler.handleAction(ACTION, ActionType.VIEW, FRAME, view, VE_LOGGING_TOKEN);

    verify(loggingListener, never()).onContentClicked();
    verify(actionParser)
        .parseAction(ACTION, streamActionApi, view, VE_LOGGING_TOKEN, ActionSource.VIEW);
  }

  @Test
  public void testHideActionsOnUnbind() {
    pietViewHolder.bind(
        Frame.getDefaultInstance(),
        pietSharedStates,
        streamActionApi,
        FeedActionPayload.getDefaultInstance(),
        loggingListener,
        actionParser);

    doAnswer(
            invocation -> {
              actionHandler.handleAction(
                  Action.getDefaultInstance(),
                  ActionType.VIEW,
                  Frame.getDefaultInstance(),
                  view,
                  null);
              return null;
            })
        .when(frameAdapter)
        .unbindModel();

    pietViewHolder.unbind();

    verify(actionParser)
        .parseAction(
            Action.getDefaultInstance(),
            streamActionApi,
            view,
            null,
            ActionSourceConverter.convertPietAction(ActionType.VIEW));
    // Does not crash
  }

  @Test
  public void testPietError_loggedToHost() {
    ArgumentCaptor<EventLogger> pietEventLoggerCaptor = ArgumentCaptor.forClass(EventLogger.class);

    verify(pietManager)
        .createPietFrameAdapter(
            Mockito.<Supplier<ViewGroup>>any(),
            eq(actionHandler),
            pietEventLoggerCaptor.capture(),
            eq(context));

    pietEventLoggerCaptor
        .getValue()
        .logEvents(Collections.singletonList(ErrorCode.ERR_MISSING_BINDING_VALUE));

    verify(basicLoggingApi)
        .onPietFrameRenderingEvent(
            Collections.singletonList(ErrorCode.ERR_MISSING_BINDING_VALUE.getNumber()));
  }
}
