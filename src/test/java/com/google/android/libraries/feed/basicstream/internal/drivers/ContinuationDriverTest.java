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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.android.libraries.feed.common.testing.RunnableSubject.assertThatRunnable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelError.ErrorType;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.basicstream.internal.drivers.ContinuationDriver.CursorChangedListener;
import com.google.android.libraries.feed.basicstream.internal.logging.LoggingListener;
import com.google.android.libraries.feed.basicstream.internal.logging.StreamBasicLoggingApi;
import com.google.android.libraries.feed.basicstream.internal.scroll.ScrollRestorer;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.R;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.stream.SnackbarApi;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelCursor;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContinuationDriver}.j */
@RunWith(RobolectricTestRunner.class)
public class ContinuationDriverTest {

  private static final int POSITION = 1;

  @Mock private StreamBasicLoggingApi basicLoggingApi;
  @Mock private ModelChild modelChild;
  @Mock private ModelProvider modelProvider;
  @Mock private ModelToken modelToken;
  @Mock private ScrollRestorer scrollRestorer;
  @Mock private SnackbarApi snackbarApi;
  @Mock private ThreadUtils threadUtils;
  @Mock private CursorChangedListener cursorChangedListener;
  @Mock private ContinuationViewHolder continuationViewHolder;

  private Context context;
  private ContinuationDriver continuationDriver;
  private Configuration configuration = new Configuration.Builder().build();

