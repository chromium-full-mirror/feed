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

package com.google.android.libraries.feed.piet.host;

import com.google.search.now.ui.piet.ActionsProto.Action;
import com.google.search.now.ui.piet.PietProto.Frame;
import javax.annotation.Nullable;

/**
 * Interface from the Piet host which provides handling of {@link Action} from the UI. An instance
 * of this is provided by the host. When an action is raised by the client {@link
 * #handleAction(Action, Frame, String)} will be called.
 */
public interface ActionHandler {
  /** Called on a touch event on a UI element */
  // TODO: Do we need to pass back the thing that was pressed?
  // TODO: Do we need the veLoggingToken, one is defined in the Action
  // TODO: Handle long press actions.
  void handleAction(Action action, Frame frame, @Nullable String veLoggingToken);
}
