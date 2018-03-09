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

/**
 * This represents a Cursor through the children of a {@link ModelFeature}. A Cursor will provide
 * forward only access to the children of the container. When a cursor is created by calling {@link
 * ModelFeature#getCursor()}, it is positioned at the first child. Access to the next child is done
 * through the {@link ProcessNextChild} interface. The interface provides a method which will
 * process the next child.
 *
 * <p>Cursors are designed so they can be held onto by the UI. The Cursor can be invalidated, for
 * example on a Stream refresh, all existing cursors will become invalid. An invalidated cursor will
 * not process any remaining children. All resources will be freed.
 */
public interface ModelCursor {
  /**
   * A callback method which is called during {@link #processNextChild(ModelChild)}. This allows the
   * caller to process the Child.
   */
  interface ProcessNextChild {
    /**
     * Called to process the {@link com.google.android.libraries.feed.api.modelprovider.ModelChild}.
     */
    void processNextChild(ModelChild child);
  }

  /**
   * Return {@code true} if the child was valid and passed to ProcessNextChild method. If the cursor
   * is at the end of the list, or if it has been invalidated, then this will always return {@code
   * false}.
   *
   * <p>The cursor is designed to support a callback instead of a {@code hasNext(), next()} model to
   * avoid race conditions which can occur with multiple calls on an object. This is designed to
   * allow Lambda function use at the calling site.
   */
  boolean advanceCursor(ProcessNextChild processor);

  /**
   * Boolean indicating the Cursor is at the end. When the cursor is at the end it will always
   * return {@code false} from advanceCursor. If the cursor has been released this this will return
   * {@code true}.
   */
  boolean isAtEnd();
}
