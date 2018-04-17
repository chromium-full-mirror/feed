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

package com.google.android.libraries.feed.mocknetworkclient;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;
import com.google.android.libraries.feed.api.requestmanager.RequestManager;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.feedrequestmanager.FeedRequestManager;
import com.google.android.libraries.feed.host.network.HttpRequest;
import com.google.android.libraries.feed.host.network.HttpRequest.HttpMethod;
import com.google.android.libraries.feed.host.network.HttpResponse;
import com.google.android.libraries.feed.host.network.NetworkClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.search.now.wire.feed.FeedRequestProto.FeedRequest;
import com.google.search.now.wire.feed.RequestProto.Request;
import com.google.search.now.wire.feed.ResponseProto.Response;
import com.google.search.now.wire.feed.mockserver.MockServerProto.ConditionalResponse;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockServer;
import com.google.search.now.wire.feed.mockserver.MockServerProto.MockUpdate;
import java.io.IOException;
import java.util.List;

/** A network client that returns mock responses provided via a config proto */
public class MockServerNetworkClient implements NetworkClient {
  private static final String TAG = "MockServerNetworkClient";

  private final Response initialResponse;
  private final List<ConditionalResponse> conditionalResponses;
  private final ExtensionRegistryLite extensionRegistry;

  @VisibleForTesting final MockServerLooper looper;
  @VisibleForTesting boolean hasQuit = false;

  /*@Nullable*/ @VisibleForTesting RequestManager requestManager;

  @SuppressWarnings("nullness")
  public MockServerNetworkClient(MockServer mockServer) {
    this.initialResponse = mockServer.getInitialResponse();
    conditionalResponses = mockServer.getConditionalResponsesList();

    this.looper = new MockServerLooper();
    Handler handler = looper.getHandler();
    extensionRegistry = ExtensionRegistryLite.newInstance();
    extensionRegistry.add(FeedRequest.feedRequest);
    // This triggers a nullness warning, suppressing above.  This should be ok since this is a mock
    // class.
    for (MockUpdate update : mockServer.getMockUpdatesList()) {
      handler.postDelayed(() -> handleUpdate(update.getResponse()), update.getUpdateTriggerTime());
    }
  }

  /** Called to set the {@code RequestManager}. This is required to support updates */
  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  /** Called to shutdown the Looper providing support for Mock Updates */
  public void quitSafely() {
    looper.getHandler().removeCallbacksAndMessages(null);
    looper.quitSafely();
    hasQuit = true;
  }

  @Override
  public void send(HttpRequest httpRequest, Consumer<HttpResponse> responseConsumer) {
    try {
      Request request = getRequest(httpRequest);
      ByteString requestToken =
          (request.getExtension(FeedRequest.feedRequest).getFeedQuery().hasPageToken())
              ? request.getExtension(FeedRequest.feedRequest).getFeedQuery().getPageToken()
              : null;
      if (requestToken != null) {
        for (ConditionalResponse response : conditionalResponses) {
          if (!response.hasContinuationToken()) {
            Logger.w(TAG, "Conditional response without a token");
            continue;
          }
          if (requestToken.equals(response.getContinuationToken())) {
            responseConsumer.accept(createHttpResponse(response.getResponse()));
            return;
          }
        }
      }
      responseConsumer.accept(createHttpResponse(initialResponse));
    } catch (IOException e) {
      // TODO : handle errors here
      Logger.e(TAG, e.getMessage());
      responseConsumer.accept(new HttpResponse(400));
    }
  }

  private Request getRequest(HttpRequest httpRequest) throws IOException {
    byte[] rawRequest = new byte[0];
    if (httpRequest.getMethod().equals(HttpMethod.GET)) {
      if (httpRequest.getUri().getQueryParameter(FeedRequestManager.MOTHERSHIP_PARAM_PAYLOAD)
          != null) {
        rawRequest =
            Base64.decode(
                httpRequest.getUri().getQueryParameter(FeedRequestManager.MOTHERSHIP_PARAM_PAYLOAD),
                Base64.URL_SAFE);
      }
    } else {
      rawRequest = httpRequest.getBody();
    }
    return Request.parseFrom(rawRequest, extensionRegistry);
  }

  @Override
  public void close() {
    quitSafely();
  }

  @VisibleForTesting
  void handleUpdate(Response response) {
    RequestManager rm = Validators.checkNotNull(requestManager);
    if (hasQuit) {
      Logger.i(TAG, "Calling update after MockServerNetworkClient has quit");
      return;
    }
    Logger.i(TAG, "Calling handlePushedResponse");
    rm.handlePushedResponseBytes(response.toByteArray());
  }

  private HttpResponse createHttpResponse(Response response) {
    if (response == null) {
      return new HttpResponse(500);
    }

    try {
      byte[] rawResponse = response.toByteArray();
      byte[] newResponse = new byte[rawResponse.length + (Integer.SIZE / 8)];
      CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(newResponse);
      codedOutputStream.writeUInt32NoTag(rawResponse.length);
      codedOutputStream.writeRawBytes(rawResponse);
      codedOutputStream.flush();
      return new HttpResponse(200, newResponse);
    } catch (IOException e) {
      Logger.e(TAG, "Error creating response", e);
      return new HttpResponse(500);
    }
  }

  @VisibleForTesting
  static class MockServerLooper {
    private final HandlerThread updateThread;
    private final Handler handler;

    private MockServerLooper() {
      Logger.i(TAG, "createing MockServerLooperImpl");
      updateThread = new HandlerThread("UpdateThread");
      updateThread.start();
      handler = new Handler(updateThread.getLooper());
    }

    Handler getHandler() {
      return handler;
    }

    void quitSafely() {
      Logger.i(TAG, "Looper quitSafely");
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
        updateThread.quitSafely();
      } else {
        updateThread.quit();
      }
    }
  }
}
