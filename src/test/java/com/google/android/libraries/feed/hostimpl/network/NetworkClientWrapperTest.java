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
package com.google.android.libraries.feed.hostimpl.network;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link NetworkClientWrapper}. */
@RunWith(RobolectricTestRunner.class)
public class NetworkClientWrapperTest {
  @Mock private ThreadUtils threadUtils;
  @Mock private NetworkClient networkClient;
  @Mock private HttpRequest request;
  @Mock private Consumer<HttpResponse> responseConsumer;

  @Spy private MainThreadRunner mainThreadRunner = new FakeMainThreadRunner();

  @Before
  public void setup() {
    initMocks(this);
  }

  @After
  public void validate() {
    validateMockitoUsage();
  }

  @Test
  public void testSend_mainThread() {
    when(threadUtils.isMainThread()).thenReturn(true);
    NetworkClientWrapper wrapper =
        new NetworkClientWrapper(networkClient, threadUtils, mainThreadRunner);
    wrapper.send(request, responseConsumer);
    verify(networkClient).send(request, responseConsumer);
    verify(mainThreadRunner, never()).execute(anyString(), any());
  }

  @Test
  public void testSend_backgroundThread() {
    when(threadUtils.isMainThread()).thenReturn(false);
    NetworkClientWrapper wrapper =
        new NetworkClientWrapper(networkClient, threadUtils, mainThreadRunner);
    wrapper.send(request, responseConsumer);
    verify(networkClient).send(request, responseConsumer);
    verify(mainThreadRunner).execute(anyString(), any(Runnable.class));
  }

  private static class FakeMainThreadRunner extends MainThreadRunner {
    @Override
    public void execute(String name, Runnable runnable) {
      runnable.run();
    }
  }
}
