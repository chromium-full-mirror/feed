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

import static com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder.ROOT_CONTENT_ID;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.common.concurrent.testing.FakeThreadUtils;
import com.google.android.libraries.feed.common.testing.InfrastructureIntegrationScope;
import com.google.android.libraries.feed.common.testing.WireProtocolResponseBuilder;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests that assert the behavior of garbage collection. */
@RunWith(RobolectricTestRunner.class)
public final class GcTest {
  private static final ContentId[] REQUEST_1 =
      new ContentId[] {
        WireProtocolResponseBuilder.createFeatureContentId(1),
        WireProtocolResponseBuilder.createFeatureContentId(2)
      };
  private static final ContentId[] REQUEST_2 =
      new ContentId[] {
        WireProtocolResponseBuilder.createFeatureContentId(3),
        WireProtocolResponseBuilder.createFeatureContentId(4)
      };
  private static final long LIFETIME_MS = Duration.ofHours(1).toMillis();

  private final FakeClock fakeClock = new FakeClock();
  private final InfrastructureIntegrationScope scope =
      new InfrastructureIntegrationScope.Builder(
              new FakeThreadUtils(/* enforceThreadChecks= */ false),
              MoreExecutors.newDirectExecutorService())
          .setClock(fakeClock)
          .setConfiguration(
              new Configuration.Builder().put(ConfigKey.SESSION_LIFETIME_MS, LIFETIME_MS).build())
          .build();

  @Test
  public void testGc_contentInLiveSessionRetained() {
    scope
        .getRequestManager()
        .queueResponse(createResponse(REQUEST_1))
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // Create a new session based on this request.
    scope.getModelProviderFactory().createNew(/* viewDepthProvider= */ null).detachModelProvider();
    assertPayloads(REQUEST_1, scope, /* shouldExist= */ true);

    // Populate HEAD with new data.
    scope
        .getRequestManager()
        .queueResponse(createResponse(REQUEST_2))
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // Advance the clock without expiring the first session.
    fakeClock.advance(LIFETIME_MS / 2);
    InfrastructureIntegrationScope secondScope = scope.clone();
    assertPayloads(REQUEST_1, secondScope, /* shouldExist= */ true);
    assertPayloads(REQUEST_2, secondScope, /* shouldExist= */ true);
  }

  @Test
  public void testGc_contentInExpiredSessionDeleted() {
    scope
        .getRequestManager()
        .queueResponse(createResponse(REQUEST_1))
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // Create a new session based on this request.
    scope.getModelProviderFactory().createNew(/* viewDepthProvider= */ null).detachModelProvider();
    assertPayloads(REQUEST_1, scope, /* shouldExist= */ true);

    // Populate HEAD with new data.
    scope
        .getRequestManager()
        .queueResponse(createResponse(REQUEST_2))
        .triggerRefresh(
            RequestReason.OPEN_WITHOUT_CONTENT,
            scope.getSessionManager().getUpdateConsumer(MutationContext.EMPTY_CONTEXT));

    // Advance the clock to expire the first session, create a new scope that will run
    // initialization and delete content from the expired session.
    fakeClock.advance(LIFETIME_MS + 1L);
    InfrastructureIntegrationScope secondScope = scope.clone();
    assertPayloads(REQUEST_1, secondScope, /* shouldExist= */ false);
    assertPayloads(REQUEST_2, secondScope, /* shouldExist= */ true);
  }

  private static void assertPayloads(
      ContentId[] contentIds, InfrastructureIntegrationScope scope, boolean shouldExist) {
    for (ContentId contentId : contentIds) {
      List<PayloadWithId> payloads =
          scope
              .getStore()
              .getPayloads(
                  Arrays.asList(
                      new String[] {scope.getProtocolAdapter().getStreamContentId(contentId)}))
              .getValue();
      if (shouldExist) {
        assertThat(payloads).hasSize(1);
      } else {
        assertThat(payloads).isEmpty();
      }
    }
  }

  private static Response createResponse(ContentId[] contentIds) {
    WireProtocolResponseBuilder responseBuilder =
        new WireProtocolResponseBuilder().addClearOperation().addRootFeature();
    for (ContentId contentId : contentIds) {
      responseBuilder.addCard(contentId, ROOT_CONTENT_ID);
    }
    return responseBuilder.build();
  }
}
