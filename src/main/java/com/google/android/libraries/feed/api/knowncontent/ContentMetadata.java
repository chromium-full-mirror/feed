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

package com.google.android.libraries.feed.api.knowncontent;


/** Metadata for content. */
public class ContentMetadata {

  private final String url;
  private final String title;
  /*@Nullable*/ private final String imageUrl;
  /*@Nullable*/ private final String attribution;
  /*@Nullable*/ private final String faviconUrl;
  /*@Nullable*/ private final String snippet;

  public ContentMetadata(
      String url,
      String title,
      /*@Nullable*/ String imageUrl,
      /*@Nullable*/ String attribution,
      /*@Nullable*/ String faviconUrl,
      /*@Nullable*/ String snippet) {
    this.url = url;
    this.title = title;
    this.imageUrl = imageUrl;
    this.attribution = attribution;
    this.faviconUrl = faviconUrl;
    this.snippet = snippet;
  }

  public String getUrl() {
    return url;
  }

  /** Title for the content. */
  public String getTitle() {
    return title;
  }

  /*@Nullable*/
  public String getImageUrl() {
    return imageUrl;
  }

  /**
   * {@link String} representation of the source. Typically includes source and time since
   * publishing.
   */
  /*@Nullable*/
  public String getAttribution() {
    return attribution;
  }

  /*@Nullable*/
  public String getFaviconUrl() {
    return faviconUrl;
  }

  /** A {@link String} that can be displayed that is part of the content, typically the start. */
  /*@Nullable*/
  public String getSnippet() {
    return snippet;
  }
}
