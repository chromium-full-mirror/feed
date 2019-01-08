// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.piet;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.view.View;
import com.google.android.libraries.feed.common.logging.Logger;

/**
 * Controls the unbinding of actions, and saves the information needed to do that.
 *
 * <p>Actions that trigger an animation on a {@link Drawable} use a callback. Currently every click
 * actions triggers a ripple through the {@link android.graphics.drawable.RippleDrawable} by
 * default.
 *
 * <p>The callback needs to be saved here to ensure that the callback, which Drawable holds in a
 * weak reference, is not garbage collected too soon. The callback is released when unbind() is
 * called in this class, at which point it should be safe to garbage collect.
 */
public class ActionBinding {
  private final View view;
  private static final String TAG = "ActionBinding";

  /*@Nullable*/
  @SuppressWarnings("unused") // Callback is saved so it won't be garbage collected prematurely.
  private Callback callback;

  private boolean isBound = true;

  public ActionBinding(View view, /*@Nullable*/ Callback callback) {
    this.view = view;
    this.callback = callback;
  }

  /**
   * Tears down actions on the view, including clearing click actions and setting the foreground
   * Drawable and Drawable.Callback to null so that they can be garbage collected.
   *
   * <p>Currently, action binding actually happens in ElementAdapter.bindActions(), which then calls
   * ViewUtils.setOnClickActions(). TODO: Move logic to bind actions into this class.
   */
  public void unbind() {
    if (!isBound) {
      Logger.wtf(TAG, "Trying to unbind an ActionBinding that is not currently bound.");
    }
    ViewUtils.clearOnClickActions(view);
    ViewUtils.clearOnLongClickActions(view);
    view.setForeground(null);
    callback = null;
    isBound = false;
  }
}
