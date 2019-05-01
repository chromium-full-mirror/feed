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

package com.google.android.libraries.feed.feedstore.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.testing.FakeMainThreadRunner;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedstore.testing.AbstractClearableFeedStoreTest;
import com.google.android.libraries.feed.feedstore.testing.DelegatingContentStorage;
import com.google.android.libraries.feed.feedstore.testing.DelegatingJournalStorage;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.android.libraries.feed.internalapi.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.internalapi.common.PayloadWithId;
import com.google.android.libraries.feed.internalapi.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.internalapi.common.ThreadUtils;
import com.google.android.libraries.feed.internalapi.store.LocalActionMutation.ActionType;
import com.google.android.libraries.feed.internalapi.store.Store;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamLocalAction;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.wire.feed.OpaqueActionDataForTestProto.OpaqueActionDataForTest;
import com.google.search.now.wire.feed.OpaqueActionDataProto.OpaqueActionData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests of the {@link com.google.android.libraries.feed.feedstore.internal.PersistentFeedStore}
 * class.
 */
@RunWith(RobolectricTestRunner.class)
public class PersistentFeedStoreTest extends AbstractClearableFeedStoreTest {

  private ThreadUtils threadUtils = mock(ThreadUtils.class);
  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorageDirect contentStorage = new InMemoryContentStorage();
  private final JournalStorageDirect journalStorage = new InMemoryJournalStorage();
  private FakeMainThreadRunner mainThreadRunner;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    mainThreadRunner = FakeMainThreadRunner.runTasksImmediately();
  }

  @Override
  protected Store getStore(MainThreadRunner mainThreadRunner) {
    return new PersistentFeedStore(
        timingUtils,
        extensionRegistry,
        contentStorage,
        journalStorage,
        threadUtils,
        fakeClock,
        new FeedStoreHelper());
  }

  @Test
  public void clearStorage_contentStorage_failure_getAllContent() {
    ContentStorageDirect contentStorageSpy = spy(new DelegatingContentStorage(contentStorage));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorageSpy,
            journalStorage,
            threadUtils,
            fakeClock,
            new FeedStoreHelper());
    doAnswer(ans -> Result.failure()).when(contentStorageSpy).getAllKeys();

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_contentStorage_failure_commit() {
    ContentStorageDirect contentStorageSpy = spy(new DelegatingContentStorage(contentStorage));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorageSpy,
            journalStorage,
            threadUtils,
            fakeClock,
            new FeedStoreHelper());

    CommitResult commitResult = store.editContent().add(CONTENT_ID, STREAM_PAYLOAD).commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<PayloadWithId>> payloadsResult =
        store.getPayloads(Collections.singletonList(CONTENT_ID));
    assertThat(payloadsResult.isSuccessful()).isTrue();
    assertThat(payloadsResult.getValue()).hasSize(1);
    assertThat(payloadsResult.getValue().get(0).contentId).isEqualTo(CONTENT_ID);
    assertThat(payloadsResult.getValue().get(0).payload).isEqualTo(STREAM_PAYLOAD);
    doAnswer(ans -> CommitResult.FAILURE)
        .when(contentStorageSpy)
        .commit(any(ContentMutation.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_journalStorage_failure_getAllJournals() {
    JournalStorageDirect journalStorageSpy = spy(new DelegatingJournalStorage(journalStorage));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorageSpy,
            threadUtils,
            fakeClock,
            new FeedStoreHelper());

    boolean commitResult =
        store
            .editSession(SESSION_ID)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID).build())
            .commit();
    assertThat(commitResult).isTrue();

    Result<List<String>> sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(1);
    assertThat(sessionsResult.getValue().get(0)).isEqualTo(SESSION_ID);

    doAnswer(ans -> CommitResult.FAILURE)
        .when(journalStorageSpy)
        .commit(any(JournalMutation.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_journalStorage_failure_deleteJournal() {
    JournalStorageDirect journalStorageSpy = spy(new DelegatingJournalStorage(journalStorage));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorageSpy,
            threadUtils,
            fakeClock,
            new FeedStoreHelper());
    doAnswer(ans -> Result.failure()).when(journalStorageSpy).getAllJournals();

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_allStorage() {
    PersistentFeedStore store = (PersistentFeedStore) getStore(mainThreadRunner);

    /*
    SETUP
    */

    // Payload
    CommitResult commitResult =
        store
            .editContent()
            .add(CONTENT_ID, STREAM_PAYLOAD)
            .add(CONTENT_ID_2, STREAM_PAYLOAD_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<PayloadWithId>> payloadsResult =
        store.getPayloads(Arrays.asList(CONTENT_ID, CONTENT_ID_2));
    assertThat(payloadsResult.isSuccessful()).isTrue();
    assertThat(payloadsResult.getValue()).hasSize(2);

    // Semantic properties
    commitResult =
        store
            .editSemanticProperties()
            .add(CONTENT_ID, ByteString.copyFrom(SEMANTIC_PROPERTIES))
            .add(CONTENT_ID_2, ByteString.copyFrom(SEMANTIC_PROPERTIES_2))
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<SemanticPropertiesWithId>> semanticPropertiesResult =
        store.getSemanticProperties(Arrays.asList(CONTENT_ID, CONTENT_ID_2));
    assertThat(semanticPropertiesResult.isSuccessful()).isTrue();
    assertThat(semanticPropertiesResult.getValue()).hasSize(2);

    // Action properties
    OpaqueActionData data1 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(CONTENT_ID).build())
            .build();
    OpaqueActionData data2 =
        OpaqueActionData.newBuilder()
            .setExtension(
                OpaqueActionDataForTest.opaqueActionDataForTestExtension,
                OpaqueActionDataForTest.newBuilder().setId(CONTENT_ID_2).build())
            .build();
    commitResult =
        store.editActionProperties().add(CONTENT_ID, data1).add(CONTENT_ID_2, data2).commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<ActionPropertiesWithId>> actionPropertiesResult =
        store.getActionProperties(Arrays.asList(CONTENT_ID, CONTENT_ID_2));
    assertThat(actionPropertiesResult.isSuccessful()).isTrue();
    assertThat(actionPropertiesResult.getValue()).hasSize(2);
    assertThat(actionPropertiesResult.getValue())
        .containsExactly(
            new ActionPropertiesWithId(CONTENT_ID, data1),
            new ActionPropertiesWithId(CONTENT_ID_2, data2));

    // Shared State
    commitResult =
        store
            .editContent()
            .add(CONTENT_ID, STREAM_PAYLOAD_SHARED_STATE)
            .add(CONTENT_ID_2, STREAM_PAYLOAD_SHARED_STATE_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<StreamSharedState>> sharedStatesResult = store.getSharedStates();
    assertThat(sharedStatesResult.isSuccessful()).isTrue();
    assertThat(sharedStatesResult.getValue()).hasSize(2);

    // Journal
    boolean boolCommitResult =
        store
            .editSession(SESSION_ID)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID).build())
            .commit();
    assertThat(boolCommitResult).isTrue();
    boolCommitResult =
        store
            .editSession(SESSION_ID_2)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID_2).build())
            .commit();
    assertThat(boolCommitResult).isTrue();

    Result<List<String>> sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(2);

    // Actions
    commitResult =
        store
            .editLocalActions()
            .add(ActionType.DISMISS, CONTENT_ID)
            .add(ActionType.DISMISS, CONTENT_ID_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<StreamLocalAction>> dismissActionsResult = store.getAllDismissLocalActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    assertThat(dismissActionsResult.getValue()).hasSize(2);

    commitResult =
        store
            .editUploadableActions()
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
                CONTENT_ID)
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID_2).build(),
                CONTENT_ID_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<Set<StreamUploadableAction>> uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.isSuccessful()).isTrue();
    assertThat(uploadableActionsResult.getValue()).hasSize(2);

    /*
    CLEAR
    */

    assertThat(store.clearNonActionContent()).isTrue();

    /*
    VERIFICATION
    */

    // Payload
    payloadsResult = store.getPayloads(Collections.singletonList(CONTENT_ID));
    assertThat(payloadsResult.isSuccessful()).isTrue();
    assertThat(payloadsResult.getValue()).hasSize(0);

    // Semantic properties (should not be cleared)
    semanticPropertiesResult = store.getSemanticProperties(Arrays.asList(CONTENT_ID, CONTENT_ID_2));
    assertThat(semanticPropertiesResult.isSuccessful()).isTrue();
    assertThat(semanticPropertiesResult.getValue()).hasSize(2);
    assertThat(semanticPropertiesResult.getValue())
        .containsExactly(
            new SemanticPropertiesWithId(CONTENT_ID, SEMANTIC_PROPERTIES),
            new SemanticPropertiesWithId(CONTENT_ID_2, SEMANTIC_PROPERTIES_2));

    // Action properties
    actionPropertiesResult = store.getActionProperties(Arrays.asList(CONTENT_ID, CONTENT_ID_2));
    assertThat(actionPropertiesResult.isSuccessful()).isTrue();
    assertThat(actionPropertiesResult.getValue()).hasSize(0);

    // Shared state
    sharedStatesResult = store.getSharedStates();
    assertThat(sharedStatesResult.isSuccessful()).isTrue();
    assertThat(sharedStatesResult.getValue()).hasSize(0);

    // Journal
    sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(0);

    // Actions (should not be cleared)
    dismissActionsResult = store.getAllDismissLocalActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    assertThat(dismissActionsResult.getValue()).hasSize(2);
    assertThat(dismissActionsResult.getValue().get(0).getFeatureContentId()).isEqualTo(CONTENT_ID);
    assertThat(dismissActionsResult.getValue().get(0).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActionsResult.getValue().get(1).getFeatureContentId())
        .isEqualTo(CONTENT_ID_2);
    assertThat(dismissActionsResult.getValue().get(1).getAction()).isEqualTo(ActionType.DISMISS);

    // UploadableActions (should be cleared)
    uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.isSuccessful()).isTrue();
    assertThat(uploadableActionsResult.getValue()).isEmpty();
  }

  @Test
  public void uploadActions_removedBeforeUpserted_stillUpserts() {
    PersistentFeedStore store = (PersistentFeedStore) getStore(mainThreadRunner);

    CommitResult commitResult =
        store
            .editUploadableActions()
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
                CONTENT_ID)
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID_2).build(),
                CONTENT_ID_2)
            .remove(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID_2).build(),
                CONTENT_ID_2)
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID_2).build(),
                CONTENT_ID_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<Set<StreamUploadableAction>> uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.isSuccessful()).isTrue();
    assertThat(uploadableActionsResult.getValue()).hasSize(2);
    assertThat(uploadableActionsResult.getValue())
        .containsAtLeast(
            StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
            StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID_2).build());
  }

  @Test
  public void uploadActions_removedAfterCommittedUpsert_stillRemoves() {
    PersistentFeedStore store = (PersistentFeedStore) getStore(mainThreadRunner);

    CommitResult commitResult =
        store
            .editUploadableActions()
            .upsert(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
                CONTENT_ID)
            .commit();

    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);
    Result<Set<StreamUploadableAction>> uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.getValue()).hasSize(1);
    assertThat(uploadableActionsResult.getValue())
        .contains(StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build());

    commitResult =
        store
            .editUploadableActions()
            .remove(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
                CONTENT_ID)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.isSuccessful()).isTrue();
    assertThat(uploadableActionsResult.getValue()).isEmpty();
  }

  @Test
  public void uploadActions_removedNonExistantAction_succeeds() {
    PersistentFeedStore store = (PersistentFeedStore) getStore(mainThreadRunner);

    CommitResult commitResult =
        store
            .editUploadableActions()
            .remove(
                StreamUploadableAction.newBuilder().setFeatureContentId(CONTENT_ID).build(),
                CONTENT_ID)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<Set<StreamUploadableAction>> uploadableActionsResult = store.getAllUploadableActions();
    assertThat(uploadableActionsResult.isSuccessful()).isTrue();
    assertThat(uploadableActionsResult.getValue()).isEmpty();
  }
}
