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

package com.google.android.libraries.feed.feedrequestmanager;

import android.net.Uri;
import com.google.android.libraries.feed.api.common.ConfigManager;
import com.google.android.libraries.feed.api.common.ConfigManager.ConfigFlag;
import com.google.android.libraries.feed.api.network.HttpRequest;
import com.google.android.libraries.feed.api.network.HttpResponse;
import com.google.android.libraries.feed.api.network.NetworkClient;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.Consumer;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nullable;

/** Default implementation of RequestManager. */
public class FeedRequestManager implements RequestManager {
  private static final String TAG = "JardinRM";

  private static final ConfigFlag<String> SERVICE_ENDPOINT_FLAG =
      ConfigFlag.forString("feed_service_endpoint", null);

  private final ConfigManager configManager;
  private final NetworkClient networkClient;
  private final ProtocolAdapter protocolAdapter;
  private final FeedExtensionRegistry extensionRegistry;

  public FeedRequestManager(
      ConfigManager configManager,
      NetworkClient networkClient,
      ProtocolAdapter protocolAdapter,
      FeedExtensionRegistry extensionRegistry) {
    this.configManager = configManager;
    this.networkClient = networkClient;
    this.protocolAdapter = protocolAdapter;
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public void loadMore(ByteString pageToken) {
    RequestBuilder request = newDefaultRequest().setPageToken(pageToken);
    executeRequest(request);
  }

  @Override
  public void triggerRefresh(RequestReason reason) {
    RequestBuilder request = newDefaultRequest();
    executeRequest(request);
  }

  @Override
  public void handlePushedResponseBytes(byte[] responseBytes) {
    try {
      handleResponseBytes(responseBytes, null);
    } catch (IOException e) {
      Logger.e(TAG, e, "Error handling pushed response bytes.");
    }
  }

  private RequestBuilder newDefaultRequest() {
    return new RequestBuilder();
  }

  private void executeRequest(RequestBuilder requestBuilder) {
      Request request = requestBuilder.build();
      String endpoint = configManager.getValue(SERVICE_ENDPOINT_FLAG);
      HttpRequest httpRequest =
          new HttpRequest(Uri.parse(endpoint), request.toByteArray(), /* withAuthToken= */ true);
    networkClient.send(
        httpRequest,
        new Consumer<HttpResponse>() {
          @Override
          public void accept(HttpResponse input) {
            if (input.getResponseCode() != 200) {
              String errorBody = null;
              try {
                errorBody = new String(input.getResponseBody(), "UTF-8");
              } catch (UnsupportedEncodingException e) {
                Logger.e(TAG, "Error handling http error logging", e);
              }
              Logger.e(TAG, "errorCode: %d", input.getResponseCode());
              Logger.e(TAG, "errorResponse: %s", errorBody);
              return;
            }

            try {
              // TODO:  Figure out a more scalable to parse extensions.
              handleResponseBytes(input.getResponseBody(), requestBuilder.getPageToken());
            } catch (IOException e) {
              Logger.e(TAG, "Error handling response bytes");
            }
          }
        });
  }

  private void handleResponseBytes(byte[] responseBytes, ByteString pageToken) throws IOException {
    // Try parsing the bytes as a length-prefixed proto, then as an ordinary proto.
    Response response;
    try {
      response =
          Response.parseFrom(
              getLengthPrefixedValue(responseBytes), extensionRegistry.getExtensionRegistry());
    } catch (IOException responseIsNotLengthPrefixed) {
      response = Response.parseFrom(responseBytes, extensionRegistry.getExtensionRegistry());
    }
    protocolAdapter.createModel(response, pageToken);
  }

  /**
   * Returns the first length-prefixed value from {@code input}. The first bytes of input are
   * assumed to be a varint32 encoding the length of the rest of the message. If input contains more
   * than one message, only the first message is returned.
   *
   * @throws IOException if input cannot be parsed
   */
  private byte[] getLengthPrefixedValue(byte[] input) throws IOException {
    CodedInputStream codedInputStream = CodedInputStream.newInstance(input);
    try {
      if (codedInputStream.isAtEnd()) {
        throw new IOException("Empty length-prefixed response");
      } else {
        int length = codedInputStream.readRawVarint32();
        return codedInputStream.readRawBytes(length);
      }
    } catch (IOException e) {
      throw new IOException("Exception parsing length-prefixed response");
    }
  }

  // TODO: Populate ClientInfo
  private static class RequestBuilder {

    private ByteString token;

    /**
     * Sets the token used to tell the server which page of results we want in the response.
     *
     * @param token the token copied from FeedResponse.next_page_token.
     */
    RequestBuilder setPageToken(ByteString token) {
      this.token = token;
      return this;
    }

    @Nullable
    ByteString getPageToken() {
      return token;
    }

    public Request build() {

      FeedQuery.Builder feedQuery = FeedQuery.newBuilder();
      if (token != null) {
        feedQuery.setPageToken(token);
      }

      return Request.newBuilder()
          .setExtension(
              FeedRequest.feedRequest, FeedRequest.newBuilder().setFeedQuery(feedQuery).build())
          .build();
    }
  }
}
