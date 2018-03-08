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

package com.google.android.libraries.feed.feedstore;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.ContentMutation.ContentMutationResults;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.testing.RunnableSubject;
import com.google.android.libraries.feed.common.testing.RunnableSubject.ThrowingRunnable;
import com.google.android.libraries.feed.host.common.ProtoExtensionProvider;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload.Builder;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link FeedStore} class. */
@RunWith(RobolectricTestRunner.class)
public class FeedStoreTest {

  private Store store;
  private static final long PAYLOAD_ID = 12345;
  private static final long OPERATION_ID = 67890;
  private static final StreamContentId PAYLOAD_CONTENT_ID =
      StreamContentId.newBuilder().setId(PAYLOAD_ID).build();
  private static final StreamContentId OPERATION_CONTENT_ID =
      StreamContentId.newBuilder().setId(OPERATION_ID).build();
  private static final Builder STREAM_PAYLOAD =
      StreamPayload.newBuilder()
          .setStreamFeature(
              StreamFeature.newBuilder()
                  .setContentId(PAYLOAD_CONTENT_ID)
                  .setParentId(StreamContentId.getDefaultInstance()));
  private static final StreamStructure STREAM_STRUCTURE =
      StreamStructure.newBuilder()
          .setContentId(OPERATION_CONTENT_ID)
          .setParentContentId(StreamContentId.getDefaultInstance())
          .setOperation(Operation.UPDATE_OR_APPEND)
          .build();
  private static final StreamDataOperation STREAM_DATA_OPERATION =
      StreamDataOperation.newBuilder()
          .setStreamStructure(STREAM_STRUCTURE)
          .setStreamPayload(STREAM_PAYLOAD)
          .build();

  private final InternalProtocolUtils internalProtocolUtils = new InternalProtocolUtils();
  private final TimingUtils timingUtils = new TimingUtils();
  private final FeedExtensionRegistry extensionRegistry =
      new FeedExtensionRegistry(
          new ProtoExtensionProvider() {
            @Override
            public List<GeneratedExtension<?, ?>> getProtoExtensions() {
              return new ArrayList<>();
            }
          });

  @Before
  public void setUp() throws Exception {
    // TODO: update the test to work with the old and new feed stores.
    store = new FeedStore(timingUtils, internalProtocolUtils, extensionRegistry);
  }

  @Test
  public void testMinimalStore() {
    FeedStore feedStore = new FeedStore(timingUtils, internalProtocolUtils, extensionRegistry);
    assertThat(feedStore.getHeadSession()).isNotNull();
  }

  @Test
  public void testContentMutation() {
    FeedStore feedStore = new FeedStore(timingUtils, internalProtocolUtils, extensionRegistry);
    ContentMutation contentMutation = feedStore.editContent();
    assertThat(contentMutation).isNotNull();
  }

  @Test
  public void newStoreHasHeadSession() throws Exception {
    StreamSession newSession = store.getHeadSession();
    assertThat(newSession).isNotNull();
  }

  @Test
  public void addStructureOperationToSession() throws Exception {
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_DATA_OPERATION.getStreamStructure());
    mutation.commit();

    List<StreamStructure> results = store.getStreamStructures(headSession);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getContentId()).isEqualTo(OPERATION_CONTENT_ID);
  }

  @Test
  public void addContentOperationToSession() throws Exception {
    ContentMutation mutation = store.editContent();
    mutation.add(PAYLOAD_CONTENT_ID, STREAM_DATA_OPERATION.getStreamPayload());
    ContentMutationResults results = mutation.commit();

    assertThat(results.getContentAdded()).hasSize(1);
    assertThat(results.getContentAdded().get(0)).isEqualTo(PAYLOAD_CONTENT_ID);
  }

  @Test
  public void createNewSession() throws Exception {
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    StreamSession session = store.createNewSession();

    List<StreamStructure> results = store.getStreamStructures(session);
    assertThat(results).hasSize(1);
    StreamStructure streamStructure = results.get(0);
    assertThat(streamStructure.getContentId().getId()).isEqualTo(OPERATION_ID);
  }

  @Test
  public void removeSession() throws Exception {
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    StreamSession session = store.createNewSession();
    store.removeSession(session);

    List<StreamStructure> results = store.getStreamStructures(session);
    assertThat(results).isEmpty();
  }

  @Test
  public void clearHead() throws Exception {
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    store.clearHead();

    List<StreamStructure> results = store.getStreamStructures(store.getHeadSession());
    assertThat(results).isEmpty();
  }

  @Test
  public void editContent() throws Exception {}

  @Test
  public void getStreamFeatures() throws Exception {}

  @Test
  public void getSharedStates() throws Exception {}

  @Test
  public void getPayloads_noPayload() throws Exception {
    List<StreamContentId> contentIds = new ArrayList<>();
    contentIds.add(PAYLOAD_CONTENT_ID);

    List<PayloadWithId> results = store.getPayloads(contentIds);
    assertThat(results).isEmpty();
  }

  @Test
  public void deleteHead_notAllowed() throws Exception {
    RunnableSubject.assertThatRunnable(
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                store.removeSession(StreamSession.newBuilder().setSessionId("$HEAD").build());
              }
            })
        .throwsAnExceptionOfType(IllegalStateException.class);
  }
}
