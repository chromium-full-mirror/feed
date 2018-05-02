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
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;

/** Fake for {@link com.google.android.libraries.feed.api.modelprovider.ModelChild}. */
public class FakeModelChild implements ModelChild {

  private final @Type int type;
  private final String contentId;
  private final ModelFeature modelFeature;
  private final ModelToken modelToken;

  private final /*@Nullable*/ String parentId;

  private FakeModelChild(
      @Type int type,
      String contentId,
      ModelFeature modelFeature,
      ModelToken modelToken,
      /*@Nullable*/ String parentId) {
    this.type = type;
    this.contentId = contentId;
    this.modelFeature = modelFeature;
    this.modelToken = modelToken;
    this.parentId = parentId;
  }

  @Override
  public @Type int getType() {
    return type;
  }

  @Override
  public String getContentId() {
    return contentId;
  }

  @Override
  public boolean hasParentId() {
    return parentId != null;
  }

  @Override
  public /*@Nullable*/ String getParentId() {
    return parentId;
  }

  @Override
  public ModelFeature getModelFeature() {
    return modelFeature;
  }

  @Override
  public ModelToken getModelToken() {
    return modelToken;
  }

  public static class Builder {
    private @Type int type = Type.UNBOUND;
    private String contentId = "";
    private ModelFeature modelFeature = new FakeModelFeature.Builder().build();
    private ModelToken modelToken = new FakeModelToken.Builder().build();
    private /*@Nullable*/ String parentId;

    public Builder setType(@Type int type) {
      this.type = type;
      return this;
    }

    public Builder setContentId(String contentId) {
      this.contentId = contentId;
      return this;
    }

    public Builder setParentId(/*@Nullable*/ String parentId) {
      this.parentId = parentId;
      return this;
    }

    public Builder setModelFeature(ModelFeature modelFeature) {
      this.modelFeature = modelFeature;
      return this;
    }

    public Builder setModelToken(ModelToken modelToken) {
      this.modelToken = modelToken;
      return this;
    }

    public FakeModelChild build() {
      return new FakeModelChild(type, contentId, modelFeature, modelToken, parentId);
    }
  }
}
