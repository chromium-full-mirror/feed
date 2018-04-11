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
import android.util.Base64;
import com.google.android.libraries.feed.api.common.MutationContext;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery;
import com.google.search.now.wire.feed.FeedQueryProto.FeedQuery.RequestReason;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.RequestProto.Request.RequestVersion;
import com.google.search.now.wire.feed.ResponseProto.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/** Default implementation of RequestManager. */
public class FeedRequestManager implements RequestManager {
  public static final String MOTHERSHIP_PARAM_PAYLOAD = "reqpld";
  private static final String TAG = "JardinRM";
  private static final String MOTHERSHIP_PARAM_FORMAT = "fmt";
  private static final String MOTHERSHIP_VALUE_BINARY = "bin";

  private final Configuration configuration;
  private final NetworkClient networkClient;
  private final ProtocolAdapter protocolAdapter;
  private final FeedExtensionRegistry extensionRegistry;
  private final SchedulerApi scheduler;
  private final ExecutorService executor;
  private final TimingUtils timingUtils;
  private final ThreadUtils threadUtils;

  public FeedRequestManager(
      Configuration configuration,
      NetworkClient networkClient,
      ProtocolAdapter protocolAdapter,
      FeedExtensionRegistry extensionRegistry,
      SchedulerApi scheduler,
      ExecutorService executor,
      TimingUtils timingUtils,
      ThreadUtils threadUtils) {
    this.configuration = configuration;
    this.networkClient = networkClient;
    this.protocolAdapter = protocolAdapter;
    this.extensionRegistry = extensionRegistry;
    this.scheduler = scheduler;
    this.executor = executor;
    this.timingUtils = timingUtils;
    this.threadUtils = threadUtils;
  }

  @Override
  public void loadMore(MutationContext mutationContext) {
    threadUtils.checkMainThread();
    ByteString token = Validators.checkNotNull(mutationContext.getContinuationToken());
    executor.execute(
        () -> {
          Logger.i(TAG, "Task: FeedRequestManager LoadMore");
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
          RequestBuilder request = newDefaultRequest().setPageToken(token);
          executeRequest(request, mutationContext);
          timeTracker.stop("task", "FeedRequestManager LoadMore");
        });
  }

  @Override
  public void triggerRefresh(RequestReason reason) {
    RequestBuilder request = newDefaultRequest();
    executeRequest(request, new MutationContext.Builder().build());
  }

  @Override
  public void handlePushedResponseBytes(byte[] responseBytes) {
    try {
      // TODO: Does this have a MutationContext?
      handleResponseBytes(
          responseBytes, new MutationContext.Builder().build(), /* isLengthPrefixed= */ false);
    } catch (IOException e) {
      Logger.e(TAG, e, "Error handling pushed response bytes.");
    }
  }

  private RequestBuilder newDefaultRequest() {
    return new RequestBuilder();
  }

  private void executeRequest(RequestBuilder requestBuilder, MutationContext mutationContext) {
    String host = configuration.getValueOrDefault(ConfigKey.FEED_SERVER_HOST, "");
    String path = configuration.getValueOrDefault(ConfigKey.FEED_SERVER_PATH_AND_PARAMS, "");
    Uri.Builder uriBuilder = Uri.parse(host + path).buildUpon();
    uriBuilder.appendQueryParameter(
        MOTHERSHIP_PARAM_PAYLOAD,
        Base64.encodeToString(
            requestBuilder.build().toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP));
    uriBuilder.appendQueryParameter(MOTHERSHIP_PARAM_FORMAT, MOTHERSHIP_VALUE_BINARY);

    HttpRequest httpRequest =
        new HttpRequest(uriBuilder.build(), ByteBuffer.allocate(0), HttpMethod.GET);
    networkClient.send(
        httpRequest,
        input -> {
          if (input.getResponseCode() != 200) {
            String errorBody = null;
            try {
              errorBody = new String(input.getResponseBody().array(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
              Logger.e(TAG, "Error handling http error logging", e);
            }
            Logger.e(TAG, "errorCode: %d", input.getResponseCode());
            Logger.e(TAG, "errorResponse: %s", errorBody);
            scheduler.onRequestError(input.getResponseCode());
            return;
          } else {
            scheduler.onReceiveNewContent();
          }

          try {
            handleResponseBytes(
                input.getResponseBody().array(), mutationContext, /* isLengthPrefixed= */ true);
          } catch (IOException e) {
            Logger.e(TAG, "Error handling response bytes", e);
          }
        });
  }

  private void handleResponseBytes(
      byte[] responseBytes, MutationContext mutationContext, boolean isLengthPrefixed)
      throws IOException {
    Response response =
        Response.parseFrom(
            isLengthPrefixed ? getLengthPrefixedValue(responseBytes) : responseBytes,
            extensionRegistry.getExtensionRegistry());
    protocolAdapter.createModel(response, mutationContext);
  }

  /**
   * Returns the first length-prefixed value from {@code input}. The first bytes of input are
   * assumed to be a varint32 encoding the length of the rest of the message. If input contains more
   * than one message, only the first message is returned.i w
   *
   * @throws IOException if input cannot be parsed
   */
  private byte[] getLengthPrefixedValue(byte[] input) throws IOException {
    CodedInputStream codedInputStream = CodedInputStream.newInstance(input);
    if (codedInputStream.isAtEnd()) {
      throw new IOException("Empty length-prefixed response");
    } else {
      int length = codedInputStream.readRawVarint32();
      return codedInputStream.readRawBytes(length);
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

    /*@Nullable*/
    ByteString getPageToken() {
      return token;
    }

    public Request build() {
      Request.Builder requestBuilder =
          Request.newBuilder().setRequestVersion(RequestVersion.FEED_QUERY);

      FeedQuery.Builder feedQuery = FeedQuery.newBuilder();
      if (token != null) {
        feedQuery.setPageToken(token);
      }
      requestBuilder.setExtension(
          FeedRequest.feedRequest, FeedRequest.newBuilder().setFeedQuery(feedQuery).build());

      return requestBuilder.build();
    }
  }
}
