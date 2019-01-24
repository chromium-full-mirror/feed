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

package com.google.android.libraries.feed.common.time.testing;

import com.google.android.libraries.feed.common.time.Clock;
import java.util.HashSet;
import java.util.Set;

/** Fake implementation of {@link Clock} for testing. */
public class FakeClock implements Clock {
  private long currentTime;
  private long elapsedRealtimeMs;
  private final Set<ClockUpdateListener> listeners;

  public FakeClock() {
    this(0, 0);
  }

  public FakeClock(long currentTime, long elapsedRealtime) {
    this.currentTime = currentTime;
    elapsedRealtimeMs = elapsedRealtime;
    this.listeners = new HashSet<>();
  }

  /**
   * Sets the current time and elapsed real time to the same value. Returns {@code this} for
   * convenience.
   */
  public FakeClock set(long millis) {
    return set(millis, millis);
  }

  /**
   * Sets the current time and elapsed real time individually. Returns {@code this} for convenience.
   */
  public FakeClock set(long currentTime, long elapsedRealtime) {
    this.currentTime = currentTime;
    elapsedRealtimeMs = elapsedRealtime;
    onClockUpdated(currentTime, elapsedRealtime);
    return this;
  }

  public void tick() {
    advance(1);
  }

  public void advance(long millis) {
    currentTime += millis;
    elapsedRealtimeMs += millis;
    onClockUpdated(currentTime, elapsedRealtimeMs);
  }

  @Override
  public long currentTimeMillis() {
    return currentTime;
  }

  @Override
  public long elapsedRealtime() {
    return elapsedRealtimeMs;
  }

  @Override
  public long uptimeMillis() {
    return elapsedRealtimeMs;
  }

  private void onClockUpdated(long currentTime, long elapsedRealtime) {
    for (ClockUpdateListener listener : listeners) {
      listener.onClockUpdated(currentTime, elapsedRealtime);
    }
  }

  public void addClockUpdateListener(ClockUpdateListener listener) {
    listeners.add(listener);
  }

  /** Listener for when the clock time is updated. */
  public interface ClockUpdateListener {
    void onClockUpdated(long newCurrentTime, long newElapsedRealtime);
  }
}
