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

import static com.google.android.libraries.feed.piet.StyleProvider.DEFAULT_STYLE;

import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.piet.FrameContext.ErrorType;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.search.now.ui.piet.PietProto.Frame;
import com.google.search.now.ui.piet.StylesProto.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets a {@link Frame}'s style information to create a {@link FrameContext}. This class will
 * provide the binding of {@code StyleProvider} through the {@link FrameContext}. It also defines
 * the default style provider which acts as the root style. {@see DEFAULT_STYLE_PROVIDER)} The
 * {@link FrameContext} contains all of the style information to theme a card, as well as the host's
 * {@link AssetProvider} to retrieve images and other host-specific resources.
 */
class FrameModelBinder {

  private static final String TAG = "FrameModelBinder";

  private final PietStylesHelper stylesHelper;
  private final DebugBehavior debugBehavior;
  private final AssetProvider assetProvider;
  private final CustomElementProvider customElementProvider;

  FrameModelBinder(
      PietStylesHelper stylesHelper,
      DebugBehavior debugBehavior,
      AssetProvider assetProvider,
      CustomElementProvider customElementProvider) {
    this.stylesHelper = stylesHelper;
    this.debugBehavior = debugBehavior;
    this.assetProvider = assetProvider;
    this.customElementProvider = customElementProvider;
  }

  FrameContext bindFrame(Frame frame) {
    Map<String, Style> stylesheetMap = null;
    List<String> errors = new ArrayList<>(3);
    if (frame.hasStylesheetId()) {
      stylesheetMap = stylesHelper.getStylesheet(frame.getStylesheetId());
      if (stylesheetMap.isEmpty()) {
        String error =
            String.format("Stylesheet [%s] not found, no stylesheet used", frame.getStylesheetId());
        errors.add(error);
        Logger.w(TAG, "[%s] E - %s", frame.getTag(), error);
      }
    } else if (frame.hasStylesheet()) {
      stylesheetMap = PietStylesHelper.createMapFromStylesheet(frame.getStylesheet());
    } else {
      String error = "Frame defined without any Stylesheet";
      errors.add(error);
      Logger.w(TAG, "[%s] W - %s", frame.getTag(), error);
    }

    Style frameStyle = null;
    if (frame.hasStyleReferences()) {
      if (stylesheetMap != null) {
        frameStyle =
            PietStylesHelper.mergeStyleIdsStack(
                DEFAULT_STYLE, frame.getStyleReferences(), stylesheetMap, null);
        if (frameStyle == null) {
          String error =
              String.format(
                  "Couldn't find frame styles [%s] in the Stylesheet", frame.getStyleReferences());
          errors.add(error);
          Logger.w(TAG, "[%s] E - %s", frame.getTag(), error);
        }
      }
    }
    StyleProvider styleProvider =
        (frameStyle != null) ? new StyleProvider(frameStyle) : StyleProvider.DEFAULT_STYLE_PROVIDER;
    if (stylesheetMap == null) {
      stylesheetMap = new HashMap<>();
    }
    FrameContext frameContext =
        new FrameContext(
            frame,
            stylesheetMap,
            DEFAULT_STYLE,
            styleProvider,
            stylesHelper,
            debugBehavior,
            assetProvider,
            customElementProvider);
    for (String error : errors) {
      frameContext.reportError(ErrorType.ERROR, error);
    }
    return frameContext;
  }

}
