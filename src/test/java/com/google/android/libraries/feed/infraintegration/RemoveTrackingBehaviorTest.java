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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.RemoveTrackingFactory;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.modelprovider.RemoveTracking;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Function;
import com.google.android.libraries.feed.common.testing.InfraIntegrationScope;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.testing.requestmanager.FakeRequestManager;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the ModelProvider RemoveTracking behavior. */
@RunWith(RobolectricTestRunner.class)
public class RemoveTrackingBehaviorTest {
  private static final ContentId[] CARDS =
      new ContentId[] {
        WireProtocolResponseBuilder.createFeatureContentId(1),
        WireProtocolResponseBuilder.createFeatureContentId(2),
        WireProtocolResponseBuilder.createFeatureContentId(3),
        WireProtocolResponseBuilder.createFeatureContentId(4),
        WireProtocolResponseBuilder.createFeatureContentId(5),
      };

  @Mock private ThreadUtils threadUtils;

  private FakeRequestManager requestManager;
  private SessionManager sessionManager;
  private ModelProviderFactory modelProviderFactory;
  private ProtocolAdapter protocolAdapter;

  @Before
  public void setUp() {
    initMocks(this);
    InfraIntegrationScope scope = new InfraIntegrationScope.Builder(threadUtils).build();
    requestManager = scope.getRequestManager();
    sessionManager = scope.getSessionManager();
    modelProviderFactory = scope.getModelProviderFactory();
    protocolAdapter = scope.getProtocolAdapter();
  }

  @Test
  public void testBaseRemoveTracking() {
    loadInitialData();

    AtomicBoolean called = new AtomicBoolean(false);
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    modelProvider.enableRemoveTracking(
        getRemoveTrackingFactory(
            (contentIds) -> {
              assertThat(contentIds).hasSize(1);
              called.set(true);
            }));
    WireProtocolResponseBuilder responseBuilder =
        WireProtocolResponseBuilder.builder().removeFeature(CARDS[1], ROOT_CONTENT_ID);
    List<StreamDataOperation> dataOperations = getDataOperations(responseBuilder);
    MutationContext mutationContext = new MutationContext.Builder().setUserInitiated(true).build();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(mutationContext);
    updateConsumer.accept(Result.success(dataOperations));
    assertThat(called.get()).isTrue();
  }

  @Test
  public void testBaseRemoveTracking_multipleItems() {
    loadInitialData();

    AtomicBoolean called = new AtomicBoolean(false);
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    modelProvider.enableRemoveTracking(
        getRemoveTrackingFactory(
            (contentIds) -> {
              assertThat(contentIds).hasSize(2);
              called.set(true);
            }));
    WireProtocolResponseBuilder responseBuilder =
        WireProtocolResponseBuilder.builder()
            .removeFeature(CARDS[1], ROOT_CONTENT_ID)
            .removeFeature(CARDS[3], ROOT_CONTENT_ID);
    List<StreamDataOperation> dataOperations = getDataOperations(responseBuilder);
    MutationContext mutationContext = new MutationContext.Builder().setUserInitiated(true).build();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(mutationContext);
    updateConsumer.accept(Result.success(dataOperations));
    assertThat(called.get()).isTrue();
  }

  /**
   * For non-user initiated mutations, the test is setup to return null from the factory. The result
   * is the Consumer should not be called.
   */
  @Test
  public void testNonUserInitiated() {
    loadInitialData();

    AtomicBoolean called = new AtomicBoolean(false);
    ModelProvider modelProvider = modelProviderFactory.createNew(null);
    modelProvider.enableRemoveTracking(
        getRemoveTrackingFactory(
            (contentIds) -> {
              assertThat(contentIds).isEmpty();
              called.set(true);
            }));

    WireProtocolResponseBuilder responseBuilder =
        WireProtocolResponseBuilder.builder().removeFeature(CARDS[1], ROOT_CONTENT_ID);
    List<StreamDataOperation> dataOperations = getDataOperations(responseBuilder);
    MutationContext mutationContext = new MutationContext.Builder().setUserInitiated(false).build();
    Consumer<Result<List<StreamDataOperation>>> updateConsumer =
        sessionManager.getUpdateConsumer(mutationContext);
    updateConsumer.accept(Result.success(dataOperations));
    assertThat(called.get()).isFalse();
  }

  private RemoveTrackingFactory<String> getRemoveTrackingFactory(Consumer<List<String>> consumer) {
    return new RemoveTrackingFactory<String>() {
      @Override
      public /*@Nullable*/ RemoveTracking<String> create(MutationContext mutationContext) {
        // Only support RemoveTracking on user initiated removes
        return mutationContext.isUserInitiated()
            ? getRemoveTracking(
                (streamFeature) -> simpleTransform(streamFeature),
                consumer)
            : null;
      }
    };
  }

  private RemoveTracking<String> getRemoveTracking(
      Function<StreamFeature, String> transformer,
      Consumer<List<String>> consumer) {
    return new RemoveTracking<>(transformer, consumer);
  }

  @SuppressWarnings("unused")
  private boolean alwaysTrue(String value) {
    return true;
  }

  private String simpleTransform(StreamFeature streamFeature) {
    // only return the Content StreamFeatures.
    return streamFeature.hasContent() ? streamFeature.getContentId() : null;
  }

  private List<StreamDataOperation> getDataOperations(WireProtocolResponseBuilder builder) {
    Response response = builder.build();
    Result<List<StreamDataOperation>> result = protocolAdapter.createModel(response);
    assertThat(result.isSuccessful()).isTrue();
    return result.getValue();
  }

  private void loadInitialData() {
    requestManager.queueResponse(WireProtocolResponseBuilder.forClearAllWithCards(CARDS).build());
    requestManager.triggerRefresh(
        RequestReason.OPEN_WITHOUT_CONTENT,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));
  }
}
