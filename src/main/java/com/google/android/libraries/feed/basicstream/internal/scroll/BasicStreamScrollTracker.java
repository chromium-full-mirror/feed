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

package com.google.android.libraries.feed.basicstream.internal.scroll;

import com.google.android.libraries.feed.api.stream.ScrollListener;
import com.google.android.libraries.feed.api.stream.ScrollListener.ScrollState;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.host.logging.ScrollType;
import com.google.android.libraries.feed.sharedstream.scroll.ScrollLogger;
import com.google.android.libraries.feed.sharedstream.scroll.ScrollTracker;
import com.google.android.libraries.feed.sharedstream.scroll.StreamScrollMonitor;

/** A @link{ScrollTracker} used by BasicStream */
public class BasicStreamScrollTracker extends ScrollTracker {
  private final ScrollLogger scrollLogger;
  private final ScrollListener scrollListener;
  private final StreamScrollMonitor scrollMonitor;

  public BasicStreamScrollTracker(
      MainThreadRunner mainThreadRunner,
      ScrollLogger scrollLogger,
      Clock clock,
      StreamScrollMonitor scrollMonitor) {
    super(mainThreadRunner, clock);
    this.scrollLogger = scrollLogger;
    this.scrollMonitor = scrollMonitor;
    this.scrollListener = new BasicStreamScrollListener();
    scrollMonitor.addScrollListener(scrollListener);
  }

  @Override
  protected void onScrollEvent(int scrollAmount, long timestamp) {
    scrollLogger.handleScroll(ScrollType.STREAM_SCROLL, scrollAmount);
  }

  @Override
  public void onUnbind() {
    super.onUnbind();
    scrollMonitor.removeScrollListener(scrollListener);
  }

  private class BasicStreamScrollListener implements ScrollListener {

    @Override
    public void onScrollStateChanged(@ScrollState int state) {}

    @Override
    public void onScrolled(int dx, int dy) {
      trackScroll(dx, dy);
    }
  }
}
