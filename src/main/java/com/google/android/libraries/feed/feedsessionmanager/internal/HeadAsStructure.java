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

import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.functional.Function;
import com.google.android.libraries.feed.common.functional.Predicate;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.common.time.TimingUtils.ElapsedTimeTracker;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;

/**
 * This class will load the current $HEAD to allow for structured access to the defined contents and
 * structure. Currently, the only supported access is filtering the tree in a pre-order traversal of
 * the structure, {@see #filter}.
 */
public class HeadAsStructure {
  private static final String TAG = "HeadFilter";

  private final Store store;
  private final TimingUtils timingUtils;
  private final ThreadUtils threadUtils;

  private final Object lock = new Object();

  @GuardedBy("lock")
  private boolean initalized;

  @VisibleForTesting Map<String, List<TreeNode>> tree = new HashMap<>();
  @VisibleForTesting Map<String, TreeNode> content = new HashMap<>();
  @VisibleForTesting TreeNode root;

  /**
   * Define a Node within the tree formed by $HEAD. This contains both the structure and content.
   * Allowing filtering of either the structure or content.
   */
  public static class TreeNode {
    final StreamStructure streamStructure;
    StreamPayload streamPayload;

    TreeNode(StreamStructure streamStructure) {
      this.streamStructure = streamStructure;
    }

    public StreamStructure getStreamStructure() {
      return streamStructure;
    }

    public StreamPayload getStreamPayload() {
      return streamPayload;
    }
  }

  public HeadAsStructure(Store store, TimingUtils timingUtils, ThreadUtils threadUtils) {
    this.store = store;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
  }

  /**
   * Capture the state of the current $HEAD, returning success or failure through a {@link
   * Consumer}. The snapshot of head will not be updated if $HEAD is updated. Initialization may
   * only be called once. This method must be called on a background thread.
   */
  public void initialize(Consumer<Result<Void>> consumer) {
    Logger.i(TAG, "initialize HeadFilter");
    threadUtils.checkNotMainThread();
    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);

    synchronized (lock) {
      if (initalized) {
        consumer.accept(Result.failure());
        return;
      }
      if (!buildTree()) {
        timeTracker.stop("", "buildTree Failed");
        consumer.accept(Result.failure());
        return;
      }
      if (!bindChildren()) {
        timeTracker.stop("", "bindChildren Failed");
        consumer.accept(Result.failure());
        return;
      }
      initalized = true;
    }

