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

package com.google.android.libraries.feed.feedknowncontent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.libraries.feed.api.knowncontent.ContentRemoval;
import com.google.android.libraries.feed.api.knowncontent.KnownContentApi.KnownContentListener;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FeedKnownContentApi}. */
@RunWith(RobolectricTestRunner.class)
public class FeedKnownContentApiTest {

  private static final long CONTENT_CREATION_DATE_TIME_MS = 123L;
  private static final List<ContentRemoval> CONTENT_REMOVED =
      Collections.singletonList(new ContentRemoval("url", /* requestedByUser= */ false));

  @Mock private SessionManager sessionManager;
  @Mock private KnownContentListener listener1;
  @Mock private KnownContentListener listener2;

  private FeedKnownContentApi knownContentApi;

  @Before
  public void setUp() {
    initMocks(this);
    knownContentApi = new FeedKnownContentApi(sessionManager);
  }

  @Test
  public void testSetsListenerOnSessionManager() {
    verify(sessionManager).setKnownContentListener(knownContentApi.getKnownContentHostNotifier());
  }

  @Test
  public void testNotifyListeners_contentReceived() {
    knownContentApi.addListener(listener1);
    knownContentApi.addListener(listener2);

    knownContentApi
        .getKnownContentHostNotifier()
        .onNewContentReceived(/* isNewRefresh= */ false, CONTENT_CREATION_DATE_TIME_MS);

    verify(listener1)
        .onNewContentReceived(/* isNewRefresh= */ false, CONTENT_CREATION_DATE_TIME_MS);
    verify(listener2)
        .onNewContentReceived(/* isNewRefresh= */ false, CONTENT_CREATION_DATE_TIME_MS);
  }

  @Test
  public void testNotifyListeners_contentRemoved() {
    knownContentApi.addListener(listener1);
    knownContentApi.addListener(listener2);

    knownContentApi.getKnownContentHostNotifier().onContentRemoved(CONTENT_REMOVED);

    verify(listener1).onContentRemoved(CONTENT_REMOVED);
    verify(listener2).onContentRemoved(CONTENT_REMOVED);
  }

  @Test
  public void testRemoveListener() {
    knownContentApi.addListener(listener1);
    knownContentApi.removeListener(listener1);

    knownContentApi.getKnownContentHostNotifier().onContentRemoved(CONTENT_REMOVED);
    knownContentApi
        .getKnownContentHostNotifier()
        .onNewContentReceived(/* isNewRefresh= */ true, CONTENT_CREATION_DATE_TIME_MS);

    verifyNoMoreInteractions(listener1);
  }
}
