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

package com.google.android.libraries.feed.piet;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.ViewGroup;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.util.Producer;

/**
 * A state shared by instances of Cards and Slices. The state is accessed directly from the instance
 * instead of going through getX methods.
 *
 * <p>TODO: This is basically the Dagger state for Piet. Need to implement dagger support.
 */
public class AdapterParameters {
  final ParameterizedTextEvaluator templatedStringEvaluator;
  final Producer<ViewGroup> parentViewProducer;
  final ActionHandler actionHandler;

  final Context context;

  final ElementAdapterFactory elementAdapterFactory;

  @SuppressWarnings("nullness:assignment.type.incompatible")
  public AdapterParameters(
      Context context, Producer<ViewGroup> parentViewProducer, ActionHandler actionHandler) {
    this.context = context;
    this.parentViewProducer = parentViewProducer;
    this.actionHandler = actionHandler;

    templatedStringEvaluator = new ParameterizedTextEvaluator();
    elementAdapterFactory = new ElementAdapterFactory(context, this);
  }

  /** Testing-only constructor for mocking the internally-constructed objects. */
  @VisibleForTesting
  AdapterParameters(
      Context context,
      Producer<ViewGroup> parentViewProducer,
      ActionHandler actionHandler,
      ParameterizedTextEvaluator templatedStringEvaluator,
      ElementAdapterFactory elementAdapterFactory) {
    this.context = context;
    this.parentViewProducer = parentViewProducer;
    this.actionHandler = actionHandler;

    this.templatedStringEvaluator = templatedStringEvaluator;
    this.elementAdapterFactory = elementAdapterFactory;
  }
}
