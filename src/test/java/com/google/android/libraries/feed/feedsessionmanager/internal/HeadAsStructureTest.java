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

package com.google.android.libraries.feed.feedsessionmanager.internal;

import static com.google.android.libraries.feed.api.common.testing.ContentIdGenerators.ROOT_PREFIX;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.common.testing.InternalProtocolBuilder;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedsessionmanager.internal.HeadAsStructure.TreeNode;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link HeadAsStructure}. */
@RunWith(RobolectricTestRunner.class)
public class HeadAsStructureTest {

  @Mock private Store store;
  @Mock private ThreadUtils threadUtils;

  private final TimingUtils timingUtils = new TimingUtils();
  private final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private final String rootContentId = idGenerators.createRootContentId(0);

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testUninitialized() {
    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    Result<List<Void>> result = headAsStructure.filter(payload -> null);
    assertThat(result.isSuccessful()).isFalse();
  }

  @Test
  public void testInitialization() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    assertThat(headAsStructure.root).isNotNull();
    assertThat(headAsStructure.content).hasSize(4);
    assertThat(headAsStructure.tree).hasSize(4);
    TreeNode root = headAsStructure.content.get(rootContentId);
    assertThat(root).isNotNull();
    assertThat(headAsStructure.root).isEqualTo(root);
    List<TreeNode> rootChildren = headAsStructure.tree.get(rootContentId);
    assertThat(rootChildren).isNotNull();
    assertThat(rootChildren).hasSize(3);
  }

  @Test
  public void testInitialization_withRemove() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    String contentId = idGenerators.createFeatureContentId(2);
    protocolBuilder.removeFeature(contentId, rootContentId);
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    assertThat(headAsStructure.root).isNotNull();
    assertThat(headAsStructure.content).hasSize(3);
    assertThat(headAsStructure.tree).hasSize(3);
    List<TreeNode> rootChildren = headAsStructure.tree.get(rootContentId);
    assertThat(rootChildren).isNotNull();
    assertThat(rootChildren).hasSize(2);
  }

  @Test
  public void testInitialization_doubleInitialization() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isFalse());
  }

  @Test
  public void testInitialization_buildTreeFailure() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID)).thenReturn(Result.failure());
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isFalse());
  }

  @Test
  public void testInitialization_bindTreeFailure() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any())).thenReturn(Result.failure());

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isFalse());
  }

  @Test
  public void testFiltering_payload() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    Result<List<StreamFeature>> results =
        headAsStructure.filter(
            node -> {
              StreamPayload payload = node.getStreamPayload();
              assertThat(payload).isNotNull();
              if (payload.hasStreamFeature()) {
                return payload.getStreamFeature();
              } else {
                return null;
              }
            });
    assertThat(results.isSuccessful()).isTrue();
    assertThat(results.getValue()).hasSize(4);
  }

  @Test
  public void testFiltering_childrenOfRoot() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    Result<List<String>> results =
        headAsStructure.filter(
            node ->
                !node.getStreamStructure().getContentId().startsWith(ROOT_PREFIX)
                    ? node.getStreamStructure().getContentId()
                    : null);
    assertThat(results.isSuccessful()).isTrue();
    assertThat(results.getValue()).hasSize(3);
  }

  @Test
  public void testFiltering_withRemove() {
    InternalProtocolBuilder protocolBuilder = new InternalProtocolBuilder();
    protocolBuilder.addClearOperation().addRootFeature();
    for (int i = 0; i < 3; i++) {
      String contentId = idGenerators.createFeatureContentId(i);
      protocolBuilder.addFeature(contentId, rootContentId);
    }
    String contentId = idGenerators.createFeatureContentId(2);
    protocolBuilder.removeFeature(contentId, rootContentId);
    when(store.getStreamStructures(Store.HEAD_SESSION_ID))
        .thenReturn(Result.success(protocolBuilder.buildAsStreamStructure()));
    when(store.getPayloads(any()))
        .thenReturn(Result.success(protocolBuilder.buildAsPayloadWithId()));

    HeadAsStructure headAsStructure = new HeadAsStructure(store, timingUtils, threadUtils);
    headAsStructure.initialize(result -> assertThat(result.isSuccessful()).isTrue());
    Result<List<StreamFeature>> results =
        headAsStructure.filter(
            node -> {
              StreamPayload payload = node.getStreamPayload();
              assertThat(payload).isNotNull();
              if (payload.hasStreamFeature()) {
                return payload.getStreamFeature();
              } else {
                return null;
              }
            });
    assertThat(results.isSuccessful()).isTrue();
    assertThat(results.getValue()).hasSize(3);
  }
}
