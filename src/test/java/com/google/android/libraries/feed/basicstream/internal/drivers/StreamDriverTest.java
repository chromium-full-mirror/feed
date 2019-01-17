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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import com.google.android.libraries.feed.api.actionmanager.ActionManager;
import com.google.android.libraries.feed.api.actionparser.ActionParserFactory;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.knowncontent.KnownContentApi;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.RemoveTrackingFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.State;
import com.google.android.libraries.feed.api.stream.ContentChangedListener;
import com.google.android.libraries.feed.basicstream.internal.drivers.StreamDriver.StreamContentListener;
import com.google.android.libraries.feed.basicstream.internal.drivers.testing.FakeFeatureDriver;
import com.google.android.libraries.feed.basicstream.internal.drivers.testing.FakeLeafFeatureDriver;
import com.google.android.libraries.feed.basicstream.internal.scroll.ScrollRestorer;
import com.google.android.libraries.feed.common.testing.FakeClock;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.stream.SnackbarApi;
import com.google.android.libraries.feed.sharedstream.contextmenumanager.ContextMenuManager;
import com.google.android.libraries.feed.sharedstream.offlinemonitor.StreamOfflineMonitor;
import com.google.android.libraries.feed.sharedstream.removetrackingfactory.StreamRemoveTrackingFactory;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelChild;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelCursor;
import com.google.android.libraries.feed.testing.modelprovider.FakeModelFeature;
import com.google.android.libraries.feed.testing.modelprovider.FeatureChangeBuilder;
import com.google.common.collect.Lists;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Cluster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link StreamDriver}. */
@RunWith(RobolectricTestRunner.class)
public class StreamDriverTest {

  private StreamDriverForTest streamDriver;

  private static final ModelFeature UNBOUND_CARD =
      new FakeModelFeature.Builder()
          .setStreamFeature(StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build())
          .build();

  @Mock private ActionApi actionApi;
  @Mock private ActionManager actionManager;
  @Mock private ActionParserFactory actionParserFactory;
  @Mock private ModelFeature streamFeature;
  @Mock private ModelProvider modelProvider;
  @Mock private ContinuationDriver continuationDriver;
  @Mock private NoContentDriver noContentDriver;
  @Mock private StreamContentListener contentListener;
  @Mock private ContentChangedListener contentChangedListener;
  @Mock private SnackbarApi snackbarApi;
  @Mock private ScrollRestorer scrollRestorer;
  @Mock private BasicLoggingApi basicLoggingApi;
  @Mock private KnownContentApi knownContentApi;
  @Mock private StreamOfflineMonitor streamOfflineMonitor;

  @Captor private ArgumentCaptor<List<LeafFeatureDriver>> featureDriversCaptor;

  private Configuration configuration = new Configuration.Builder().build();
  private Context context;
  private Clock clock;
  private ThreadUtils threadUtils;

  private static FakeModelChild createFakeClusterModelChild() {
    return new FakeModelChild.Builder()
        .setModelFeature(
            new FakeModelFeature.Builder()
                .setStreamFeature(
                    StreamFeature.newBuilder().setCluster(Cluster.getDefaultInstance()).build())
                .build())
        .build();
  }

  @Before
  public void setup() {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    clock = new FakeClock();

    when(continuationDriver.getLeafFeatureDriver()).thenReturn(continuationDriver);
    when(noContentDriver.getLeafFeatureDriver()).thenReturn(noContentDriver);
    when(modelProvider.getRootFeature()).thenReturn(streamFeature);
    when(modelProvider.getCurrentState()).thenReturn(State.READY);
    threadUtils = new ThreadUtils();

    streamDriver = createNonRestoringStreamDriver();

    streamDriver.setStreamContentListener(contentListener);
  }

  @Test
  public void testConstruction() {
    ArgumentCaptor<RemoveTrackingFactory> removeTrackingFactoryArgumentCaptor =
        ArgumentCaptor.forClass(RemoveTrackingFactory.class);

    verify(modelProvider).enableRemoveTracking(removeTrackingFactoryArgumentCaptor.capture());

    assertThat(removeTrackingFactoryArgumentCaptor.getValue())
        .isInstanceOf(StreamRemoveTrackingFactory.class);
  }

