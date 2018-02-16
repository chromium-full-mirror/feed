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

package com.google.android.libraries.feed.host.scheduler;

/** An API to report feed state information to the scheduler. */
public interface SchedulerApi {
  /**
   * Notifies the scheduler that there is no data to render.
   *
   * @return true if the scheduler will schedule a request for more data
   */
  boolean notifyNoData();
}