  @Before
  public void setup() {
    initMocks(this);
    context = Robolectric.setupActivity(Activity.class);
    when(modelChild.getModelToken()).thenReturn(modelToken);

    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            configuration,
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);
    continuationDriver.initialize();
  }

  @Test
  public void testIsSynthetic() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    assertThat(continuationDriver.isSynthetic()).isFalse();

    when(modelToken.isSynthetic()).thenReturn(true);
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            configuration,
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    assertThat(continuationDriver.isSynthetic()).isTrue();
  }

  @Test
  public void testInitialize_nonSyntheticToken() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);
    continuationDriver.initialize();

    verify(modelToken).registerObserver(continuationDriver);
    verify(modelProvider, never()).handleToken(any(ModelToken.class));
  }

  @Test
  public void testInitialize_syntheticToken() {
    when(modelToken.isSynthetic()).thenReturn(true);
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);
    continuationDriver.initialize();

    // Call initialize again to make sure it doesn't duplicate work
    continuationDriver.initialize();

    verify(modelToken).registerObserver(continuationDriver);
    verify(modelProvider).handleToken(modelToken);

    continuationDriver.bind(continuationViewHolder);
    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), eq(true));
  }

  @Test
  public void testBind_immediatePaginationOn() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    continuationDriver.initialize();
    continuationDriver.bind(continuationViewHolder);
    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), /* showSpinner= */ eq(true));
  }

  @Test
  public void testBind_immediatePaginationOff() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, false).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    continuationDriver.initialize();
    continuationDriver.bind(continuationViewHolder);
    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), /* showSpinner= */ eq(false));
  }

  @Test
  public void testBind_noInitialization() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, false).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    assertThatRunnable(() -> continuationDriver.bind(continuationViewHolder))
        .throwsAnExceptionOfType(IllegalStateException.class);
  }

  @Test
  public void testShowsSpinnerAfterClick() {
    continuationDriver.bind(continuationViewHolder);

    continuationDriver.onClick(/* v= */ null);

    continuationDriver.unbind();

    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), /* showSpinner= */ eq(true));
  }

  @Test
  public void testOnDestroy() {
    continuationDriver.onDestroy();

    verify(modelToken).unregisterObserver(continuationDriver);
  }

  @Test
  public void testOnClick() {
    continuationDriver.bind(continuationViewHolder);
    continuationDriver.onClick(/* v= */ null);

    verify(continuationViewHolder).setShowSpinner(true);
    verify(modelProvider).handleToken(modelToken);
  }

  @Test
  public void testBind_listenerLogsMoreButtonClicked() {
    ArgumentCaptor<LoggingListener> captor = ArgumentCaptor.forClass(LoggingListener.class);
    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder).bind(eq(continuationDriver), captor.capture(), anyBoolean());
    captor.getValue().onContentClicked();

    verify(basicLoggingApi).onMoreButtonClicked(POSITION);
  }

  @Test
  public void testBind_listenerLogsMoreButtonViewed() {
    ArgumentCaptor<LoggingListener> captor = ArgumentCaptor.forClass(LoggingListener.class);
    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder).bind(eq(continuationDriver), captor.capture(), anyBoolean());
    captor.getValue().onViewVisible();

    verify(basicLoggingApi).onMoreButtonViewed(POSITION);
  }

  @Test
  public void testBind_listenerDoesNotLogViewTwice() {
    ArgumentCaptor<LoggingListener> captor = ArgumentCaptor.forClass(LoggingListener.class);
    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder).bind(eq(continuationDriver), captor.capture(), anyBoolean());
    captor.getValue().onViewVisible();
    reset(basicLoggingApi);
    captor.getValue().onViewVisible();

    verify(basicLoggingApi, never()).onMoreButtonViewed(anyInt());
  }

  @Test
  public void testBind_listenerDoesNotLogViewIfSpinnerShown() {
    ArgumentCaptor<LoggingListener> captor = ArgumentCaptor.forClass(LoggingListener.class);
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);
    continuationDriver.initialize();
    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder).bind(eq(continuationDriver), captor.capture(), anyBoolean());
    captor.getValue().onViewVisible();

    verify(basicLoggingApi, never()).onMoreButtonViewed(anyInt());
  }

  @Test
  public void testUnbind() {
    continuationDriver.bind(continuationViewHolder);
    assertThat(continuationDriver.isBound()).isTrue();
    continuationDriver.unbind();

    verify(continuationViewHolder).unbind();
    assertThat(continuationDriver.isBound()).isFalse();
  }

  @Test
  public void testOnTokenCompleted_checksMainThread() {
    continuationDriver.onTokenCompleted(new TokenCompleted(new FakeModelCursor.Builder().build()));

    verify(threadUtils).checkMainThread();
  }

  @Test
  public void testOnTokenCompleted_extractsModelChildren() {
    FakeModelCursor cursor =
        new FakeModelCursor.Builder().addCard().addCluster().addToken().build();

    continuationDriver.onTokenCompleted(new TokenCompleted(cursor));

    verify(cursorChangedListener).onNewChildren(modelChild, cursor.getModelChildren());
    verifyNoMoreInteractions(snackbarApi);
  }

  @Test
  public void testOnTokenCompleted_restoresScroll() {
    when(modelToken.isSynthetic()).thenReturn(false);

    continuationDriver.onTokenCompleted(new TokenCompleted(new FakeModelCursor.Builder().build()));

    verify(scrollRestorer).maybeRestoreScroll();
  }

  @Test
  public void testOnTokenCompleted_doesNotRestoreScrollForSyntheticToken() {
    when(modelToken.isSynthetic()).thenReturn(true);

    continuationDriver.onTokenCompleted(new TokenCompleted(new FakeModelCursor.Builder().build()));

    verify(scrollRestorer, never()).maybeRestoreScroll();
  }

  @Test
  public void testOnTokenCompleted_createsSnackbar_whenCursorReturnsEmptyPage() {
    continuationDriver.onTokenCompleted(new TokenCompleted(new FakeModelCursor.Builder().build()));

    verify(snackbarApi).show(context.getString(R.string.snackbar_fetch_no_new_suggestions));
    verify(cursorChangedListener).onNewChildren(modelChild, Collections.emptyList());
  }

  @Test
  public void testOnTokenCompleted_createsSnackbar_whenCursorJustReturnsToken() {
    FakeModelCursor cursor = new FakeModelCursor.Builder().addToken().build();
    continuationDriver.onTokenCompleted(new TokenCompleted(cursor));

    verify(snackbarApi).show(context.getString(R.string.snackbar_fetch_no_new_suggestions));
    verify(cursorChangedListener).onNewChildren(modelChild, cursor.getModelChildren());
  }

  @Test
  public void testOnError() {
    continuationDriver.bind(continuationViewHolder);

    continuationDriver.onError(new ModelError(ErrorType.PAGINATION_ERROR, null));

    verify(continuationViewHolder).setShowSpinner(false);
    verify(snackbarApi).show(context.getString(R.string.snackbar_fetch_failed));
  }

  @Test
  public void testOnError_hidesSpinnerAfterBeingBound() {
    continuationDriver =
        new ContinuationDriver(
            basicLoggingApi,
            new Configuration.Builder().put(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, true).build(),
            context,
            cursorChangedListener,
            modelChild,
            modelProvider,
            POSITION,
            scrollRestorer,
            snackbarApi,
            threadUtils);

    continuationDriver.initialize();
    continuationDriver.bind(continuationViewHolder);
    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), eq(true));

    continuationDriver.unbind();
    continuationDriver.onError(new ModelError(ErrorType.PAGINATION_ERROR, null));
    continuationDriver.bind(continuationViewHolder);

    verify(continuationViewHolder)
        .bind(eq(continuationDriver), any(LoggingListener.class), eq(false));
    verify(continuationViewHolder, never()).setShowSpinner(anyBoolean());
  }
}
