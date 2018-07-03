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

package com.google.android.libraries.feed.basicstream.internal.logging;

import com.google.search.now.ui.stream.StreamStructureProto.BasicLoggingMetadata;

/** Stand-in class to capture content data when logging events. */
public class StreamContentLoggingData {

  private final int positionInStream;
  private final int publishedTimeSeconds;
  private final int timeContentBecameAvailable;
  private final int score;

  public StreamContentLoggingData(int positionInStream, BasicLoggingMetadata basicLoggingMetadata) {
    this.positionInStream = positionInStream;
    this.publishedTimeSeconds = basicLoggingMetadata.getPublishedTimeSeconds();
    this.timeContentBecameAvailable = basicLoggingMetadata.getAvailabilityTimeSeconds();
    this.score = basicLoggingMetadata.getScore();
  }

  public int getPositionInStream() {
    return positionInStream;
  }

  public int getPublishedTimeSeconds() {
    return publishedTimeSeconds;
  }

  public int getTimeContentBecameAvailable() {
    return timeContentBecameAvailable;
  }

  public int getScore() {
    return score;
  }

  @Override
  public boolean equals(/*@Nullable*/ Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StreamContentLoggingData that = (StreamContentLoggingData) o;

    if (positionInStream != that.positionInStream) {
      return false;
    }
    if (publishedTimeSeconds != that.publishedTimeSeconds) {
      return false;
    }
    if (timeContentBecameAvailable != that.timeContentBecameAvailable) {
      return false;
    }
    return score == that.score;
  }

  @Override
  public int hashCode() {
    int result = positionInStream;
    result = 31 * result + publishedTimeSeconds;
    result = 31 * result + timeContentBecameAvailable;
    result = 31 * result + score;
    return result;
  }

  @Override
  public String toString() {
    return "StreamContentLoggingData{"
        + "positionInStream="
        + positionInStream
        + ", publishedTimeSeconds="
        + publishedTimeSeconds
        + ", timeContentBecameAvailable="
        + timeContentBecameAvailable
        + ", score="
        + score
        + '}';
  }
}
