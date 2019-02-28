// Copyright 2019 The Feed Authors.
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

package com.google.android.libraries.feed.feedrequestmanager;

import com.google.android.libraries.feed.api.actionmanager.ActionReader;
import com.google.android.libraries.feed.api.common.ActionPropertiesWithId;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.common.Result;
import com.google.search.now.feed.client.StreamDataProto.StreamUploadableAction;
import com.google.search.now.ui.action.FeedActionProto.FeedActionMetadata.Type;
import com.google.search.now.wire.feed.ActionPropertiesProto.ActionProperties;
import com.google.search.now.wire.feed.ActionRequestProto.ActionRequest;
import com.google.search.now.wire.feed.ConsistencyTokenProto.ConsistencyToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import com.google.search.now.wire.feed.FeedActionProto.FeedAction;
import com.google.search.now.wire.feed.FeedActionRequestProto.FeedActionRequest;
import com.google.search.now.wire.feed.UploadActionTypeProto.UploadActionType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A class that creates an ActionsRequest for uploading actions
final class UploadableActionsRequestBuilder {
  private Set<StreamUploadableAction> uploadableActions;
  private ConsistencyToken token;
  private final ActionReader actionReader;
  private final ProtocolAdapter protocolAdapter;

  UploadableActionsRequestBuilder(ActionReader actionReader, ProtocolAdapter protocolAdapter) {
    this.actionReader = actionReader;
    this.protocolAdapter = protocolAdapter;
  }

  UploadableActionsRequestBuilder setConsistencyToken(ConsistencyToken token) {
    this.token = token;
    return this;
  }

  boolean hasConsistencyToken() {
    return token != null;
  }

  UploadableActionsRequestBuilder setActions(Set<StreamUploadableAction> uploadableActions) {
    this.uploadableActions = uploadableActions;
    return this;
  }

  public ActionRequest build() {
    ActionRequest.Builder requestBuilder =
        ActionRequest.newBuilder()
            .setRequestVersion(ActionRequest.RequestVersion.FEED_UPLOAD_ACTION);
    FeedActionRequest.Builder feedActionRequestBuilder = FeedActionRequest.newBuilder();
    if (uploadableActions != null) {
      Map<String, ActionProperties> actionPropertyMap = new HashMap<>();
      List<String> contentIds = new ArrayList<>(uploadableActions.size());
      for (StreamUploadableAction action : uploadableActions) {
        contentIds.add(action.getFeatureContentId());
      }
      List<ActionPropertiesWithId> actionProperties = getActionPropertiesForIds(contentIds);
      for (ActionPropertiesWithId property : actionProperties) {
        actionPropertyMap.put(
            property.contentId,
            ActionProperties.newBuilder().setActionData(property.actionData).build());
      }
      for (StreamUploadableAction action : uploadableActions) {
        String contentId = action.getFeatureContentId();
        FeedAction.Builder feedAction = FeedAction.newBuilder();
        ActionProperties properties = actionPropertyMap.get(contentId);
        if (properties != null) {
          feedAction.setActionProperties(properties);
        }
        feedAction.setActionType(convertActionType(action.getActionType()));
        Result<ContentId> contentIdResult = protocolAdapter.getWireContentId(contentId);
        if (contentIdResult.isSuccessful()) {
          feedAction.setContentId(contentIdResult.getValue());
        }
        feedActionRequestBuilder.addFeedAction(feedAction);
      }
    }
    if (hasConsistencyToken()) {
      feedActionRequestBuilder.setConsistencyToken(token);
    }
    requestBuilder.setExtension(
        FeedActionRequest.feedActionRequest, feedActionRequestBuilder.build());

    return requestBuilder.build();
  }

  private UploadActionType convertActionType(Type actionType) {
    switch (actionType) {
      case DISMISS:
        return UploadActionType.DISMISS_UPLOAD;
      case NOT_INTERESTED_IN_SOURCE:
        return UploadActionType.NOT_INTERESTED_IN_SOURCE;
      case NOT_INTERESTED_IN_TOPIC:
        return UploadActionType.NOT_INTERESTED_IN_TOPIC;
      case OPEN_URL:
      case OPEN_URL_NEW_WINDOW:
      case OPEN_URL_INCOGNITO:
      case OPEN_URL_NEW_TAB:
      case OPEN_CONTEXT_MENU:
      case DISMISS_LOCAL:
      case DOWNLOAD:
      case LEARN_MORE:
      case VIEW_ELEMENT:
      case HIDE_ELEMENT:
      default:
        return UploadActionType.UNKNOWN_UPLOAD_ACTION_TYPE;
    }
  }

  private List<ActionPropertiesWithId> getActionPropertiesForIds(List<String> ids) {
    Result<List<ActionPropertiesWithId>> actionResult = actionReader.getActionProperties(ids);
    if (actionResult.isSuccessful()) {
      return actionResult.getValue();
    }
    return new ArrayList<>();
  }
}
