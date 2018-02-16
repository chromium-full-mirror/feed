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

package com.google.android.libraries.feed.host.common;

/** Provides support for processing asynchronous responses. */
public interface Callback<T> {
  /** Handles a result. */
  // TODO: We need to handle errors, probably should have a Result
  // TODO: We need some ability to check if the callback has completed
  void handleResult(T value);
}
