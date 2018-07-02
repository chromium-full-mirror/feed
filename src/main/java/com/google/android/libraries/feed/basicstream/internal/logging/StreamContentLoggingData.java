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


/** Stand-in class to capture content data when logging events. */
public class StreamContentLoggingData {

  private final int positionInStream;

  public StreamContentLoggingData(int positionInStream) {
    this.positionInStream = positionInStream;
  }

  public int getPositionInStream() {
    return positionInStream;
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
    return positionInStream == that.positionInStream;
  }

  @Override
  public int hashCode() {
    return positionInStream;
  }
}
