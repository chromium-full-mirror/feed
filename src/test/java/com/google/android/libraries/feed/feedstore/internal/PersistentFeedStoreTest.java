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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.ActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.feedstore.testing.AbstractClearableFeedStoreTest;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import com.google.android.libraries.feed.host.storage.JournalMutation;
import com.google.android.libraries.feed.host.storage.JournalStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamAction;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests of the {@link com.google.android.libraries.feed.feedstore.internal.PersistentFeedStore}
 * class.
 */
@RunWith(RobolectricTestRunner.class)
public class PersistentFeedStoreTest extends AbstractClearableFeedStoreTest {

  private final FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(ArrayList::new);
  private final ContentStorage contentStorage = new InMemoryContentStorage();
  private final MainThreadRunner mainThreadRunner = new MainThreadRunner();
  @Mock private ThreadUtils threadUtils;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Override
  protected Store getStore() {
    return new PersistentFeedStore(
        timingUtils,
        extensionRegistry,
        contentStorage,
        new InMemoryJournalStorage(threadUtils),
        threadUtils,
        mainThreadRunner,
        clock,
        new FeedStoreHelper());
  }

  @Test
  public void clearStorage_contentStorage_failure_getAllContent() {
    ContentStorage contentStorageSpy = spy(contentStorage);
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorageSpy,
            new InMemoryJournalStorage(threadUtils),
            threadUtils,
            mainThreadRunner,
            clock,
            new FeedStoreHelper());
    doAnswer(
            ans -> {
              Consumer<Result<Map<String, byte[]>>> consumer =
                  (Consumer<Result<Map<String, byte[]>>>) ans.getArguments()[1];
              consumer.accept(Result.failure());
              return null;
            })
        .when(contentStorageSpy)
        .getAll(eq(""), any(Consumer.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_contentStorage_failure_commit() {
    ContentStorage contentStorageSpy = spy(contentStorage);
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorageSpy,
            new InMemoryJournalStorage(threadUtils),
            threadUtils,
            mainThreadRunner,
            clock,
            new FeedStoreHelper());

    CommitResult commitResult = store.editContent().add(CONTENT_ID, STREAM_PAYLOAD).commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<PayloadWithId>> payloadsResult =
        store.getPayloads(Collections.singletonList(CONTENT_ID));
    assertThat(payloadsResult.isSuccessful()).isTrue();
    assertThat(payloadsResult.getValue()).hasSize(1);
    assertThat(payloadsResult.getValue().get(0).contentId).isEqualTo(CONTENT_ID);
    assertThat(payloadsResult.getValue().get(0).payload).isEqualTo(STREAM_PAYLOAD);

    doAnswer(
            ans -> {
              Consumer<CommitResult> consumer = (Consumer<CommitResult>) ans.getArguments()[1];
              consumer.accept(CommitResult.FAILURE);
              return null;
            })
        .when(contentStorageSpy)
        .commit(any(ContentMutation.class), any(Consumer.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_journalStorage_failure_getAllJournals() {
    JournalStorage journalStorageSpy = spy(new InMemoryJournalStorage(threadUtils));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorageSpy,
            threadUtils,
            mainThreadRunner,
            clock,
            new FeedStoreHelper());

    boolean commitResult =
        store
            .editSession(STREAM_SESSION)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID).build())
            .commit();
    assertThat(commitResult).isTrue();

    Result<List<StreamSession>> sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(1);
    assertThat(sessionsResult.getValue().get(0)).isEqualTo(STREAM_SESSION);

    doAnswer(
            ans -> {
              Consumer<CommitResult> consumer = (Consumer<CommitResult>) ans.getArguments()[1];
              consumer.accept(CommitResult.FAILURE);
              return null;
            })
        .when(journalStorageSpy)
        .commit(any(JournalMutation.class), any(Consumer.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_journalStorage_failure_deleteJournal() {
    JournalStorage journalStorageSpy = spy(new InMemoryJournalStorage(threadUtils));
    PersistentFeedStore store =
        new PersistentFeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorageSpy,
            threadUtils,
            mainThreadRunner,
            clock,
            new FeedStoreHelper());
    doAnswer(
            ans -> {
              Consumer<Result<Map<String, byte[]>>> consumer =
                  (Consumer<Result<Map<String, byte[]>>>) ans.getArguments()[0];
              consumer.accept(Result.failure());
              return null;
            })
        .when(journalStorageSpy)
        .getAllJournals(any(Consumer.class));

    boolean clearSuccess = store.clearNonActionContent();
    assertThat(clearSuccess).isFalse();
  }

  @Test
  public void clearStorage_allStorage() {
    PersistentFeedStore store = (PersistentFeedStore) getStore();

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
            .editSession(STREAM_SESSION)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID).build())
            .commit();
    assertThat(boolCommitResult).isTrue();
    boolCommitResult =
        store
            .editSession(STREAM_SESSION_2)
            .add(StreamStructure.newBuilder().setContentId(CONTENT_ID_2).build())
            .commit();
    assertThat(boolCommitResult).isTrue();

    Result<List<StreamSession>> sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(2);

    // Actions
    commitResult =
        store
            .editActions()
            .add(ActionType.DISMISS, CONTENT_ID)
            .add(ActionType.DISMISS, CONTENT_ID_2)
            .commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<StreamAction>> dismissActionsResult = store.getAllDismissActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    assertThat(dismissActionsResult.getValue()).hasSize(2);

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

    // Shared state
    sharedStatesResult = store.getSharedStates();
    assertThat(sharedStatesResult.isSuccessful()).isTrue();
    assertThat(sharedStatesResult.getValue()).hasSize(0);

    // Journal
    sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    assertThat(sessionsResult.getValue()).hasSize(0);

    // Actions (should not be cleared)
    dismissActionsResult = store.getAllDismissActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    assertThat(dismissActionsResult.getValue()).hasSize(2);
    assertThat(dismissActionsResult.getValue().get(0).getFeatureContentId()).isEqualTo(CONTENT_ID);
    assertThat(dismissActionsResult.getValue().get(0).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActionsResult.getValue().get(1).getFeatureContentId())
        .isEqualTo(CONTENT_ID_2);
    assertThat(dismissActionsResult.getValue().get(1).getAction()).isEqualTo(ActionType.DISMISS);
  }
}
