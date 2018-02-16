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

package com.google.android.libraries.feed.api.modelprovider;

import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class defining the specific changes made to feature nodes within the model. This is passed to the
 * observer to handle model changes.
 *
 * <p>TODO: This is very much still prototype and needs to be designed
 */
public class StreamFeatureChange {
  private final StreamContentId streamContentId;
  private final StreamFeature streamFeature;
  private final ChildChange childChange;

  public StreamFeatureChange(
      StreamContentId contentId, StreamFeature streamFeature, ChildChange childChange) {
    this.streamContentId = contentId;
    this.streamFeature = streamFeature;
    this.childChange = childChange;
  }

  public StreamContentId getContentId() {
    return streamContentId;
  }

  /** Returns the updated {@link StreamFeature} */
  @Nullable
  public StreamFeature getUpdatedStreamFeature() {
    return streamFeature;
  }

  /**
   * Returns a list of new children, order is important, these will be appended to to end of the
   * children for this container
   */
  public List<ModelChild> getNewChildren() {
    return childChange.appendChildren;
  }

  /** Returns a list of children which should be removed */
  public List<ModelChild> getRemovedChildren() {
    return childChange.removeChildren;
  }

  /** Structure used to define the children changes. */
  public static class ChildChange {
    private List<ModelChild> appendChildren;
    private List<ModelChild> removeChildren;

    ChildChange(
        @Nullable List<ModelChild> appendChildren, @Nullable List<ModelChild> removeChildren) {
      this.appendChildren = (appendChildren != null) ? appendChildren : new ArrayList<>();
      this.removeChildren = (removeChildren != null) ? removeChildren : new ArrayList<>();
    }
  }
}
