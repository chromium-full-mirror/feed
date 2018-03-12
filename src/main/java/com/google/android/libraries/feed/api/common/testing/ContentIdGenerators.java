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

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;

/** Test support class which creates Jardin content ids. */
public class ContentIdGenerators {
  private static final String FEATURE = "feature";
  private static final ContentId FEATURE_CONTENT_ID =
      ContentId.newBuilder().setContentDomain(FEATURE).setId(0).setTable(FEATURE).build();
  private static final ContentId TOKEN_ID =
      ContentId.newBuilder().setContentDomain("token").setId(0).setTable(FEATURE).build();
  private static final ContentId SHARED_STATE_CONTENT_ID =
      ContentId.newBuilder().setContentDomain("shared-state").setId(0).setTable(FEATURE).build();
  private final InternalProtocolUtils protocolUtils = new InternalProtocolUtils();

  public String createFeatureContentId(long id) {
    return protocolUtils.createContentId(FEATURE_CONTENT_ID.toBuilder().setId(id).build());
  }

  public String createTokenContentId(long id) {
    return protocolUtils.createContentId(TOKEN_ID.toBuilder().setId(id).build());
  }

  public String createSharedStateContentId(long id) {
    return protocolUtils.createContentId(SHARED_STATE_CONTENT_ID.toBuilder().setId(id).build());
  }

  public String createRootContentId(int id) {
    return protocolUtils.createContentId(
        ContentId.newBuilder().setContentDomain("stream_root").setId(id).setTable(FEATURE).build());
  }
}
