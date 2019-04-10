// Copyright 2019 The Feed Authors.
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.ModelProviderValidator;
import com.google.android.libraries.feed.common.testing.ResponseBuilder;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.RequestBehavior;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi.SessionManagerState;
import com.google.android.libraries.feed.testing.modelprovider.FakeViewDepthProvider;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests of the ViewDepthProvider. The ViewDepthProvider indicates the depth of the Stream the user
 * has seen. For some types of SchedulerApi {@link RequestBehavior} values this will prune the
 * Stream beyond that depth.
 */
@RunWith(RobolectricTestRunner.class)
public class ViewDepthProviderTests {
  private static final ContentId[] REQUEST_ONE =
      new ContentId[] {
        ResponseBuilder.createFeatureContentId(1),
        ResponseBuilder.createFeatureContentId(2),
        ResponseBuilder.createFeatureContentId(3),
        ResponseBuilder.createFeatureContentId(4)
      };
  private static final ContentId[] REQUEST_TWO =
      new ContentId[] {
        ResponseBuilder.createFeatureContentId(5),
        ResponseBuilder.createFeatureContentId(6),
        ResponseBuilder.createFeatureContentId(7)
      };
  private static final ContentId[] REQUEST_TWO_WITH_DUPLICATES =
      new ContentId[] {
        ResponseBuilder.createFeatureContentId(5),
        ResponseBuilder.createFeatureContentId(6),
        ResponseBuilder.createFeatureContentId(4)
      };
  private static final ContentId[] REQUEST_TWO_WITH_DUPLICATES_PAGE =
      new ContentId[] {
        ResponseBuilder.createFeatureContentId(5),
        ResponseBuilder.createFeatureContentId(4),
        ResponseBuilder.createFeatureContentId(7)
      };

  @Mock private ThreadUtils threadUtils;
  @Mock private SchedulerApi schedulerApi;

  private FakeRequestManager requestManager;
  private ModelProviderFactory modelProviderFactory;
  private ProtocolAdapter protocolAdapter;
  private ModelProviderValidator modelValidator;
  private SessionManager sessionManager;
  private ViewDepthProvider viewDepthProvider;

  @Before
  public void setUp() {
    initMocks(this);
    Configuration configuration = InfraIntegrationScope.getTimeoutSchedulerConfig();
    InfraIntegrationScope scope =
        new InfraIntegrationScope.Builder(threadUtils)
            .setConfiguration(configuration)
            .setSchedulerApi(schedulerApi)
            .build();
    requestManager = scope.getRequestManager();
    modelProviderFactory = scope.getModelProviderFactory();
    protocolAdapter = scope.getProtocolAdapter();
    modelValidator = new ModelProviderValidator(scope.getProtocolAdapter());
    sessionManager = scope.getSessionManager();
    viewDepthProvider =
        new FakeViewDepthProvider()
            .setChildViewDepth(protocolAdapter.getStreamContentId(REQUEST_ONE[1]));
  }

  @Test
  public void baseDepthProviderTest() {

    // Load up the initial request
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(REQUEST_ONE).build());

    // The REQUEST_ONE content will be added to head, this is then used to create the initial
    // session.
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // The REQUEST_TWO content acts as a second request on the server, it is triggered by
    // REQUEST_WITH_CONTENT
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_CONTENT);
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(REQUEST_TWO).build());
    ModelProvider modelProvider = modelProviderFactory.createNew(viewDepthProvider);

    // The second request will be added after the first request, the ViewDepthProvider indicates
    // we only saw [0] and [1] from the first request, so [2] and [3] will be removed.
    modelValidator.assertCursorContents(
        modelProvider,
        REQUEST_ONE[0],
        REQUEST_ONE[1],
        REQUEST_TWO[0],
        REQUEST_TWO[1],
        REQUEST_TWO[2]);
  }

  @Test
  public void testDuplicateEntries() {
    // Load up the initial request
    requestManager.queueResponse(ResponseBuilder.forClearAllWithCards(REQUEST_ONE).build());

    // The REQUEST_ONE content will be added to head, this is then used to create the initial
    // session.
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // The REQUEST_TWO content acts as a second request on the server, it is triggered by
    // REQUEST_WITH_CONTENT
    when(schedulerApi.shouldSessionRequestData(any(SessionManagerState.class)))
        .thenReturn(RequestBehavior.REQUEST_WITH_CONTENT);
    requestManager.queueResponse(
        ResponseBuilder.forClearAllWithCards(REQUEST_TWO_WITH_DUPLICATES).build());
    ModelProvider modelProvider = modelProviderFactory.createNew(viewDepthProvider);

    // The second request will be added after the first request, the ViewDepthProvider indicates
    // we only saw [0] and [1] from the first request, so [2] and [3] will be removed.
    modelValidator.assertCursorContents(
        modelProvider,
        REQUEST_ONE[0],
        REQUEST_ONE[1],
        REQUEST_TWO_WITH_DUPLICATES[0],
        REQUEST_TWO_WITH_DUPLICATES[1],
        REQUEST_TWO_WITH_DUPLICATES[2]);

    // Now page in the same content, this should all be updates
    requestManager.queueResponse(
        ResponseBuilder.builder().addCardsToRoot(REQUEST_TWO_WITH_DUPLICATES_PAGE).build());
    requestManager.triggerRefresh(
        RequestReason.HOST_REQUESTED,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    modelValidator.assertCursorContents(
        modelProvider,
        REQUEST_ONE[0],
        REQUEST_ONE[1],
        REQUEST_TWO_WITH_DUPLICATES[0],
        REQUEST_TWO_WITH_DUPLICATES[1],
        REQUEST_TWO_WITH_DUPLICATES[2],
        REQUEST_TWO_WITH_DUPLICATES_PAGE[2]);
  }
}
