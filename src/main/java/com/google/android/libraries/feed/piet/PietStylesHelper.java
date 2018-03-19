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

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.piet.util.Producer;
import com.google.search.now.ui.piet.PietProto.PietSharedState;
import com.google.search.now.ui.piet.PietProto.Stylesheet;
import com.google.search.now.ui.piet.PietProto.Template;
import com.google.search.now.ui.piet.StylesProto.BoundStyle;
import com.google.search.now.ui.piet.StylesProto.Style;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** This class provides support helpers methods for managing styles in Piet. */
public class PietStylesHelper {
  private static final String TAG = "PietStylesHelper";

  private Producer<PietSharedState> pietSharedStateSupplier;
  @Nullable private PietSharedState currentPietSharedState;

  private final Map<String, Map<String, Style>> stylesheetScopes = new HashMap<>();
  private final Map<String, Stylesheet> stylesheets = new HashMap<>();
  private final Map<String, Template> templates = new HashMap<>();

  public PietStylesHelper(Producer<PietSharedState> pietSharedStateSupplier) {
    this.pietSharedStateSupplier = pietSharedStateSupplier;
  }

  /** Returns a Map of style_id to Style. This represents the Stylesheet. */
  @Nullable
  Map<String, Style> getStylesheet(String stylesheetId) {
    checkCurrent();
    if (stylesheetScopes.containsKey(stylesheetId)) {
      return stylesheetScopes.get(stylesheetId);
    }
    Stylesheet stylesheet = stylesheets.get(stylesheetId);
    if (stylesheet != null) {
      Map<String, Style> stylesheetMap = createMapFromStylesheet(stylesheet);
      stylesheetScopes.put(stylesheet.getStylesheetId(), stylesheetMap);
      return stylesheetMap;
    }
    Logger.w(TAG, "Stylesheet [%s] was not found in the Stylesheet", stylesheetId);
    return null;
  }

  static Map<String, Style> createMapFromStylesheet(Stylesheet stylesheet) {
    Map<String, Style> stylesheetMap = new HashMap<>();
    for (Style style : stylesheet.getStylesList()) {
      stylesheetMap.put(style.getStyleId(), style);
    }
    return stylesheetMap;
  }

  /** Returns a {@link Template} for the template */
  @Nullable
  public Template getTemplate(String templateId) {
    checkCurrent();
    return templates.get(templateId);
  }

  @Nullable
  public PietSharedState getCurrentPietSharedState() {
    return currentPietSharedState;
  }

  /**
   * Given a StyleIdsStack, a base style, and a styleMap that contains the styles definition,
   * returns a Style that is the proto-merge of all the styles in the stack, starting with the base.
   *
   * <p>TODO: This is only nullable to handle an error case. Maybe once error-handling
   * is standardized we could create some reasonable fallback behavior and get rid of @Nullable.
   */
  @Nullable
  static Style mergeStyleIdsStack(
      Style baseStyle,
      StyleIdsStack stack,
      Map<String, Style> styleMap,
      @Nullable FrameContext frameContext,
      boolean suppressErrors) {
    Style.Builder mergedStyle = baseStyle.toBuilder();
    for (String style : stack.getStyleIdsList()) {
      if (styleMap.containsKey(style)) {
        mergedStyle.mergeFrom(styleMap.get(style)).build();
      } else {
        if (!suppressErrors) {
          Logger.e(TAG, "Unable to bind style [%s], style not found in Stylesheet", style);
          return null;
        }
      }
    }
    if (stack.hasStyleBinding()) {
      checkNotNull(frameContext, "Binding styles not supported when frameContext is null");
      BoundStyle boundStyle = frameContext.getStyleFromBinding(stack.getStyleBinding());
      if (boundStyle.hasBackground()) {
        mergedStyle.setBackground(
            mergedStyle.getBackground().toBuilder().mergeFrom(boundStyle.getBackground()));
      }
      if (boundStyle.hasColor()) {
        mergedStyle.setColor(boundStyle.getColor());
      }
    }
    return mergedStyle.build();
  }

  // Make sure the data is all current
  @SuppressWarnings("ReferenceEquality")
  private void checkCurrent() {
    PietSharedState pietSharedState = pietSharedStateSupplier.get();
    if (pietSharedState == null) {
      Logger.e(TAG, "Got null PietSharedState from supplier");
      return;
    }
    // If the new pietSharedState is .equals() but not ==, we do an expensive proto equality
    // calculation every time we call this method, and set ourselves up for more of these .equals
    // on our Stylesheets and Templates. Better to update everything to the new instance.
    // TODO: Maybe change this strategy; compare content ID or something.
    if (pietSharedState != currentPietSharedState) {
      stylesheetScopes.clear();
      stylesheets.clear();
      if (pietSharedState.getStylesheetsCount() > 0) {
        for (Stylesheet stylesheet : pietSharedState.getStylesheetsList()) {
          stylesheets.put(stylesheet.getStylesheetId(), stylesheet);
        }
      }

      templates.clear();
      for (Template template : pietSharedState.getTemplatesList()) {
        templates.put(template.getTemplateId(), template);
      }
      currentPietSharedState = pietSharedState;
    }
  }
}
