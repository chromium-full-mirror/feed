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

package com.google.android.libraries.feed.api.common.testing;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;

/** Test support class which creates {@link StreamContentId}s. */
public class ContentIdGenerators {
  private static final StreamContentId FEATURE_CONTENT_ID =
      StreamContentId.newBuilder().setContentDomain("feature").setId(0).setTable("feature").build();
  private static final StreamContentId TOKEN_ID =
      StreamContentId.newBuilder().setContentDomain("token").setId(0).setTable("feature").build();
  private static final StreamContentId SHARED_STATE_CONTENT_ID =
      StreamContentId.newBuilder()
          .setContentDomain("shared-state")
          .setId(0)
          .setTable("feature")
          .build();

  public StreamContentId createFeatureStreamContentId(int id) {
    return FEATURE_CONTENT_ID.toBuilder().setId(id).build();
  }

  public StreamContentId createTokenContentId(int id) {
    return TOKEN_ID.toBuilder().setId(id).build();
  }

  public StreamContentId createSharedStateStreamContentId(int id) {
    return SHARED_STATE_CONTENT_ID.toBuilder().setId(id).build();
  }
}
