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

package com.google.android.libraries.feed.api.stream;

import android.os.Bundle;
import android.view.View;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import java.util.List;
import javax.annotation.Nullable;

/** Interface used for interacting with the Stream library in order to render a stream of cards. */
public interface Stream {
  /**
   * Call during {@link android.app.Activity#onCreate(Bundle)}. The {@code savedInstanceState}
   * should be the same bundle retrieved from {@link #getSavedInstanceState()} while the activity
   * was saving its state.
   *
   * @param savedInstanceState state to restore to.
   */
  void onCreate(@Nullable Bundle savedInstanceState);

  /** Call during the {@link android.app.Activity#onStart()}. */
  void onStart();

  /** Call during the {@link android.app.Activity#onStop()}. */
  void onStop();

  /** Call during the {@link android.app.Activity#onDestroy()}. */
  void onDestroy();

  /**
   * Call during {@link android.app.Activity#onSaveInstanceState(Bundle)}. The returned bundle
   * should be passed to {@link #onCreate(Bundle)} when the activity is recreated and the stream
   * needs to be recreated.
   */
  Bundle getSavedInstanceState();

  /** Return the root view which holds all card stream views. */
  View getView();

  /**
   * Set headers on the stream. Headers will be added to the top of the stream in the order they are
   * in the list. The headers are not sticky and will scroll with content. Headers can be cleared by
   * passing in an empty list.
   */
  void setHeaders(List<View> headers);

  /** Sets the data this Stream should display based on the information in {@link ModelProvider}. */
  void setModelProvider(ModelProvider modelProvider);
}
