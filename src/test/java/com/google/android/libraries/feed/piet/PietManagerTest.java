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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.functional.Supplier;
import com.google.android.libraries.feed.common.testing.Suppliers;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.SystemClockImpl;
import com.google.android.libraries.feed.common.time.testing.FakeClock;
import com.google.android.libraries.feed.host.config.DebugBehavior;
import com.google.android.libraries.feed.piet.host.ActionHandler;
import com.google.android.libraries.feed.piet.host.AssetProvider;
import com.google.android.libraries.feed.piet.host.CustomElementProvider;
import com.google.android.libraries.feed.piet.host.EventLogger;
import com.google.android.libraries.feed.piet.host.HostBindingProvider;
import com.google.android.libraries.feed.piet.host.ThrowingCustomElementProvider;
import com.google.search.now.ui.piet.PietProto.Frame;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests of the {@link PietManager}. */
@RunWith(RobolectricTestRunner.class)
public class PietManagerTest {

  @Mock private ActionHandler actionHandler;
  @Mock private EventLogger eventLogger;
  @Mock private AssetProvider assetProvider;
  @Mock private CustomElementProvider customElementProvider;
  @Mock Context mockContext1;
  @Mock Context mockContext2;

  private Context context;
  private ViewGroup viewGroup1;
  private ViewGroup viewGroup2;

  private PietManager pietManager;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    context = Robolectric.buildActivity(Activity.class).get();
    viewGroup1 = new LinearLayout(context);
    viewGroup2 = new FrameLayout(context);
    pietManager =
        PietManager.builder(assetProvider)
            .setDebugBehavior(DebugBehavior.VERBOSE)
            .setCustomElementProvider(customElementProvider)
            .build();
  }

  @Test
  public void testCreatePietFrameAdapter() {
    Supplier<ViewGroup> cardViewSupplier = Suppliers.of(viewGroup1);
    FrameAdapter frameAdapter =
        pietManager.createPietFrameAdapter(cardViewSupplier, actionHandler, eventLogger, context);
    assertThat(frameAdapter.getParameters().parentViewSupplier).isSameAs(cardViewSupplier);
  }

  @Test
  public void testGetAdapterParameters() {
    Supplier<ViewGroup> viewGroupProducer1 = Suppliers.of(viewGroup1);
    Supplier<ViewGroup> viewGroupProducer2 = Suppliers.of(viewGroup2);
    AdapterParameters returnParams;

    // Get params for a context that does not exist
    returnParams = pietManager.getAdapterParameters(mockContext1, viewGroupProducer1);
    assertThat(returnParams.parentViewSupplier).isEqualTo(viewGroupProducer1);
    assertThat(returnParams.context).isEqualTo(mockContext1);

    // Get params for the same context again (use cached value)
    returnParams = pietManager.getAdapterParameters(mockContext1, Suppliers.of(null));
    assertThat(returnParams.parentViewSupplier).isEqualTo(viewGroupProducer1);

    // Get params for a different context
    returnParams = pietManager.getAdapterParameters(mockContext2, viewGroupProducer2);
    assertThat(returnParams.parentViewSupplier).isEqualTo(viewGroupProducer2);
  }

  @Test
  public void testBuilder_defaults() {
    PietManager manager = PietManager.builder(assetProvider).build();
    AdapterParameters parameters = manager.getAdapterParameters(context, Suppliers.of(viewGroup1));
    assertThat(parameters.hostProviders.getAssetProvider()).isSameAs(assetProvider);
    assertThat(parameters.hostProviders.getCustomElementProvider())
        .isInstanceOf(ThrowingCustomElementProvider.class);
    assertThat(parameters.clock).isInstanceOf(SystemClockImpl.class);
    // There's no good way to test the HostBindingProvider.

    FrameAdapter frameAdapter =
        manager.createPietFrameAdapter(
            Suppliers.of(viewGroup1), actionHandler, eventLogger, context);
    FrameContext frameContext =
        frameAdapter.createFrameContext(
            Frame.getDefaultInstance(), 0, Collections.emptyList(), viewGroup2);
    assertThat(frameContext.getDebugBehavior()).isEqualTo(DebugBehavior.SILENT);
  }

  @Test
  public void testBuilder_setters() {
    HostBindingProvider hostBindingProvider = new HostBindingProvider();
    Clock clock = new FakeClock();
    PietManager manager =
        PietManager.builder(assetProvider)
            .setDebugBehavior(DebugBehavior.VERBOSE)
            .setCustomElementProvider(customElementProvider)
            .setHostBindingProvider(hostBindingProvider)
            .setClock(clock)
            .build();
    AdapterParameters parameters = manager.getAdapterParameters(context, Suppliers.of(viewGroup1));
    assertThat(parameters.hostProviders.getAssetProvider()).isSameAs(assetProvider);
    assertThat(parameters.hostProviders.getCustomElementProvider()).isSameAs(customElementProvider);
    assertThat(parameters.hostProviders.getHostBindingProvider()).isSameAs(hostBindingProvider);
    assertThat(parameters.clock).isSameAs(clock);

    FrameAdapter frameAdapter =
        manager.createPietFrameAdapter(
            Suppliers.of(viewGroup1), actionHandler, eventLogger, context);
    FrameContext frameContext =
        frameAdapter.createFrameContext(
            Frame.getDefaultInstance(), 0, Collections.emptyList(), viewGroup2);
    assertThat(frameContext.getDebugBehavior()).isEqualTo(DebugBehavior.VERBOSE);
  }

  @Test
  public void testPurgeRecyclerPools() {
    // Test with null adapterParameters
    pietManager.purgeRecyclerPools();

    ElementAdapterFactory mockFactory = mock(ElementAdapterFactory.class);
    TemplateBinder mockTemplateBinder = mock(TemplateBinder.class);
    HostProviders hostProviders = mock(HostProviders.class);
    pietManager.adapterParameters =
        new AdapterParameters(
            context,
            Suppliers.of(viewGroup1),
            hostProviders,
            null,
            mockFactory,
            mockTemplateBinder,
            new FakeClock());
    pietManager.purgeRecyclerPools();
    verify(mockFactory).purgeRecyclerPools();
  }
}
