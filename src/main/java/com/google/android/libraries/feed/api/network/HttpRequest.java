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

package com.google.android.libraries.feed.api.network;

import android.net.Uri;

/** Representation of an HTTP request. */
public class HttpRequest {
  private Uri uri;
  private boolean withAuthToken;
  private byte[] body;

  private HttpRequest() {}

  public boolean getWithAuthToken() {
    return withAuthToken;
  }

  public Uri getUri() {
    return uri;
  }

  public byte[] getBody() {
    return body;
  }

  /** Builds a {@link HttpRequest}. */
  public static class Builder {
    private final HttpRequest request = new HttpRequest();

    public Builder withAuthToken() {
      request.withAuthToken = true;
      return this;
    }

    public Builder setBody(byte[] body) {
      request.body = body;
      return this;
    }

    public Builder setUri(Uri uri) {
      request.uri = uri;
      return this;
    }

    public HttpRequest build() {
      return request;
    }
  }
}
