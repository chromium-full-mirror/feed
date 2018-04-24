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

package com.google.android.libraries.feed.testing.modelprovider;

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.common.functional.Consumer;
import java.util.List;

/** A fake {@link ModelCursor} for testing. */
public class FakeModelCursor implements ModelCursor {

  private List<ModelChild> modelChildren;
  private int currentIndex = 0;

  public void setModelChildren(List<ModelChild> modelChildren) {
    currentIndex = 0;
    this.modelChildren = modelChildren;
  }

  @Override
  public boolean advanceCursor(Consumer<ModelChild> processor) {
    if (isAtEnd()) {
      return false;
    }
    processor.accept(modelChildren.get(currentIndex++));
    return !isAtEnd();
  }

  @Override
  /*@Nullable*/
  public ModelChild getNextItem() {
    if (isAtEnd()) {
      return null;
    }
    return modelChildren.get(currentIndex++);
  }

  @Override
  public boolean isAtEnd() {
    return currentIndex >= modelChildren.size();
  }
}
