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

package com.google.android.libraries.feed.basicstream.internal.drivers;

import static com.google.android.libraries.feed.common.Validators.checkNotNull;
import static com.google.android.libraries.feed.common.Validators.checkState;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelError;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.api.modelprovider.TokenCompletedObserver;
import com.google.android.libraries.feed.basicstream.internal.scroll.ScrollRestorer;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ContinuationViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.FeedViewHolder;
import com.google.android.libraries.feed.basicstream.internal.viewholders.R;
import com.google.android.libraries.feed.basicstream.internal.viewholders.ViewHolderType;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.stream.SnackbarApi;
import java.util.ArrayList;
import java.util.List;

/** {@link FeatureDriver} for the more button. */
public class ContinuationDriver extends LeafFeatureDriver
    implements OnClickListener, TokenCompletedObserver {

  private static final String TAG = "ContinuationDriver";
  private final Context context;
  private final CursorChangedListener cursorChangedListener;
  private final ModelChild modelChild;
  private final ModelToken modelToken;
  private final ModelProvider modelProvider;
  private final ScrollRestorer scrollRestorer;
  private final SnackbarApi snackbarApi;
  private final ThreadUtils threadUtils;
  private boolean showSpinner;
  private boolean initialized;
  /*@Nullable*/ private ContinuationViewHolder continuationViewHolder;

  ContinuationDriver(
      Configuration configuration,
      Context context,
      CursorChangedListener cursorChangedListener,
      ModelChild modelChild,
      ModelProvider modelProvider,
      ScrollRestorer scrollRestorer,
      SnackbarApi snackbarApi,
      ThreadUtils threadUtils) {
    this.context = context;
    this.cursorChangedListener = cursorChangedListener;
    this.modelChild = modelChild;
    this.modelProvider = modelProvider;
    this.modelToken = modelChild.getModelToken();
    this.scrollRestorer = scrollRestorer;
    this.snackbarApi = snackbarApi;
    this.threadUtils = threadUtils;
    this.showSpinner =
        configuration.getValueOrDefault(ConfigKey.TRIGGER_IMMEDIATE_PAGINATION, false);
  }

  public boolean isSynthetic() {
    return modelToken.isSynthetic();
  }

  public void initialize() {
    if (initialized) {
      return;
    }

    initialized = true;
    modelToken.registerObserver(this);
    if (modelToken.isSynthetic()) {
      Logger.d(TAG, "Handling synthetic token");
      showSpinner = true;
      modelProvider.handleToken(modelToken);
    }
  }

  // TODO: Instead of implementing an onClickListener, define a new interface with a method
  // with no view argument.
  @Override
  public void onClick(View v) {
    if (!isBound()) {
      Logger.wtf(TAG, "Calling onClick before binding.");
      return;
    }
    showSpinner = true;
    checkNotNull(continuationViewHolder).setShowSpinner(true);
    modelProvider.handleToken(modelToken);
  }

  @Override
  public void bind(FeedViewHolder viewHolder) {
    checkState(initialized);
    if (isBound()) {
      Logger.wtf(TAG, "Rebinding.");
    }
    checkState(viewHolder instanceof ContinuationViewHolder);

    ((ContinuationViewHolder) viewHolder).bind(this, showSpinner);
    continuationViewHolder = (ContinuationViewHolder) viewHolder;
  }

  @Override
  public int getItemViewType() {
    return ViewHolderType.TYPE_CONTINUATION;
  }

  @Override
  public void unbind() {
    if (continuationViewHolder == null) {
      return;
    }

    continuationViewHolder.unbind();
    continuationViewHolder = null;
  }

  @VisibleForTesting
  boolean isBound() {
    return continuationViewHolder != null;
  }

  @Override
  public void onTokenCompleted(TokenCompleted tokenCompleted) {
    threadUtils.checkMainThread();
    ModelCursor cursor = tokenCompleted.getCursor();
    List<ModelChild> modelChildren = extractModelChildrenFromCursor(cursor);

    // Display snackbar if there are no more cards.
    if (modelChildren.isEmpty()
        || (modelChildren.size() == 1 && modelChildren.get(0).getType() == Type.TOKEN)) {
      snackbarApi.show(
          context.getResources().getString(R.string.ntp_suggestions_fetch_no_new_suggestions));
    }

    // TODO: Handle continuation driver. This needs to handle synthetic tokens.
    if (!isSynthetic()) {
      scrollRestorer.maybeRestoreScroll();
    }
    cursorChangedListener.onNewChildren(modelChild, modelChildren);
  }

  @Override
  public void onError(ModelError modelError) {
    showSpinner = false;

    if (continuationViewHolder != null) {
      continuationViewHolder.setShowSpinner(false);
    }

    snackbarApi.show(context.getString(R.string.ntp_suggestions_fetch_failed));
  }

  private List<ModelChild> extractModelChildrenFromCursor(ModelCursor cursor) {
    List<ModelChild> modelChildren = new ArrayList<>();
    ModelChild child;
    while ((child = cursor.getNextItem()) != null) {
      if (child.getType() == Type.UNBOUND) {
        Logger.e(TAG, "Found unbound child %s, ignoring it", child.getContentId());
        continue;
      } else if (child.getType() != Type.FEATURE && child.getType() != Type.TOKEN) {
        Logger.wtf(TAG, "Received illegal child: %s from cursor.", child.getType());
        continue;
      }
      modelChildren.add(child);
    }

    return modelChildren;
  }

  /** Interface for notifying parents of new model children from a token. */
  public interface CursorChangedListener {

    /**
     * Called to inform parent of new model children.
     *
     * @param modelChild the {@link ModelChild} representing the token that was processed.
     * @param modelChildren the list of new {@link ModelChild} from the token.
     */
    void onNewChildren(ModelChild modelChild, List<ModelChild> modelChildren);
  }
}
