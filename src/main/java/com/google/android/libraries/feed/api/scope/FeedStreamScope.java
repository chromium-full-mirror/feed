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

package com.google.android.libraries.feed.api.scope;

import android.content.Context;
import com.google.android.libraries.feed.api.actionparser.ActionParser;
import com.google.android.libraries.feed.api.common.Clock;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.api.stream.CardConfiguration;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.host.action.ActionApi;
import com.google.android.libraries.feed.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.host.logging.LoggingApi;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import javax.annotation.Nullable;

/** Per-stream instance of the feed library. */
public class FeedStreamScope {

  public Stream getStream() {
    return stream;
  };

  public ModelProviderFactory getModelProviderFactory() {
    return modelProviderFactory;
  }

  /** A builder that creates a {@link FeedStreamScope}. */
  public static class Builder {
    /** Construct this builder using {@link FeedProcessScope#createFeedStreamScopeBuilder} */
    Builder(
        Context context,
        ActionApi actionApi,
        ImageLoaderApi imageLoaderApi,
        LoggingApi loggingApi,
        FeedProtocolAdapter feedProtocolAdapter,
        SessionManager sessionManager,
        ThreadUtils threadUtils,
        TimingUtils timingUtils,
        UiRunnableHandler uiRunnableHandler,
        Clock clock,
        CardConfiguration cardConfiguration) {
      this.context = context;
      this.actionApi = actionApi;
      this.imageLoaderApi = imageLoaderApi;
      this.loggingApi = loggingApi;
      this.feedProtocolAdapter = feedProtocolAdapter;
      this.sessionManager = sessionManager;
      this.threadUtils = threadUtils;
      this.timingUtils = timingUtils;
      this.uiRunnableHandler = uiRunnableHandler;
      this.clock = clock;
      this.cardConfiguration = cardConfiguration;
    }

    public Builder setActionParser(ActionParser actionParser) {
      this.actionParser = actionParser;
      return this;
    }

    public Builder setStream(Stream stream) {
      this.stream = stream;
      return this;
    }

    public Builder setModelProviderFactory(ModelProviderFactory modelProviderFactory) {
      this.modelProviderFactory = modelProviderFactory;
      return this;
    }

    public Builder setCustomElementProvider(CustomElementProvider customElementProvider) {
      this.customElementProvider = customElementProvider;
      return this;
    }

    public FeedStreamScope build() {
      if (modelProviderFactory == null) {
        modelProviderFactory =
            new FeedModelProviderFactory(
                sessionManager, threadUtils, timingUtils, uiRunnableHandler);
      }
      // TODO: if pietManager == null, assign a PietManager to pietManager.
      // TODO: if stream == null, assign a BasicStream to stream.
      return new FeedStreamScope(
          actionApi,
          actionParser,
          feedProtocolAdapter,
          imageLoaderApi,
          loggingApi,
          sessionManager,
          stream,
          modelProviderFactory);
    }

    // Required external dependencies.
    private final Context context;
    private final ActionApi actionApi;
    private final ImageLoaderApi imageLoaderApi;
    private final LoggingApi loggingApi;
    private final FeedProtocolAdapter feedProtocolAdapter;
    private final SessionManager sessionManager;
    private final ThreadUtils threadUtils;
    private final TimingUtils timingUtils;
    private final UiRunnableHandler uiRunnableHandler;
    private final Clock clock;
    private final CardConfiguration cardConfiguration;

    // Optional internal components to override the default implementations.
    @Nullable private ActionParser actionParser = null;
    @Nullable private ModelProviderFactory modelProviderFactory = null;
    @Nullable private Stream stream = null;
    @Nullable private CustomElementProvider customElementProvider = null;
  }

  private FeedStreamScope(
      ActionApi actionApi,
      ActionParser actionParser,
      FeedProtocolAdapter feedProtocolAdapter,
      ImageLoaderApi imageLoaderApi,
      LoggingApi loggingApi,
      SessionManager sessionManager,
      Stream stream,
      ModelProviderFactory modelProviderFactory) {
    this.actionParser = actionParser;
    this.actionApi = actionApi;
    this.feedProtocolAdapter = feedProtocolAdapter;
    this.imageLoaderApi = imageLoaderApi;
    this.loggingApi = loggingApi;
    this.sessionManager = sessionManager;
    this.stream = stream;
    this.modelProviderFactory = modelProviderFactory;
  }

  private final ActionApi actionApi;
  private final ImageLoaderApi imageLoaderApi;
  private final LoggingApi loggingApi;
  private final ActionParser actionParser;
  private final FeedProtocolAdapter feedProtocolAdapter;
  private final SessionManager sessionManager;
  private final Stream stream;
  private final ModelProviderFactory modelProviderFactory;
}
