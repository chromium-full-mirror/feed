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

package com.google.android.libraries.feed.common.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;

/** Helper class providing validation method for a ModelProvider. */
public class ModelProviderValidator {
  private final ProtocolAdapter protocolAdapter;
  private final InternalProtocolUtils protocolUtils = new InternalProtocolUtils();

  public ModelProviderValidator(ProtocolAdapter protocolAdapter) {
    this.protocolAdapter = protocolAdapter;
  }

  public void assertRoot(ModelProvider modelProvider) {
    ModelFeature modelFeature = modelProvider.getRootFeature();
    assertThat(modelFeature).isNotNull();
    assertStreamContentId(
        modelFeature.getStreamFeature().getContentId(),
        protocolAdapter.getStreamContentId(WireProtocolResponseBuilder.ROOT_CONTENT_ID));
  }

  public void assertStreamContentId(StreamContentId contentId, StreamContentId expectedContentId) {
    assertThat(protocolUtils.getContentIdKey(contentId))
        .isEqualTo(protocolUtils.getContentIdKey(expectedContentId));
  }
}