  @Test
  public void testBuildChildren() {
    when(streamFeature.getCursor())
        .thenReturn(new FakeModelCursor.Builder().addCard().addCluster().addToken().build());

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(3);

    assertThat(leafFeatureDrivers.get(0)).isEqualTo(getLeafFeatureDriverFromCard(0));
    assertThat(leafFeatureDrivers.get(1)).isEqualTo(getLeafFeatureDriverFromCluster(1));
    assertThat(leafFeatureDrivers.get(2)).isEqualTo(continuationDriver);

    verify(continuationDriver).initialize();
    verify(streamOfflineMonitor).requestOfflineStatusForNewContent();
  }

  @Test
  public void testBuildChildren_unboundContent() {
    when(streamFeature.getCursor())
        .thenReturn(new FakeModelCursor.Builder().addChild(UNBOUND_CARD).addCard().build());

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(1);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(getLeafFeatureDriverFromCard(1));
  }

  @Test
  public void testBuildChildren_initializing() {
    when(modelProvider.getRootFeature()).thenReturn(null);
    when(modelProvider.getCurrentState()).thenReturn(State.INITIALIZING);
    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(1);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0)).isInstanceOf(ZeroStateDriver.class);
  }

  @Test
  public void testMaybeRestoreScroll() {
    streamDriver = createRestoringStreamDriver();

    when(streamFeature.getCursor()).thenReturn(new FakeModelCursor.Builder().addCard().build());

    streamDriver.getLeafFeatureDrivers();

    streamDriver.maybeRestoreScroll();

    verify(scrollRestorer).maybeRestoreScroll();
  }

  @Test
  public void testMaybeRestoreScroll_withToken() {
    streamDriver = createRestoringStreamDriver();

    when(streamFeature.getCursor())
        .thenReturn(new FakeModelCursor.Builder().addCard().addCluster().addToken().build());

    streamDriver.getLeafFeatureDrivers();

    streamDriver.maybeRestoreScroll();

    // Should restore scroll if a non-synthetic token is last
    verify(scrollRestorer).maybeRestoreScroll();
  }

  @Test
  public void testMaybeRestoreScroll_withSyntheticToken() {
    streamDriver = createRestoringStreamDriver();

    when(streamFeature.getCursor())
        .thenReturn(
            new FakeModelCursor.Builder().addCard().addCluster().addSyntheticToken().build());
    when(continuationDriver.isSynthetic()).thenReturn(true);

    streamDriver.getLeafFeatureDrivers();

    streamDriver.maybeRestoreScroll();

    // Should never restore scroll if a synthetic token is last
    verify(scrollRestorer, never()).maybeRestoreScroll();
  }

  @Test
  public void testMaybeRestoreScroll_notRestoring_doesNotScroll() {
    streamDriver = createNonRestoringStreamDriver();

    when(streamFeature.getCursor())
        .thenReturn(
            new FakeModelCursor.Builder().addCard().addCluster().addSyntheticToken().build());

    streamDriver.getLeafFeatureDrivers();

    streamDriver.maybeRestoreScroll();

    // Should never restore scroll if created in a non-restoring state.
    verify(scrollRestorer, never()).maybeRestoreScroll();
  }

  @Test
  public void testContinuationToken_createsContinuationContentModel() {
    when(streamFeature.getCursor()).thenReturn(new FakeModelCursor.Builder().addToken().build());

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(2);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(noContentDriver);
    assertThat(leafFeatureDrivers.get(1)).isEqualTo(continuationDriver);
  }

  @Test
  public void testContinuationToken_tokenHandling() {
    streamDriver = createStreamDriver(/* restoring= */ true, /* isInitialLoad= */ false);

    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addToken().build();
    FakeModelCursor newCursor = new FakeModelCursor.Builder().addCluster().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(2);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(noContentDriver);
    assertThat(leafFeatureDrivers.get(1)).isEqualTo(continuationDriver);

    streamDriver.onNewChildren(initialCursor.getChildAt(0), newCursor.getModelChildren());
    leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(1);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(getLeafFeatureDriverFromCluster(1));

    // If the above two assertions pass, this is also guaranteed to pass. This is just to explicitly
    // check that the ContinuationDriver has been removed.
    assertThat(leafFeatureDrivers).doesNotContain(continuationDriver);

    verify(scrollRestorer).maybeRestoreScroll();
  }

  @Test
  public void testContinuationToken_tokenHandling_newSyntheticToken() {
    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addSyntheticToken().build();
    FakeModelCursor newCursor =
        new FakeModelCursor.Builder().addCluster().addSyntheticToken().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);
    when(continuationDriver.isSynthetic()).thenReturn(true);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(2);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(noContentDriver);
    assertThat(leafFeatureDrivers.get(1)).isEqualTo(continuationDriver);

    streamDriver.onNewChildren(initialCursor.getChildAt(0), newCursor.getModelChildren());
    leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(2);
    assertThat(leafFeatureDrivers.get(0)).isEqualTo(getLeafFeatureDriverFromCluster(1));
    assertThat(leafFeatureDrivers.get(1)).isEqualTo(continuationDriver);

    verify(scrollRestorer, never()).maybeRestoreScroll();
  }

  @Test
  public void testContinuationToken_tokenHandling_notifiesObservers() {
    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addCard().addToken().build();
    FakeModelCursor newCursor = new FakeModelCursor.Builder().addCluster().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);

    streamDriver.getLeafFeatureDrivers();

    streamDriver.onNewChildren(initialCursor.getChildAt(1), newCursor.getModelChildren());

    verify(contentListener).notifyContentRemoved(1);
    verify(contentListener)
        .notifyContentsAdded(1, Lists.newArrayList(getLeafFeatureDriverFromCluster(2)));
    verify(streamOfflineMonitor, times(2)).requestOfflineStatusForNewContent();
  }

  @Test
  public void testContinuationToken_tokenChildrenAddedAtTokenPosition() {
    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addCluster().addToken().build();
    FakeModelCursor newCursor = new FakeModelCursor.Builder().addCluster().addToken().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);

    streamDriver.getLeafFeatureDrivers();
    streamDriver.onNewChildren(initialCursor.getChildAt(1), newCursor.getModelChildren());

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(3);
    assertThat(leafFeatureDrivers)
        .containsExactly(
            getLeafFeatureDriverFromCluster(0),
            getLeafFeatureDriverFromCluster(2),
            continuationDriver);
  }

  @Test
  public void testContinuationToken_tokenChildrenAddedAtTokenPosition_tokenNotAtEnd() {
    FakeModelCursor initialCursor =
        new FakeModelCursor.Builder().addCluster().addToken().addCluster().build();
    FakeModelCursor newCursor =
        new FakeModelCursor.Builder().addCluster().addCard().addCluster().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);

    streamDriver.getLeafFeatureDrivers();
    streamDriver.onNewChildren(initialCursor.getChildAt(1), newCursor.getModelChildren());

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();
    assertThat(leafFeatureDrivers).hasSize(5);
    assertThat(leafFeatureDrivers)
        .containsExactly(
            getLeafFeatureDriverFromCluster(0),
            getLeafFeatureDriverFromCluster(2),
            getLeafFeatureDriverFromCluster(3),
            getLeafFeatureDriverFromCard(4),
            getLeafFeatureDriverFromCluster(5));
  }

  @Test
  public void testContinuationToken_tokenNotFound() {
    FakeModelCursor initialCursor =
        new FakeModelCursor.Builder().addCluster().addToken().addCluster().build();
    FakeModelCursor badCursor = new FakeModelCursor.Builder().addCard().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);

    streamDriver.getLeafFeatureDrivers();
    assertThatRunnable(
            () -> streamDriver.onNewChildren(badCursor.getChildAt(0), badCursor.getModelChildren()))
        .throwsAnExceptionOfType(RuntimeException.class);
  }

  @Test
  public void testOnChange_remove() {
    FakeModelCursor fakeModelCursor =
        new FakeModelCursor.Builder().addCard().addCard().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(4);

    streamDriver.onChange(
        new FeatureChangeBuilder()
            .addChildForRemoval(fakeModelCursor.getChildAt(1))
            .addChildForRemoval(fakeModelCursor.getChildAt(2))
            .build());

    assertThat(streamDriver.getLeafFeatureDrivers())
        .containsExactly(leafFeatureDrivers.get(0), leafFeatureDrivers.get(3));
  }

  @Test
  public void testOnChange_addsZeroState_whenFeatureDriversEmpty() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(1);

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(fakeModelCursor.getChildAt(0)).build());

    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(1);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0)).isInstanceOf(ZeroStateDriver.class);
  }

  @Test
  public void testGetLeafFeatureDrivers_addsZeroState_withNoModelChildren() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(1);
    assertThat(leafFeatureDrivers.get(0)).isInstanceOf(ZeroStateDriver.class);
  }

  @Test
  public void testGetLeafFeatureDrivers_doesNotAddZeroState_ifInitialLoad() {
    streamDriver = createStreamDriver(/* restoring= */ false, /* isInitialLoad= */ true);
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).isEmpty();
  }

  @Test
  public void testGetLeafFeatureDrivers_doesNotAddZeroState_ifRestoring() {
    streamDriver = createStreamDriver(/* restoring= */ true, /* isInitialLoad= */ false);
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).isEmpty();
  }

  @Test
  public void testShowZeroState_createsZeroState() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);
    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(2);

    streamDriver.showZeroState(/* showSpinner= */ true);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(1);
    assertThat(leafFeatureDrivers.get(0)).isInstanceOf(ZeroStateDriver.class);
  }

  @Test
  public void testShowZeroState_notifiesContentsCleared() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    streamDriver.showZeroState(/* showSpinner= */ true);

    verify(contentListener).notifyContentsCleared();
  }

  @Test
  public void testShowZeroState_destroysFeatureDrivers() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addToken().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);
    streamDriver.getLeafFeatureDrivers();

    streamDriver.showZeroState(/* showSpinner= */ true);

    verify(continuationDriver).onDestroy();
  }

  @Test
  public void testOnChange_addsNoContentCard_withJustContinuationDriver() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addToken().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    List<LeafFeatureDriver> leafFeatureDrivers = streamDriver.getLeafFeatureDrivers();

    assertThat(leafFeatureDrivers).hasSize(2);

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(fakeModelCursor.getChildAt(0)).build());

    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(2);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0)).isInstanceOf(NoContentDriver.class);
  }

  @Test
  public void testContinuationToken_removesNoContentCard() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addToken().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    streamDriver.getLeafFeatureDrivers();

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(fakeModelCursor.getChildAt(0)).build());

    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(2);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0)).isInstanceOf(NoContentDriver.class);

    FakeModelCursor newCursor = new FakeModelCursor.Builder().addCard().build();
    streamDriver.onNewChildren(fakeModelCursor.getChildAt(1), newCursor.getModelChildren());

    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(1);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0))
        .isInstanceOf(FakeLeafFeatureDriver.class);
  }

  @Test
  public void testContinuationToken_replacesNoContentCardWithZeroState() {
    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addCard().addToken().build();
    when(streamFeature.getCursor()).thenReturn(initialCursor);
    streamDriver.getLeafFeatureDrivers();

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(initialCursor.getChildAt(0)).build());
    streamDriver.onNewChildren(initialCursor.getChildAt(1), Collections.emptyList());

    verify(noContentDriver).onDestroy();
    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(1);
    assertThat(streamDriver.getLeafFeatureDrivers().get(0)).isInstanceOf(ZeroStateDriver.class);
  }

  @Test
  public void testOnChange_remove_notifiesListener() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    streamDriver.getLeafFeatureDrivers();

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(fakeModelCursor.getChildAt(0)).build());

    verify(contentListener).notifyContentRemoved(0);
    verifyNoMoreInteractions(contentListener);
  }

  @Test
  public void testOnChange_addAndRemoveContent() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    streamDriver.getLeafFeatureDrivers();

    streamDriver.onChange(
        new FeatureChangeBuilder()
            .addChildForRemoval(fakeModelCursor.getChildAt(0))
            .addChildForAppending(createFakeClusterModelChild())
            .build());

    InOrder inOrder = Mockito.inOrder(contentListener);

    inOrder.verify(contentListener).notifyContentRemoved(0);
    inOrder.verify(contentListener).notifyContentsAdded(eq(1), featureDriversCaptor.capture());
    inOrder.verifyNoMoreInteractions();

    assertThat(featureDriversCaptor.getValue()).hasSize(1);
    assertThat(featureDriversCaptor.getValue().get(0))
        .isEqualTo(getLeafFeatureDriverFromCluster(2));
  }

  @Test
  public void testOnChange_addContent() {
    FakeModelCursor fakeModelCursor = new FakeModelCursor.Builder().addCard().addCard().build();

    when(streamFeature.getCursor()).thenReturn(fakeModelCursor);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    streamDriver.getLeafFeatureDrivers();

    reset(contentListener);

    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForAppending(createFakeClusterModelChild()).build());

    InOrder inOrder = Mockito.inOrder(contentListener);

    inOrder.verify(contentListener).notifyContentsAdded(eq(2), featureDriversCaptor.capture());
    inOrder.verifyNoMoreInteractions();

    assertThat(featureDriversCaptor.getValue()).hasSize(1);
    assertThat(featureDriversCaptor.getValue().get(0))
        .isEqualTo(getLeafFeatureDriverFromCluster(2));
    verify(streamOfflineMonitor, times(2)).requestOfflineStatusForNewContent();
  }

  @Test
  public void testOnDestroy() {
    FakeModelCursor cursor = new FakeModelCursor.Builder().addCard().addToken().build();
    when(streamFeature.getCursor()).thenReturn(cursor);

    // Causes StreamDriver to build a list of children based on the children from the cursor.
    streamDriver.getLeafFeatureDrivers();
    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(cursor.getChildAt(0)).build());

    assertThat(streamDriver.getLeafFeatureDrivers())
        .containsExactly(noContentDriver, continuationDriver);
    streamDriver.onDestroy();

    verify(noContentDriver).onDestroy();
    verify(continuationDriver).onDestroy();
    verify(streamFeature).unregisterObserver(streamDriver);
  }

  @Test
  public void testOnDestroy_clearsFeatureDrivers() {
    streamDriver = createStreamDriver(false, true);
    when(streamFeature.getCursor()).thenReturn(new FakeModelCursor.Builder().addCard().build());

    assertThat(streamDriver.getLeafFeatureDrivers()).hasSize(1);
    streamDriver.onDestroy();

    assertThat(streamDriver.getLeafFeatureDrivers()).isEmpty();
  }

  @Test
  public void testHasContent_returnsTrue() {
    FakeModelCursor cursor = new FakeModelCursor.Builder().addCard().addToken().build();
    when(streamFeature.getCursor()).thenReturn(cursor);
    streamDriver.getLeafFeatureDrivers();

    assertThat(streamDriver.hasContent()).isTrue();
  }

  @Test
  public void testHasContent_returnsFalse_withNoContentCard() {
    FakeModelCursor cursor = new FakeModelCursor.Builder().addCard().addToken().build();
    when(streamFeature.getCursor()).thenReturn(cursor);
    streamDriver.getLeafFeatureDrivers();
    // Remove card to trigger no content card.
    streamDriver.onChange(
        new FeatureChangeBuilder().addChildForRemoval(cursor.getChildAt(0)).build());

    assertThat(streamDriver.hasContent()).isFalse();
  }

  @Test
  public void testHasContent_returnsFalse_withZeroState() {
    FakeModelCursor cursor = new FakeModelCursor.Builder().build();
    when(streamFeature.getCursor()).thenReturn(cursor);
    streamDriver.getLeafFeatureDrivers();

    assertThat(streamDriver.hasContent()).isFalse();
  }

  @Test
  public void testAutoConsumeSyntheticTokensOnRestore() {
    FakeModelCursor initialCursor = new FakeModelCursor.Builder().addCard().addToken().build();
    FakeModelCursor newCursor = new FakeModelCursor.Builder().addToken().build();

    when(streamFeature.getCursor()).thenReturn(initialCursor);

    streamDriver = createStreamDriver(/* restoring= */ true, /* isInitialLoad= */ false);

    streamDriver.getLeafFeatureDrivers();

    assertThat(streamDriver.wasRestoringDuringLastContinuationDriver).isTrue();

    // Restoring scroll indicates that restore is over, and further continuation drivers will not be
    // forced to consume synthetic tokens.
    streamDriver.maybeRestoreScroll();

    streamDriver.onNewChildren(initialCursor.getChildAt(1), newCursor.getModelChildren());

    assertThat(streamDriver.wasRestoringDuringLastContinuationDriver).isFalse();
  }

  private StreamDriverForTest createNonRestoringStreamDriver() {
    return createStreamDriver(/* restoring= */ false, /* isInitialLoad= */ false);
  }

  private StreamDriverForTest createRestoringStreamDriver() {
    return createStreamDriver(/* restoring= */ true, /* isInitialLoad= */ true);
  }

  private StreamDriverForTest createStreamDriver(boolean restoring, boolean isInitialLoad) {
    return new StreamDriverForTest(modelProvider, threadUtils, restoring, isInitialLoad);
  }

  // TODO: Instead of just checking that the ModelFeature is of the correct type, check
  // that it is the one created by the FakeModelCursor.Builder.
  private LeafFeatureDriver getLeafFeatureDriverFromCard(int i) {
    FakeFeatureDriver featureDriver = (FakeFeatureDriver) streamDriver.childrenCreated.get(i);
    assertThat(featureDriver.getModelFeature().getStreamFeature().hasCard()).isTrue();
    return streamDriver.childrenCreated.get(i).getLeafFeatureDriver();
  }

  // TODO: Instead of just checking that the ModelFeature is of the correct type, check
  // that it is the one created by the FakeModelCursor.Builder.
  private LeafFeatureDriver getLeafFeatureDriverFromCluster(int i) {
    FakeFeatureDriver featureDriver = (FakeFeatureDriver) streamDriver.childrenCreated.get(i);
    assertThat(featureDriver.getModelFeature().getStreamFeature().hasCluster()).isTrue();
    return streamDriver.childrenCreated.get(i).getLeafFeatureDriver();
  }

  private class StreamDriverForTest extends StreamDriver {
    // TODO: create a fake for ContinuationDriver so that this can be
    // List<FakeFeatureDriver>
    private List<FeatureDriver> childrenCreated;
    private boolean wasRestoringDuringLastContinuationDriver;

    StreamDriverForTest(
        ModelProvider modelProvider,
        ThreadUtils threadUtils,
        boolean restoring,
        boolean isInitialLoad) {
      super(
          actionApi,
          actionManager,
          actionParserFactory,
          modelProvider,
          threadUtils,
          clock,
          configuration,
          context,
          snackbarApi,
          contentChangedListener,
          scrollRestorer,
          basicLoggingApi,
          streamOfflineMonitor,
          knownContentApi,
          mock(ContextMenuManager.class),
          restoring,
          isInitialLoad);
      childrenCreated = new ArrayList<>();
    }

    @Override
    ContinuationDriver createContinuationDriver(
        BasicLoggingApi basicLoggingApi,
        Clock clock,
        Configuration configuration,
        Context context,
        ModelChild modelChild,
        ModelProvider modelProvider,
        int position,
        SnackbarApi snackbarApi,
        boolean restoring) {
      this.wasRestoringDuringLastContinuationDriver = restoring;
      childrenCreated.add(continuationDriver);
      return continuationDriver;
    }

    @Override
    FeatureDriver createClusterDriver(ModelFeature modelFeature, int position) {
      FeatureDriver featureDriver =
          new FakeFeatureDriver.Builder().setModelFeature(modelFeature).build();
      childrenCreated.add(featureDriver);
      return featureDriver;
    }

    @Override
    FeatureDriver createCardDriver(ModelFeature modelFeature, int position) {
      if (modelFeature != UNBOUND_CARD) {
        FeatureDriver featureDriver =
            new FakeFeatureDriver.Builder().setModelFeature(modelFeature).build();
        childrenCreated.add(featureDriver);
        return featureDriver;
      } else {
        FeatureDriver featureDriver =
            new FakeFeatureDriver.Builder()
                .setModelFeature(modelFeature)
                .setLeafFeatureDriver(null)
                .build();
        childrenCreated.add(featureDriver);
        return featureDriver;
      }
    }

    @Override
    NoContentDriver createNoContentDriver() {
      return noContentDriver;
    }
  }

}
