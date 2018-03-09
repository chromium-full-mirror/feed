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

import static com.google.common.truth.Truth.assertThat;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests of the {@link InternalProtocolUtils} class. */
@RunWith(RobolectricTestRunner.class)
public class InternalProtocolUtilsTest {

  @Test
  public void testGetContentIdKey() {
    StreamContentId streamContentId = getStreamContentId();
    InternalProtocolUtils internalProtocolUtils = new InternalProtocolUtils();
    String key = internalProtocolUtils.getContentIdKey(streamContentId);
    assertStringContents(key, streamContentId);
  }

  @Test
  public void testCreateContentId() {
    ContentId contentId =
        ContentId.newBuilder().setContentDomain("content-id").setId(13).setTable("table").build();
    InternalProtocolUtils internalProtocolUtils = new InternalProtocolUtils();
    StreamContentId streamContentId = internalProtocolUtils.createContentId(contentId);
    assertThat(streamContentId.getContentDomain()).isEqualTo(contentId.getContentDomain());
    assertThat(streamContentId.getId()).isEqualTo(contentId.getId());
    assertThat(streamContentId.getTable()).isEqualTo(contentId.getTable());
  }

  @Test
  public void testPrintContentIdKey() {
    StreamContentId streamContentId = getStreamContentId();
    InternalProtocolUtils internalProtocolUtils = new InternalProtocolUtils();
    String key = internalProtocolUtils.printContentId(streamContentId);
    assertStringContents(key, streamContentId);
  }

  private StreamContentId getStreamContentId() {
    return StreamContentId.newBuilder()
        .setContentDomain("content-domain")
        .setId(17)
        .setTable("table")
        .build();
  }

  private void assertStringContents(String value, StreamContentId streamContentId) {
    assertThat(value.contains(streamContentId.getContentDomain())).isTrue();
    assertThat(value.contains(streamContentId.getTable())).isTrue();
    assertThat(value.contains(Long.toString(streamContentId.getId()))).isTrue();
  }
}
