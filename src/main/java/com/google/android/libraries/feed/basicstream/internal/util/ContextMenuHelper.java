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

package com.google.android.libraries.feed.basicstream.internal.util;

import android.view.View;

/** Provides information to allow for the construction of Context Menus. */
public class ContextMenuHelper {

  private final View view;

  public ContextMenuHelper(View view) {
    this.view = view;
  }

  /**
   * Gets the height of the Stream. This is specifically the height visible on screen, not including
   * anything below the screen.
   */
  public int getStreamHeight() {
    return view.getHeight();
  }

  /** Gets the width of the Stream. */
  public int getStreamWidth() {
    return view.getWidth();
  }

  /** Gets the Y coordinate of the position of the Stream. */
  public int getStreamYPosition() {
    int[] position = new int[2];
    view.getLocationInWindow(position);
    return position[1];
  }
}