    timeTracker.stop("task", "HeadFilter.initialize", "content", content.size());
    consumer.accept(Result.success(null));
  }

  /**
   * Using the current $HEAD, transformation the {@link TreeNode} stored at each node to {@code T},
   * then applying the filter {@link Predicate} to the result to create a {@link List} of {@code T}
   * which match the Predicate. If the transform returns {@code null}, the node is skipped.
   *
   * <p>This method must be called after {@link #initalized}. This method may run on the main
   * thread.
   */
  // The Nullness checker requires specifying the Nullable vs. NonNull state explicitly since it
  // can't be inferred from T.  This is done here and in the methods called below.
  public <T> Result<List</*@NonNull*/ T>> filter(
      Function<TreeNode, /*@Nullable*/ T> transformer, Predicate</*@NonNull*/ T> filter) {
    Logger.i(TAG, "filterHead");
    synchronized (lock) {
      if (!initalized) {
        Logger.e(TAG, "HeadFilter has not been initialized");
        return Result.failure();
      }
    }

    ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
    List</*@NonNull*/ T> filteredList = new ArrayList<>();
    traverseHead(transformer, filter, filteredList);
    Logger.i(TAG, "filterList size %s", filteredList.size());
    timeTracker.stop("task", "HeadFilter.filterHead");
    return Result.success(filteredList);
  }

  private <T> void traverseHead(
      Function<TreeNode, /*@Nullable*/ T> transformer,
      Predicate</*@NonNull*/ T> filter,
      List</*@NonNull*/ T> results) {
    TreeNode r = Validators.checkNotNull(root);
    traverseNode(r, transformer, filter, results);
  }

  private <T> void traverseNode(
      TreeNode node,
      Function<TreeNode, /*@Nullable*/ T> transformer,
      Predicate</*@NonNull*/ T> filter,
      List</*@NonNull*/ T> results) {
    if (node.streamPayload == null) {
      Logger.w(TAG, "Found unbound node %s", node.streamStructure.getContentId());
      return;
    }
    T data = transformer.apply(node);
    if (data != null) {
      if (filter.test(data)) {
        results.add(data);
      }
    }
    List<TreeNode> children = tree.get(node.streamStructure.getContentId());
    if (children != null) {
      for (TreeNode child : children) {
        traverseNode(child, transformer, filter, results);
      }
    }
  }

  private boolean bindChildren() {
    Result<List<PayloadWithId>> payloadResult =
        store.getPayloads(new ArrayList<>(content.keySet()));
    if (!payloadResult.isSuccessful()) {
      Logger.e(TAG, "Unable to get payloads");
      return false;
    }
    for (PayloadWithId payloadWithId : payloadResult.getValue()) {
      TreeNode node = content.get(payloadWithId.contentId);
      if (node == null) {
        // This shouldn't happen
        Logger.w(TAG, "Unable to find tree content for %s", payloadWithId.contentId);
        continue;
      }
      node.streamPayload = payloadWithId.payload;
    }
    return true;
  }

  private boolean buildTree() {
    Result<List<StreamStructure>> headResult = store.getStreamStructures(Store.HEAD);
    if (!headResult.isSuccessful()) {
      Logger.e(TAG, "Unable to load $HEAD");
      return false;
    }
    List<StreamStructure> head = headResult.getValue();
    Logger.i(TAG, "size of $head %s", head.size());
    for (StreamStructure structure : head) {
      switch (structure.getOperation()) {
        case CLEAR_ALL:
          continue;
        case UPDATE_OR_APPEND:
          updateOrAppend(structure);
          break;
        case REMOVE:
          remove(structure);
          break;
        default:
          Logger.w(TAG, "Unsupported Operation %s", structure.getOperation());
          break;
      }
    }
    if (root == null) {
      Logger.e(TAG, "Root was not found");
      return false;
    }
    return true;
  }

  private void updateOrAppend(StreamStructure structure) {
    String contentId = structure.getContentId();
    if (content.containsKey(contentId)) {
      // this is an update, ignore it
      return;
    }
    TreeNode node = new TreeNode(structure);
    content.put(contentId, node);
    updateTreeStructure(contentId);
    if (!structure.hasParentContentId()) {
      // this is the root
      if (root != null) {
        Logger.e(TAG, "Found Multiple roots");
      }
      root = node;
      return;
    }

    // add this as a child of the parent
    List<TreeNode> parentChildren = updateTreeStructure(structure.getParentContentId());
    parentChildren.add(node);
  }

  private void remove(StreamStructure structure) {
    String contentId = structure.getContentId();
    String parentId = structure.hasParentContentId() ? structure.getParentContentId() : null;
    TreeNode node = content.get(contentId);
    if (node == null) {
      Logger.w(TAG, "Unable to find StreamStructure %s to remove", contentId);
      return;
    }
    if (parentId == null) {
      // TODO: Need to support this.
      Logger.w(TAG, "Removing Root, Not yet supported");
      return;
    }
    List<TreeNode> parentChildren = tree.get(parentId);
    if (parentChildren == null) {
      Logger.w(TAG, "Parent %s not found, unable to remove", parentId, contentId);
    } else if (!parentChildren.remove(node)) {
      Logger.w(TAG, "Removing %s, not found in parent %s", contentId, parentId);
    }
    tree.remove(contentId);
    content.remove(contentId);
  }

  private List<TreeNode> updateTreeStructure(String contentId) {
    List<TreeNode> children = tree.get(contentId);
    if (children == null) {
      children = new ArrayList<>();
      tree.put(contentId, children);
    }
    return children;
  }
}
