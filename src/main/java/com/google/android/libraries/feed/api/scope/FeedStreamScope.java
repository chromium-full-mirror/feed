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
import com.google.android.libraries.feed.api.host.action.ActionApi;
import com.google.android.libraries.feed.api.host.config.ApplicationInfo;
import com.google.android.libraries.feed.api.host.config.Configuration;
import com.google.android.libraries.feed.api.host.config.DebugBehavior;
import com.google.android.libraries.feed.api.host.imageloader.ImageLoaderApi;
import com.google.android.libraries.feed.api.host.logging.BasicLoggingApi;
import com.google.android.libraries.feed.api.host.offlineindicator.OfflineIndicatorApi;
import com.google.android.libraries.feed.api.host.stream.CardConfiguration;
import com.google.android.libraries.feed.api.host.stream.SnackbarApi;
import com.google.android.libraries.feed.api.host.stream.StreamConfiguration;
import com.google.android.libraries.feed.api.host.stream.TooltipApi;
import com.google.android.libraries.feed.api.host.stream.TooltipSupportedApi;
import com.google.android.libraries.feed.api.internal.knowncontent.FeedKnownContent;
import com.google.android.libraries.feed.api.internal.sessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.api.stream.Stream;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedactionparser.FeedActionParserFactory;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.internalapi.actionmanager.ActionManager;
import com.google.android.libraries.feed.internalapi.actionparser.ActionParserFactory;
import com.google.android.libraries.feed.internalapi.common.ThreadUtils;
import com.google.android.libraries.feed.internalapi.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.internalapi.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.internalapi.stream.BasicStreamFactory;
import com.google.android.libraries.feed.internalapi.stream.StreamFactory;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.android.libraries.feed.piet.host.ThrowingCustomElementProvider;

/** Per-stream instance of the feed library. */
public final class FeedStreamScope {

  public Stream getStream() {
    return stream;
  }

  public ModelProviderFactory getModelProviderFactory() {
    return modelProviderFactory;
  }

  /** A builder that creates a {@link FeedStreamScope}. */
  public static final class Builder {
    /** Construct this builder using {@link FeedProcessScope#createFeedStreamScopeBuilder} */
    Builder(
        Context context,
        ActionApi actionApi,
        ImageLoaderApi imageLoaderApi,
        ProtocolAdapter protocolAdapter,
        FeedSessionManager feedSessionManager,
        ThreadUtils threadUtils,
        TimingUtils timingUtils,
        TaskQueue taskQueue,
        MainThreadRunner mainThreadRunner,
        Clock clock,
        DebugBehavior debugBehavior,
        StreamConfiguration streamConfiguration,
        CardConfiguration cardConfiguration,
        ActionManager actionManager,
        Configuration config,
        SnackbarApi snackbarApi,
        BasicLoggingApi basicLoggingApi,
        OfflineIndicatorApi offlineIndicatorApi,
        FeedKnownContent feedKnownContent,
        TooltipApi tooltipApi,
        TooltipSupportedApi tooltipSupportedApi,
        ApplicationInfo applicationInfo,
        FeedExtensionRegistry feedExtensionRegistry) {
      this.context = context;
      this.actionApi = actionApi;
      this.imageLoaderApi = imageLoaderApi;
      this.protocolAdapter = protocolAdapter;
      this.feedSessionManager = feedSessionManager;
      this.threadUtils = threadUtils;
      this.timingUtils = timingUtils;
      this.taskQueue = taskQueue;
      this.mainThreadRunner = mainThreadRunner;
      this.streamConfiguration = streamConfiguration;
      this.cardConfiguration = cardConfiguration;
      this.clock = clock;
      this.debugBehavior = debugBehavior;
      this.actionManager = actionManager;
      this.config = config;
      this.snackbarApi = snackbarApi;
      this.basicLoggingApi = basicLoggingApi;
      this.offlineIndicatorApi = offlineIndicatorApi;
      this.feedKnownContent = feedKnownContent;
      this.tooltipApi = tooltipApi;
      this.applicationInfo = applicationInfo;
      this.feedExtensionRegistry = feedExtensionRegistry;
    }

