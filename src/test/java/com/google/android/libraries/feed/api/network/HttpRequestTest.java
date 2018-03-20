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

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import com.google.android.libraries.feed.api.network.HttpRequest.HttpMethod;
import org.robolectric.RobolectricTestRunner;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test class for {@link HttpRequest} */
@RunWith(RobolectricTestRunner.class)
public class HttpRequestTest {

  @Test
  public void testConstructor() throws Exception {
    Uri uri = Uri.EMPTY;
    ByteBuffer byteBuffer = ByteBuffer.allocate(0);
    String httpMethod = HttpMethod.GET;
    HttpRequest httpRequest = new HttpRequest(uri, byteBuffer, httpMethod);

    assertThat(httpRequest.getUri()).isEqualTo(uri);
    assertThat(httpRequest.getBody()).isEqualTo(byteBuffer);
    assertThat(httpRequest.getMethod()).isEqualTo(httpMethod);
  }
}
