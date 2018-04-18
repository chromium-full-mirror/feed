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

import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.basicstream.internal.contentmodels.ContentModel;
import java.util.List;

/** Used to communicate the handling of a continuation token in the {@link BasicStream}. */
public interface TokenHandler {

  // TODO: Replace with a method indicating the BasicStream should re-query for
  // new contents instead of directly adding them.
  /** Adds the given {@link List} of {@link ContentModel} to the adapter. */
  void addContentModels(List<ContentModel> newContentModels);

  /** Instructs the {@link BasicStream} to handle the token. */
  void handleToken(ModelToken token);
}
