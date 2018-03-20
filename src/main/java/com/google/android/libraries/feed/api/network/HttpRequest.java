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
import android.support.annotation.StringDef;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;

/** Representation of an HTTP request. */
public class HttpRequest {

  /**
   * These string values line up with HTTP method values, see {@link
   * HttpURLConnection#setRequestMethod(String)}.
   */
  @StringDef({
    HttpMethod.GET,
    HttpMethod.POST,
    HttpMethod.PUT,
    HttpMethod.DELETE,
  })
  public @interface HttpMethod {
    String GET = "GET";
    String POST = "POST";
    String PUT = "PUT";
    String DELETE = "DELETE";
  }

  private final Uri uri;
  private final ByteBuffer body;
  private final @HttpMethod String method;

  public HttpRequest(Uri uri, ByteBuffer body, String method) {
    this.uri = uri;
    this.body = body;
    this.method = method;
  }

  public Uri getUri() {
    return uri;
  }

  public ByteBuffer getBody() {
    return body;
  }

  public @HttpMethod String getMethod() {
    return method;
  }
}
