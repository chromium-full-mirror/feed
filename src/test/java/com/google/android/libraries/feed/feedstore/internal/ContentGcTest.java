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

import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.ACTION_PROPERTIES_PREFIX;
import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SEMANTIC_PROPERTIES_PREFIX;
import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SHARED_STATE_PREFIX;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.host.storage.CommitResult;
import com.google.android.libraries.feed.api.host.storage.ContentMutation;
import com.google.android.libraries.feed.api.host.storage.ContentMutation.Builder;
import com.google.android.libraries.feed.api.host.storage.ContentOperation;
import com.google.android.libraries.feed.api.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.internalapi.store.LocalActionMutation.ActionType;
import com.google.search.now.feed.client.StreamDataProto.StreamLocalAction;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link ContentGc} class. */
@RunWith(RobolectricTestRunner.class)
public class ContentGcTest {

  @Mock private ContentStorageDirect contentStorage;

  private static final String CONTENT_ID_1 = "contentId1";
  private static final String CONTENT_ID_2 = "contentId2";
  private static final String SEMANTIC_PROPERTIES_1 = SEMANTIC_PROPERTIES_PREFIX + CONTENT_ID_1;
  private static final String SEMANTIC_PROPERTIES_2 = SEMANTIC_PROPERTIES_PREFIX + CONTENT_ID_2;
  private static final String ACTION_PROPERTIES_1 = ACTION_PROPERTIES_PREFIX + CONTENT_ID_1;
  private static final String ACTION_PROPERTIES_2 = ACTION_PROPERTIES_PREFIX + CONTENT_ID_2;

  private final TimingUtils timingUtils = new TimingUtils();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Test
  public void gc() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(CONTENT_ID_1);
    contentKeys.add(CONTENT_ID_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(CONTENT_ID_1).delete(CONTENT_ID_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_accessible() {
    Set<String> accessibleContent = new HashSet<>(Collections.singletonList(CONTENT_ID_1));
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(CONTENT_ID_1);
    contentKeys.add(CONTENT_ID_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(CONTENT_ID_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_reserved() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = new HashSet<>(Collections.singletonList(CONTENT_ID_1));
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(CONTENT_ID_1);
    contentKeys.add(CONTENT_ID_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(CONTENT_ID_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_actionProperties_noAction() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(ACTION_PROPERTIES_1);
    contentKeys.add(ACTION_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder()
            .delete(ACTION_PROPERTIES_1)
            .delete(ACTION_PROPERTIES_2)
            .build()
            .getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_actionProperties_validAction() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions =
        new HashSet<>(
            Collections.singletonList(
                StreamUploadableAction.newBuilder()
                    .setUploadAttempts(1)
                    .setFeatureContentId(CONTENT_ID_1)
                    .build()));
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(ACTION_PROPERTIES_1);
    contentKeys.add(ACTION_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(ACTION_PROPERTIES_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_actionProperties_accessibleContent() {
    Set<String> accessibleContent = Collections.singleton(CONTENT_ID_1);
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(CONTENT_ID_1);
    contentKeys.add(ACTION_PROPERTIES_1);
    contentKeys.add(ACTION_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(ACTION_PROPERTIES_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_semanticProperties_noAction() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(SEMANTIC_PROPERTIES_1);
    contentKeys.add(SEMANTIC_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder()
            .delete(SEMANTIC_PROPERTIES_1)
            .delete(SEMANTIC_PROPERTIES_2)
            .build()
            .getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_semanticProperties_validAction() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions =
        new HashSet<>(
            Collections.singletonList(
                StreamLocalAction.newBuilder()
                    .setAction(ActionType.DISMISS)
                    .setFeatureContentId(CONTENT_ID_1)
                    .build()));

    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(SEMANTIC_PROPERTIES_1);
    contentKeys.add(SEMANTIC_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(SEMANTIC_PROPERTIES_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_semanticProperties_accessibleContent() {
    Set<String> accessibleContent = Collections.singleton(CONTENT_ID_1);
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(CONTENT_ID_1);
    contentKeys.add(SEMANTIC_PROPERTIES_1);
    contentKeys.add(SEMANTIC_PROPERTIES_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(SEMANTIC_PROPERTIES_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  @Test
  public void gc_prefixed_sharedState() {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Set<StreamLocalAction> dismissActions = Collections.emptySet();
    Set<StreamUploadableAction> uploadableActions = Collections.emptySet();
    List<String> contentKeys = new ArrayList<>();
    contentKeys.add(SHARED_STATE_PREFIX + CONTENT_ID_1);
    contentKeys.add(CONTENT_ID_2);
    mockContentStorageWithContents(contentKeys);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            () -> dismissActions,
            () -> uploadableActions,
            contentStorage,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture());
    List<ContentOperation> expectedOperations =
        new Builder().delete(CONTENT_ID_2).build().getOperations();
    List<ContentOperation> resultOperations = contentMutationCaptor.getValue().getOperations();
    assertListsContainSameElements(expectedOperations, resultOperations);
  }

  private void mockContentStorageWithContents(List<String> contentKeys) {
    when(contentStorage.getAllKeys()).thenReturn(Result.success(contentKeys));
    when(contentStorage.commit(any(ContentMutation.class))).thenReturn(CommitResult.SUCCESS);
  }

  private void assertListsContainSameElements(
      List<ContentOperation> expectedOperations, List<ContentOperation> resultOperations) {
    assertThat(resultOperations.size()).isEqualTo(expectedOperations.size());
    assertThat(resultOperations.containsAll(expectedOperations)).isTrue();
  }
}
