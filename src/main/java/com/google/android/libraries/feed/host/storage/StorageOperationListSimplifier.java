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

package com.google.android.libraries.feed.host.storage;

import java.util.List;

/**
 * Simplifies a {@link List<StorageOperation>} by combining and removing Operations according to the
 * methods in {@link ContentMutation.Builder}.
 */
class StorageOperationListSimplifier {
  /**
   * Returns a new {@link List<StorageOperation>}, which is a simplification of {@code
   * storageOperations}.
   *
   * <p>The returned list will have a length at most equal to {@code storageOperations}.
   */
  List<StorageOperation> simplify(List<StorageOperation> storageOperations) {
    // TODO: implement
    return storageOperations;
  }
}
