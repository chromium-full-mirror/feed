// Copyright 2019 The Feed Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License") {}
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

package com.google.android.libraries.feed.common.testing;

import com.google.android.libraries.feed.host.logging.ActionType;
import com.google.android.libraries.feed.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.host.logging.ContentLoggingData;
import com.google.android.libraries.feed.host.logging.ElementLoggingData;
import com.google.android.libraries.feed.host.logging.ElementType;
import com.google.android.libraries.feed.host.logging.InternalFeedError;
import com.google.android.libraries.feed.host.logging.RequestReason;
import com.google.android.libraries.feed.host.logging.SessionEvent;
import com.google.android.libraries.feed.host.logging.SpinnerType;
import com.google.android.libraries.feed.host.logging.ZeroStateShowReason;
import java.util.List;

/** Fake implementation of {@link BasicLoggingApi}. */
public class FakeBasicLoggingApi implements BasicLoggingApi {

  @RequestReason public int serverRequestReason = RequestReason.UNKNOWN;

  public FakeBasicLoggingApi() {}

  @Override
  public void onContentViewed(ContentLoggingData data) {}

  @Override
  public void onContentDismissed(ContentLoggingData data, boolean wasCommitted) {}

  @Override
  public void onContentSwiped(ContentLoggingData data) {}

  @Override
  public void onContentClicked(ContentLoggingData data) {}

  @Override
  public void onClientAction(ContentLoggingData data, @ActionType int actionType) {}

  @Override
  public void onContentContextMenuOpened(ContentLoggingData data) {}

  @Override
  public void onInitialSessionEvent(
      @SessionEvent int sessionEvent, int timeFromRegisteringMs, int sessionCount) {}

  @Override
  public void onMoreButtonViewed(int position) {}

  @Override
  public void onMoreButtonClicked(int position) {}

  @Override
  public void onNotInterestedInSource(ContentLoggingData data, boolean wasCommitted) {}

  @Override
  public void onNotInterestedInTopic(ContentLoggingData data, boolean wasCommitted) {}

  @Override
  public void onOpenedWithContent(int timeToPopulateMs, int contentCount) {}

  @Override
  public void onOpenedWithNoImmediateContent() {}

  @Override
  public void onOpenedWithNoContent() {}

  @Override
  public void onServerRequest(@RequestReason int requestReason) {
    serverRequestReason = requestReason;
  }

  @Override
  public void onSpinnerDestroyedWithoutCompleting(int timeShownMs, @SpinnerType int spinnerType) {}

  @Override
  public void onSpinnerFinished(int timeShownMs, @SpinnerType int spinnerType) {}

  @Override
  public void onSpinnerStarted(@SpinnerType int spinnerType) {}

  @Override
  public void onTokenCompleted(boolean wasSynthetic, int contentCount, int tokenCount) {}

  @Override
  public void onTokenFailedToComplete(boolean wasSynthetic, int failureCount) {}

  @Override
  public void onPietFrameRenderingEvent(List<Integer> pietErrorCodes) {}

  @Override
  public void onVisualElementClicked(ElementLoggingData data, @ElementType int elementType) {}

  @Override
  public void onVisualElementViewed(ElementLoggingData data, @ElementType int elementType) {}

  @Override
  public void onInternalError(@InternalFeedError int internalError) {}

  @Override
  public void onZeroStateRefreshCompleted(int newContentCount, int newTokenCount) {}

  @Override
  public void onZeroStateShown(@ZeroStateShowReason int zeroStateShowReason) {}
}
