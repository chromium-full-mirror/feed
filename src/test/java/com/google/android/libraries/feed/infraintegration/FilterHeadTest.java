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
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Function;
import com.google.android.libraries.feed.common.testing.FakeRequestManager;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link SessionManager#getStreamFeaturesFromHead(Function, Consumer)} method. */
@RunWith(RobolectricTestRunner.class)
public class FilterHeadTest {
  @Mock private ThreadUtils threadUtils;

  private FakeRequestManager requestManager;
  private SessionManager sessionManager;

  @Before
  public void setUp() {
    initMocks(this);
    InfrastructureIntegrationScope scope =
        new InfrastructureIntegrationScope.Builder(
                threadUtils, MoreExecutors.newDirectExecutorService())
            .build();
    requestManager = scope.getRequestManager();
    sessionManager = scope.getSessionManager();
  }

  @Test
  public void testFiltering() {
    ContentId[] cards =
        new ContentId[] {
          WireProtocolResponseBuilder.createFeatureContentId(1),
          WireProtocolResponseBuilder.createFeatureContentId(2),
          WireProtocolResponseBuilder.createFeatureContentId(3),
          WireProtocolResponseBuilder.createFeatureContentId(4),
          WireProtocolResponseBuilder.createFeatureContentId(5),
        };

    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : cards) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    requestManager.queueResponse(responseBuilder.build());
    requestManager.triggerRefresh(
        RequestReason.APP_OPEN_REFRESH,
        sessionManager.getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    sessionManager.getStreamFeaturesFromHead(
        transformer,
        result -> {
          assertThat(result.isSuccessful()).isTrue();
          assertThat(result.getValue()).hasSize(11);
        });

    // only return the contentId of the Content cards
    sessionManager.getStreamFeaturesFromHead(
        toContent,
        result -> {
          assertThat(result.isSuccessful()).isTrue();
          assertThat(result.getValue()).hasSize(5);
        });
  }

  // Return only StreamFeatures
  private Function<StreamPayload, /*@Nullable*/ StreamFeature> transformer =
      streamPayload -> streamPayload.hasStreamFeature() ? streamPayload.getStreamFeature() : null;

  // Return the contentId from a feature with a RenderableUnit type of CONTENT
  private Function<StreamPayload, /*@Nullable*/ String> toContent =
      streamPayload -> {
        if (!streamPayload.hasStreamFeature()) {
          return null;
        }
        StreamFeature streamFeature = streamPayload.getStreamFeature();
        return streamFeature.hasContent() ? streamFeature.getContentId() : null;
      };
}
