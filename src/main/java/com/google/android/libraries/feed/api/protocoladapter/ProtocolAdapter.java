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

package com.google.android.libraries.feed.api.protocoladapter;

import com.google.protobuf.ByteString;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.ResponseProto.Response;

/**
 * The Protocol Adapter will convert the wire protocol into the internal protocol used in Jardin.
 */
public interface ProtocolAdapter {
  /**
   * Create the internal protocol from a wire protocol response definition. The {@code token} {@link
   * ByteString} represents the continuation token used to create the response.
   */
  void createModel(Response response, ByteString token);

  /** Convert a wire protocol ContentId into the {@code String} version used in Jardin. */
  String getStreamContentId(ContentId contentId);
}
