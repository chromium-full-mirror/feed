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

package com.google.android.libraries.feed.piet.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.view.View;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.ActionsProto.Actions;
import com.google.search.now.ui.piet.BindingRefsProto.ActionsBindingRef;
import com.google.search.now.ui.piet.PietProto.Frame;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Tests of the {@link ViewUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest = "AndroidManifest.xml"
)
public class ViewUtilsTest {
  private final Context context = RuntimeEnvironment.application;

  private static final Frame DEFAULT_FRAME = Frame.newBuilder().setTag("Frame").build();
  private static final Action DEFAULT_ACTION = Action.getDefaultInstance();
  private static final Actions DEFAULT_ACTIONS =
      Actions.newBuilder().setOnClick(DEFAULT_ACTION).build();

  @Mock private ActionHandler mockActionHandler;
  @Mock private FrameContext mockFrameContext;
  @Mock private View.OnClickListener mockListener;

  private final View view = new View(context);

  @Before
  public void setUp() {
    initMocks(this);
    when(mockFrameContext.getFrame()).thenReturn(DEFAULT_FRAME);
  }

  @Test
  public void testDpToPx() {
    assertThat(ViewUtils.dpToPx(1000.0f, context)).isEqualTo(1000.0f);
  }

  @Test
  public void testSetOnClickActions_success() {
    ViewUtils.setOnClickActions(DEFAULT_ACTIONS, view, mockActionHandler, mockFrameContext);

    assertThat(view.hasOnClickListeners()).isTrue();

    view.callOnClick();
    verify(mockActionHandler).handleAction(DEFAULT_ACTION, DEFAULT_FRAME, null);
  }

  @Test
  public void testSetOnClickActions_successBinding() {
    ActionsBindingRef actionsBinding =
        ActionsBindingRef.newBuilder().setBindingId("ACTION!").build();
    when(mockFrameContext.getActionsFromBinding(actionsBinding)).thenReturn(DEFAULT_ACTIONS);

    ViewUtils.setOnClickActions(actionsBinding, view, mockActionHandler, mockFrameContext);

    assertThat(view.hasOnClickListeners()).isTrue();

    view.callOnClick();
    verify(mockActionHandler).handleAction(DEFAULT_ACTION, DEFAULT_FRAME, null);
  }

  @Test
  public void testSetOnClickActions_nullActionsClearsActions() {
    view.setOnClickListener(mockListener);
    assertThat(view.hasOnClickListeners()).isTrue();

    ViewUtils.setOnClickActions((Actions) null, view, mockActionHandler, mockFrameContext);

    assertThat(view.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testSetOnClickActions_nullActionsBindingClearsActions() {
    view.setOnClickListener(mockListener);
    assertThat(view.hasOnClickListeners()).isTrue();

    ViewUtils.setOnClickActions(
        (ActionsBindingRef) null, view, mockActionHandler, mockFrameContext);

    assertThat(view.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testSetOnClickActions_nullViewDoesNothing() {
    ViewUtils.setOnClickActions(DEFAULT_ACTIONS, null, mockActionHandler, mockFrameContext);
  }

  @Test
  public void testSetOnClickActions_noOnClickActionsDefinedClearsActions() {
    view.setOnClickListener(mockListener);
    assertThat(view.hasOnClickListeners()).isTrue();

    ViewUtils.setOnClickActions(
        Actions.getDefaultInstance(), view, mockActionHandler, mockFrameContext);

    assertThat(view.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testClearOnClickActions_success() {
    view.setOnClickListener(mockListener);
    assertThat(view.hasOnClickListeners()).isTrue();

    ViewUtils.clearOnClickActions(view);

    assertThat(view.hasOnClickListeners()).isFalse();
  }

  @Test
  public void testClearOnClickActions_nullViewDoesNothing() {
    ViewUtils.clearOnClickActions(null);
  }
}
