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

package com.google.android.libraries.feed.api.common;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import javax.annotation.Nullable;

/** Utility class defining methods which manage the internal protocol. */
public class InternalProtocolUtils {

  /** Returns a key for a {@link StreamContentId}. */
  public String getContentIdKey(StreamContentId contentId) {
    return String.format(
        "%s::%s::%s", contentId.getContentDomain(), contentId.getId(), contentId.getTable());
  }

  /** Create a {@link StreamContentId} from a {@link ContentId}. */
  public StreamContentId createContentId(ContentId contentId) {
    return StreamContentId.newBuilder()
        .setContentDomain(contentId.getContentDomain())
        .setId(contentId.getId())
        .setTable(contentId.getTable())
        .build();
  }

  /**
   * Returns a String representation of a {@link StreamContentId which can be used
   * in Log messages.
   */
  public String printContentId(@Nullable StreamContentId contentId) {
    if (contentId == null) {
      return "NULL ContentID";
    }
    return String.format(
        "ContentId - Domain: %s, id: %s, table: %s",
        contentId.getContentDomain(), contentId.getId(), contentId.getTable());
  }
}
