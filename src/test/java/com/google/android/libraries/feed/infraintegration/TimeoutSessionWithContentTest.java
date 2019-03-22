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

package com.google.android.libraries.feed.infraintegration;

import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.ROOT_CONTENT_ID;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.FeatureChange.ChildChanges;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.feed.client.StreamDataProto.UiContext;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

/**
 * This is a TimeoutSession test which verifies REQUEST_WITH_CONTENT
 *
 * <p>NOTE: This test has multiple threads running. There is a single threaded executor created in
 * addition to the main thread. The Test will throw a TimeoutException in in production in the event
 * of a deadlock. The DEBUG boolean controls the behavior to allow debugging.
 */
@RunWith(RobolectricTestRunner.class)
public class TimeoutSessionWithContentTest {
  // This flag will should be flipped to debug the test.  It will disable TimeoutExceptions.
  private static final boolean DEBUG = false;
  private static final ContentId[] REQUEST_ONE =
      new ContentId[] {
        WireProtocolResponseBuilder.createFeatureContentId(1),
        WireProtocolResponseBuilder.createFeatureContentId(2),
        WireProtocolResponseBuilder.createFeatureContentId(3)
      };
  private static final ContentId[] REQUEST_TWO =
      new ContentId[] {
        WireProtocolResponseBuilder.createFeatureContentId(4),
        WireProtocolResponseBuilder.createFeatureContentId(3),
        WireProtocolResponseBuilder.createFeatureContentId(5)
      };

  @Mock private ThreadUtils threadUtils;
  @Mock private SchedulerApi schedulerApi;

  private FakeRequestManager requestManager;
  private ModelProviderFactory modelProviderFactory;
  private ProtocolAdapter protocolAdapter;
  private ModelProviderValidator modelValidator;
  private long timeoutDeadline;

  @Before
  public void setUp() {
    initMocks(this);
    Configuration configuration = InfrastructureIntegrationScope.getTimeoutSchedulerConfig();
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(threadUtils, Executors.newSingleThreadExecutor())
            .setConfiguration(configuration)
            .setSchedulerApi(schedulerApi)
            .setRequestDelayMs(100)
            .build();
    requestManager = scope.getRequestManager();
    modelProviderFactory = scope.getModelProviderFactory();
    protocolAdapter = scope.getProtocolAdapter();
    modelValidator = new ModelProviderValidator(scope.getProtocolAdapter());
  }

  /**
   * Test steps:
   *
   * <ol>
   *   <li>Create the initial ModelProvider from $HEAD with a REQUEST_WITH_WAIT which makes the
   *       request before the session is populated.
   *   <li>Load the second request into the RequestManager
   *   <li>Create a second ModelProvider using REQUEST_WITH_CONTENT which displays the initial $HEAD
   *       but makes a request. The second request will be appended to and update the ModelProvider.
   * </ol>
   */
  @Test
  public void testRequestWithWait() throws TimeoutException {

    // Load up the initial request
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : REQUEST_ONE) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    requestManager.queueResponse(responseBuilder.build());

    // Wait for the request to complete (REQUEST_WITH_CONTENT).  This will trigger the request and
    // wait for it to complete to populate the new session.
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_WAIT);
    ModelProvider modelProvider = modelProviderFactory.createNew(null);

    // This will wait for the session to be created and validate the root cursor
    AtomicBoolean finished = new AtomicBoolean(false);
    assertSessionCreation(modelProvider, finished);
    while (!finished.get()) {
      // Loop through the tasks and wait for the assertSessionCreation to set finished to true
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
      if (timeoutDeadline > 0 && System.currentTimeMillis() > timeoutDeadline) {
        throw new TimeoutException();
      }
    }

    // Create a new ModelProvider from HEAD (REQUEST_WITH_CONTENT)
    responseBuilder = new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : REQUEST_TWO) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    requestManager.queueResponse(responseBuilder.build());
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_CONTENT);
    // This will wait for the session to be created and validate the root cursor
    modelProvider = modelProviderFactory.createNew(null);
    assertSessionCreationWithRequest(modelProvider, finished);
    while (!finished.get()) {
      // Loop through the tasks and wait for the assertSessionCreation to set finished to true
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
      if (timeoutDeadline > 0 && System.currentTimeMillis() > timeoutDeadline) {
        throw new TimeoutException();
      }
    }
  }

  // Verifies the initial session.
  private void assertSessionCreation(ModelProvider modelProvider, AtomicBoolean finished) {
    finished.set(false);
    timeoutDeadline =
        (DEBUG)
            ? InfrastructureIntegrationScope.TIMEOUT_TEST_TIMEOUT + System.currentTimeMillis()
            : 0;
    modelProvider.registerObserver(
        new ModelProviderObserver() {
          @Override
          public void onSessionStart() {
            System.out.println("onSessionStart");
            finished.set(true);
            modelValidator.assertCursorContents(modelProvider, REQUEST_ONE);
          }

          @Override
          public void onSessionFinished(UiContext uiContext) {
            System.out.println("onSessionFinished");
          }

          @Override
          public void onError(ModelError modelError) {
            System.out.println("onError");
          }
        });
  }

  // Verifies the second session.  There are two observers verified, the ModelProvider READ
  // and a change listener on the root for the second request.
  private void assertSessionCreationWithRequest(
      ModelProvider modelProvider, AtomicBoolean finished) {
    finished.set(false);
    timeoutDeadline =
        (DEBUG)
            ? InfrastructureIntegrationScope.TIMEOUT_TEST_TIMEOUT + System.currentTimeMillis()
            : 0;
    modelProvider.registerObserver(
        new ModelProviderObserver() {
          @Override
          public void onSessionStart() {
            System.out.println("onSessionStart");
            ModelFeature feature = modelProvider.getRootFeature();
            // The second request will cause an change on the root
            assertThat(feature).isNotNull();
            feature.registerObserver(
                change -> {
                  System.out.println("root.onChange");
                  finished.set(true);
                  modelValidator.assertCursorContents(
                      modelProvider,
                      REQUEST_ONE[0],
                      REQUEST_ONE[1],
                      REQUEST_ONE[2],
                      REQUEST_TWO[0],
                      REQUEST_TWO[2]);
                  assertThat(change.getContentId())
                      .isEqualTo(protocolAdapter.getStreamContentId(ROOT_CONTENT_ID));
                  ChildChanges childChanges = change.getChildChanges();
                  assertThat(childChanges.getAppendedChildren().size()).isEqualTo(2);
                });
            modelValidator.assertCursorContents(modelProvider, REQUEST_ONE);
          }

          @Override
          public void onSessionFinished(UiContext uiContext) {
            System.out.println("onSessionFinished");
          }

          @Override
          public void onError(ModelError modelError) {
            System.out.println("onError");
          }
        });
  }
}