    public Builder setIsBackgroundDark(boolean isBackgroundDark) {
      this.isBackgroundDark = isBackgroundDark;
      return this;
    }

    public Builder setStreamFactory(StreamFactory streamFactory) {
      this.streamFactory = streamFactory;
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

    public Builder setHostBindingProvider(HostBindingProvider hostBindingProvider) {
      this.hostBindingProvider = hostBindingProvider;
      return this;
    }

    public FeedStreamScope build() {
      if (modelProviderFactory == null) {
        modelProviderFactory =
            new FeedModelProviderFactory(
                feedSessionManager, threadUtils, timingUtils, taskQueue, mainThreadRunner, config);
      }
      if (actionParserFactory == null) {
        actionParserFactory = new FeedActionParserFactory(protocolAdapter, basicLoggingApi);
      }
      if (customElementProvider == null) {
        customElementProvider = new ThrowingCustomElementProvider();
      }
      if (hostBindingProvider == null) {
        hostBindingProvider = new HostBindingProvider();
      }
      if (streamFactory == null) {
        streamFactory = new BasicStreamFactory();
      }
      stream =
          streamFactory.build(
              Validators.checkNotNull(actionParserFactory),
              context,
              applicationInfo.getBuildType(),
              cardConfiguration,
              imageLoaderApi,
              Validators.checkNotNull(customElementProvider),
              debugBehavior,
              clock,
              Validators.checkNotNull(modelProviderFactory),
              Validators.checkNotNull(hostBindingProvider),
              offlineIndicatorApi,
              config,
              actionApi,
              actionManager,
              snackbarApi,
              streamConfiguration,
              feedExtensionRegistry,
              basicLoggingApi,
              mainThreadRunner,
              isBackgroundDark,
              tooltipApi,
              threadUtils,
              feedKnownContent);
      return new FeedStreamScope(
          Validators.checkNotNull(stream), Validators.checkNotNull(modelProviderFactory));
    }

    // Required external dependencies.
    private final Context context;
    private final ActionApi actionApi;
    private final ImageLoaderApi imageLoaderApi;

    private final ProtocolAdapter protocolAdapter;
    private final FeedSessionManager feedSessionManager;
    private final ThreadUtils threadUtils;
    private final TimingUtils timingUtils;
    private final TaskQueue taskQueue;
    private final MainThreadRunner mainThreadRunner;
    private final Clock clock;
    private final ActionManager actionManager;
    private final CardConfiguration cardConfiguration;
    private final StreamConfiguration streamConfiguration;
    private final DebugBehavior debugBehavior;
    private final Configuration config;
    private final SnackbarApi snackbarApi;
    private final BasicLoggingApi basicLoggingApi;
    private final OfflineIndicatorApi offlineIndicatorApi;
    private final FeedKnownContent feedKnownContent;
    private final TooltipApi tooltipApi;
    private final ApplicationInfo applicationInfo;
    private final FeedExtensionRegistry feedExtensionRegistry;
    private boolean isBackgroundDark;

    // Optional internal components to override the default implementations.
    /*@MonotonicNonNull*/ private ActionParserFactory actionParserFactory;
    /*@MonotonicNonNull*/ private ModelProviderFactory modelProviderFactory;
    /*@MonotonicNonNull*/ private Stream stream;
    /*@MonotonicNonNull*/ private StreamFactory streamFactory;
    /*@MonotonicNonNull*/ private CustomElementProvider customElementProvider;
    /*@MonotonicNonNull*/ private HostBindingProvider hostBindingProvider;
  }

  private FeedStreamScope(Stream stream, ModelProviderFactory modelProviderFactory) {
    this.stream = stream;
    this.modelProviderFactory = modelProviderFactory;
  }

  private final Stream stream;
  private final ModelProviderFactory modelProviderFactory;
}
