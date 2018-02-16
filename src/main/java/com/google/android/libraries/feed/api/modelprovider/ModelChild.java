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

package com.google.android.libraries.feed.api.modelprovider;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;

/**
 * Abstract Interface defining a Child of a feature which may be returned by a {@link ModelCursor}.
 * A ModelChild must be bound to either a feature or token.
 */
public interface ModelChild {

  /** Define the concrete type of the children. */
  enum Type {
    // A feature that isn't yet bound to a specific data type
    // This should never be exposed outside of the model.
    UNBOUND,

    // A stream feature child.
    FEATURE,

    // The child is a continuation token.  This means the cursor is at end
    // and a request has been made to create a new cursor with additional
    // children.
    TOKEN
  }

  /** Returns the type of the child. */
  Type getType();

  /** Returns the ContentId of the child, all children will have a Content ID. */
  StreamContentId getContentId();

  /** Returns true there is a parent to this child; false if this is a root. */
  boolean hasParentId();

  /**
   * Bind a {@link ModelFeature} to the child. This can only be called when if the ModelChild is
   * currently {@code UNBOUND}, otherwise it throws an {@link IllegalStateException}.
   */
  void bindFeature(ModelFeature modelFeature);

  /**
   * Bind a {@link ModelToken} to the child. This can only be called when if the ModelChild is
   * currently {@code UNBOUND}, otherwise it throws an {@link IllegalStateException}.
   */
  void bindToken(ModelToken modelToken);

  /**
   * Returns the parent contentId of the child, this may return {@link
   * StreamContentId#getDefaultInstance()} if the child is a root.
   */
  StreamContentId getParentId();

  /**
   * Returns a the {@link ModelFeature}. This will throw an {@link IllegalStateException} if the
   * {@link Type} is not equal to {@code FEATURE}.
   */
  ModelFeature getModelFeature();

  /**
   * Returns the {@link ModelToken}. This will throw an {@link IllegalStateException} if the {@link
   * Type} is not equal to {@code TOKEN}.
   */
  ModelToken getModelToken();
}
