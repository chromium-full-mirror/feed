package com.google.android.libraries.feed.feedstore.internal; // Copyright 2018 The Feed Authors.
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

import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SEMANTIC_PROPERTIES_PREFIX;
import static com.google.android.libraries.feed.feedstore.internal.FeedStoreConstants.SHARED_STATE_PREFIX;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.android.libraries.feed.host.storage.ContentMutation;
import com.google.android.libraries.feed.host.storage.ContentMutation.Builder;
import com.google.android.libraries.feed.host.storage.ContentStorage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link ContentGc} class. */
@RunWith(RobolectricTestRunner.class)
public class ContentGcTest {

  @Mock private ContentStorage contentStorage;

  private static final String CONTENT_ID_1 = "contentId1";
  private static final String CONTENT_ID_2 = "contentId2";
  private static final byte[] CONTENT_BYTES_1 = new byte[] {42, 17, 98, 4};
  private static final byte[] CONTENT_BYTES_2 = new byte[] {51, 8, 27, 36};

  private final MainThreadRunner mainThreadRunner = new MainThreadRunner();
  private final TimingUtils timingUtils = new TimingUtils();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Test
  public void gc() throws Exception {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Map<String, byte[]> contentStorageMap = new TreeMap<>();
    contentStorageMap.put(CONTENT_ID_1, CONTENT_BYTES_1);
    contentStorageMap.put(CONTENT_ID_2, CONTENT_BYTES_2);
    mockContentStorageWithContents(contentStorageMap);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            contentStorage,
            mainThreadRunner,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture(), any(Consumer.class));
    ContentMutation expected = new Builder().delete(CONTENT_ID_1).delete(CONTENT_ID_2).build();
    assertThat(contentMutationCaptor.getValue()).isEqualTo(expected);
  }

  @Test
  public void gc_accessible() throws Exception {
    Set<String> accessibleContent = new HashSet<>(Collections.singletonList(CONTENT_ID_1));
    Set<String> reservedContentIds = Collections.emptySet();
    Map<String, byte[]> contentStorageMap = new HashMap<>();
    contentStorageMap.put(CONTENT_ID_1, CONTENT_BYTES_1);
    contentStorageMap.put(CONTENT_ID_2, CONTENT_BYTES_2);
    mockContentStorageWithContents(contentStorageMap);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            contentStorage,
            mainThreadRunner,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture(), any(Consumer.class));
    ContentMutation expected = new Builder().delete(CONTENT_ID_2).build();
    assertThat(contentMutationCaptor.getValue()).isEqualTo(expected);
  }

  @Test
  public void gc_reserved() throws Exception {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = new HashSet<>(Collections.singletonList(CONTENT_ID_1));
    Map<String, byte[]> contentStorageMap = new HashMap<>();
    contentStorageMap.put(CONTENT_ID_1, CONTENT_BYTES_1);
    contentStorageMap.put(CONTENT_ID_2, CONTENT_BYTES_2);
    mockContentStorageWithContents(contentStorageMap);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            contentStorage,
            mainThreadRunner,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture(), any(Consumer.class));
    ContentMutation expected = new Builder().delete(CONTENT_ID_2).build();
    assertThat(contentMutationCaptor.getValue()).isEqualTo(expected);
  }

  @Test
  public void gc_prefixed_semanticProperties() throws Exception {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Map<String, byte[]> contentStorageMap = new HashMap<>();
    contentStorageMap.put(SEMANTIC_PROPERTIES_PREFIX + CONTENT_ID_1, CONTENT_BYTES_1);
    contentStorageMap.put(CONTENT_ID_2, CONTENT_BYTES_2);
    mockContentStorageWithContents(contentStorageMap);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            contentStorage,
            mainThreadRunner,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture(), any(Consumer.class));
    ContentMutation expected = new Builder().delete(CONTENT_ID_2).build();
    assertThat(contentMutationCaptor.getValue()).isEqualTo(expected);
  }

  @Test
  public void gc_prefixed_sharedState() throws Exception {
    Set<String> accessibleContent = Collections.emptySet();
    Set<String> reservedContentIds = Collections.emptySet();
    Map<String, byte[]> contentStorageMap = new HashMap<>();
    contentStorageMap.put(SHARED_STATE_PREFIX + CONTENT_ID_1, CONTENT_BYTES_1);
    contentStorageMap.put(CONTENT_ID_2, CONTENT_BYTES_2);
    mockContentStorageWithContents(contentStorageMap);
    ContentGc contentGc =
        new ContentGc(
            () -> accessibleContent,
            reservedContentIds,
            contentStorage,
            mainThreadRunner,
            timingUtils);
    contentGc.gc();
    ArgumentCaptor<ContentMutation> contentMutationCaptor =
        ArgumentCaptor.forClass(ContentMutation.class);
    verify(contentStorage).commit(contentMutationCaptor.capture(), any(Consumer.class));
    ContentMutation expected = new Builder().delete(CONTENT_ID_2).build();
    assertThat(contentMutationCaptor.getValue()).isEqualTo(expected);
  }

  private void mockContentStorageWithContents(Map<String, byte[]> contentStorageMap) {
    doAnswer(
        ans -> {
          Consumer<Result<Map<String, byte[]>>> consumer =
              (Consumer<Result<Map<String, byte[]>>>) ans.getArguments()[1];
          consumer.accept(Result.success(contentStorageMap));
          return null;
        })
        .when(contentStorage)
        .getAll(eq(""), any(Consumer.class));
    doAnswer(
        ans -> {
          Consumer<CommitResult> consumer = (Consumer<CommitResult>) ans.getArguments()[1];
          consumer.accept(CommitResult.SUCCESS);
          return null;
        })
        .when(contentStorage)
        .commit(any(ContentMutation.class), any(Consumer.class));
  }
}
