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

package com.google.android.libraries.feed.api.scope;

import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.actionhandler.FeedActionHandler;
import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.UiRunnableHandlerImpl;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests for {@link FeedStreamScope}. */
@RunWith(RobolectricTestRunner.class)
public class FeedStreamScopeTest {

  // Mocks for required fields
  @Mock private ActionApi actionApi;
  @Mock private ImageLoaderApi imageLoaderApi;
  @Mock private LoggingApi loggingApi;

  // Mocks for optional fields
  @Mock private FeedProtocolAdapter protocolAdapter;
  @Mock private SessionManager sessionManager;
  @Mock private FeedActionHandler feedActionHandler;
  @Mock private Stream stream;

  private UiRunnableHandler uiRunnableHandler;
  private ThreadUtils threadUtils;
  private InternalProtocolUtils internalProtocolUtils;

  @Before
  public void setUp() {
    initMocks(this);
    uiRunnableHandler = new UiRunnableHandlerImpl();
    threadUtils = new ThreadUtils();
    internalProtocolUtils = new InternalProtocolUtils();
  }

  @Test
  public void testBasicBuild() {
    new FeedStreamScope.Builder(
            actionApi, imageLoaderApi, loggingApi, protocolAdapter, sessionManager)
        .build()
        .getStream();

    // No crash should happen.
  }

  @Test
  public void testComplexBuild() {
    new FeedStreamScope.Builder(
            actionApi, imageLoaderApi, loggingApi, protocolAdapter, sessionManager)
        .setFeedActionHandler(feedActionHandler)
        .setStream(stream)
        .setThreadUtils(threadUtils)
        .setInternalProtocolUtils(internalProtocolUtils)
        .setUiRunnableHandler(uiRunnableHandler)
        .build()
        .getStream();

    // No crash should happen.
  }
}
