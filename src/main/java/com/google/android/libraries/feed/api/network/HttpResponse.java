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

/** Representation of an HTTP response. */
public final class HttpResponse {
  private int responseCode;
  private byte[] responseBody;

  private HttpResponse() {}

  public int getResponseCode() {
    return responseCode;
  }

  public byte[] getResponseBody() {
    return responseBody;
  }

  /** Builds a {@link HttpResponse}. */
  public static class Builder {
    private final HttpResponse result = new HttpResponse();

    public Builder setResponseCode(int responseCode) {
      result.responseCode = responseCode;
      return this;
    }

    public Builder setResponseBody(byte[] responseBody) {
      result.responseBody = responseBody;
      return this;
    }

    public HttpResponse build() {
      return result;
    }
  }
}
