package com.connectsdk.discovery.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.JmDNSImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.shadow.WifiInfoShadow;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows = { WifiInfoShadow.class })
public class ZeroConfDiscoveryPrividerTest {

    private ZeroconfDiscoveryProvider dp;
    private JmDNS mDNS;
    private ServiceEvent eventMock;

    // stub classes are used to allow mocking inside Robolectric test
    class StubJmDNS extends JmDNSImpl {

        public StubJmDNS() throws IOException {
            this(null, null);
        }

        public StubJmDNS(InetAddress address, String name) throws IOException {
            super(address, name);
        }
    }

    abstract class StubServiceEvent extends ServiceEvent {

        public StubServiceEvent(Object eventSource) {
            super(eventSource);
        }
    }

    abstract class StubServiceInfo extends javax.jmdns.ServiceInfo {}

    class StubZeroConfDiscoveryProvider extends ZeroconfDiscoveryProvider {

        public StubZeroConfDiscoveryProvider(Context context) {
            super(context);
        }

        @Override
        protected JmDNS createJmDNS() {
            return mDNS;
        }
    }

    @Before
    public void setUp() {
        dp = new StubZeroConfDiscoveryProvider(Robolectric.application);
        mDNS = mock(StubJmDNS.class);
        eventMock = mock(StubServiceEvent.class);

        dp.jmdns = mDNS;
    }

    @Test
    public void testStartShouldAddDeviceFilter() throws Exception {
        DiscoveryFilter filter = new DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.");

        dp.addDeviceFilter(filter);
        dp.start();

        Thread.sleep(500);
        verify(mDNS).addServiceListener(filter.getServiceFilter(), dp.jmdnsListener);
    }

    @Test
    public void testStartShouldCancelPreviousSearch() throws Exception {
        DiscoveryFilter filter = new DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.");

        dp.addDeviceFilter(filter);
        dp.stop();

        verify(mDNS).removeServiceListener(filter.getServiceFilter(), dp.jmdnsListener);
    }

    @Test
    public void testJmdnsServiceAdded() throws Exception {
        // Test Desc.: Verify when service added to the JmdnsServiceListener
        // then "service information is queried from registry with injected
        // event mock object.

        dp.jmdnsListener.serviceAdded(eventMock);
        dp.start();

        verify(eventMock, atLeastOnce()).getType();
        verify(eventMock, atLeastOnce()).getName();
        verify(mDNS, timeout(100)).requestServiceInfo(eventMock.getType(),
                eventMock.getName(), 1);
    }

    @Test
    public void testAddListener() throws Exception {
        // Test Desc.: Verify ZeroConfDiscoveryProvider addListener - Adds a
        // DiscoveryProviderListener instance which is the DiscoveryManager Impl
        // to ServiceListeners List.

        DiscoveryManager listenerMock = mock(DiscoveryManager.class);

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock));
        dp.addListener(listenerMock);

        Assert.assertTrue(dp.serviceListeners.contains(listenerMock));
    }

    @Test
    public void testRemoveListener() throws Exception {
        // Test Desc.: Verify ZeroConfDiscoveryProvider RemoveListener - Removes
        // a DiscoveryProviderListener instance which is the DiscoveryManager
        // Impl from ServiceListeners List.

        DiscoveryManager listenerMock = mock(DiscoveryManager.class);

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock));
        dp.serviceListeners.add(listenerMock);
        Assert.assertTrue(dp.serviceListeners.contains(listenerMock));

        dp.removeListener(listenerMock);

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock));
    }

    @Test
    public void testFiltersAreEmptyByDefault() throws Exception {
        // Test Desc.: Verify if the serviceFilters is empty prior to calling
        // the scheduled timer task start() which adds the searchTarget as
        // filter into the ServiceFilters.

        DiscoveryFilter filter = new DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.");
        Assert.assertTrue(dp.isEmpty());

        dp.serviceFilters.add(filter);

        Assert.assertFalse(dp.isEmpty());
    }

    @Test
    public void testStopZeroConfService() throws Exception {
        // Test Desc. : Verify if on stop() of ZeroConfDiscoveryProvider
        // Service, implicitly invoke the removeServiceListener() on JmDns
        // instance,

        DiscoveryFilter filter = new DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.");
        dp.serviceFilters.add(filter);

        ServiceListener listener = dp.jmdnsListener;

        verify(mDNS, Mockito.never()).removeServiceListener(
                filter.getServiceFilter(), listener);
        dp.stop();
        verify(mDNS, Mockito.times(1)).removeServiceListener(
                filter.getServiceFilter(), listener);
    }

    @Test
    public void testReset() throws Exception {
        // Test Desc. : Verify if JmdnsRegistry reset the services found for
        // ZeroConfDiscoveryProvider.

        ServiceDescription serviceDesc = new ServiceDescription();
        dp.foundServices.put("service", serviceDesc);
        Assert.assertFalse(dp.foundServices.isEmpty());

        dp.reset();
        Assert.assertTrue(dp.foundServices.isEmpty());
    }

    @Test
    public void testAddDeviceFilter() throws Exception {
        // Test Desc. : Verify if ZeroConfDiscoveryProvider. AddDeviceFilter
        // adds the specified device filter to serviceFilters list.

        DiscoveryFilter filter = new DiscoveryFilter("Test TV", "_testservicetype._tcp.local.");

        Assert.assertFalse(dp.serviceFilters.contains(filter));
        dp.addDeviceFilter(filter);

        Assert.assertTrue(dp.serviceFilters.contains(filter));
    }

    @Test
    public void testRemoveDeviceFilter() throws Exception {
        // Test Desc. : Verify if ZeroConfDiscoveryProvider. removeDeviceFilter
        // removes the entry specified device filter from to serviceFilters
        // list.

        DiscoveryFilter filter = new DiscoveryFilter("Test TV", "_testservicetype._tcp.local.");

        dp.serviceFilters.add(filter);
        Assert.assertFalse(dp.serviceFilters.isEmpty());

        dp.removeDeviceFilter(filter);

        Assert.assertTrue(dp.serviceFilters.isEmpty());
    }

    @Test
    public void testServiceIdForFilter() throws Exception {
        // Test Desc. : Verify if ZeroConfDiscoveryProvider. serviceIdForFilter
        // returns the serviceId for the specified filter added in
        // ServiceFilters list.

        DiscoveryFilter filter = new DiscoveryFilter("Test TV", "_testservicetype._tcp.local.");
        dp.serviceFilters.add(filter);

        String serviceId = dp.serviceIdForFilter(filter.getServiceFilter());

        Assert.assertEquals("Test TV", serviceId);
    }

    private ServiceEvent createMockedServiceEvent(String ip, String name) {
        ServiceEvent event = mock(StubServiceEvent.class);
        ServiceInfo info = mock(StubServiceInfo.class);
        when(event.getInfo()).thenReturn(info);
        when(info.getHostAddress()).thenReturn(ip);
        when(info.getPort()).thenReturn(7000);
        when(info.getName()).thenReturn(name);
        return event;
    }

    @Test
    public void testServiceResolveEvent() throws Exception {
        // given
        ServiceEvent event = createMockedServiceEvent("192.168.0.1", "Test TV");
        DiscoveryProviderListener listener = mock(DiscoveryProviderListener.class);
        dp.addListener(listener);

        // when
        dp.jmdnsListener.serviceResolved(event);

        // then
        verify(listener).onServiceAdded(any(DiscoveryProvider.class), any(ServiceDescription.class));
    }

    @Test
    public void testServiceResolveEventWhenThereIsFoundService() throws Exception {
        // given
        String uuid = "192.168.0.1";
        String name = "Test TV";
        ServiceDescription serviceDescription = new ServiceDescription("_testservicetype._tcp.local.", uuid, uuid);
        serviceDescription.setFriendlyName(name);
        ServiceEvent event = createMockedServiceEvent(uuid, name);
        dp.foundServices.put(uuid, serviceDescription);
        DiscoveryProviderListener listener = mock(DiscoveryProviderListener.class);
        dp.addListener(listener);

        // when
        dp.jmdnsListener.serviceResolved(event);

        // then
        verify(listener, never()).onServiceAdded(any(DiscoveryProvider.class), any(ServiceDescription.class));
    }

    @Test
    public void testServiceRemoveEvent() throws Exception {
        // given
        String uuid = "192.168.0.1";
        String name = "Test TV";
        ServiceDescription serviceDescription = new ServiceDescription("_testservicetype._tcp.local.", uuid, uuid);
        serviceDescription.setFriendlyName(name);
        ServiceEvent event = createMockedServiceEvent(uuid, name);
        DiscoveryProviderListener listener = mock(DiscoveryProviderListener.class);
        dp.addListener(listener);
        dp.foundServices.put(uuid, serviceDescription);

        // when
        dp.jmdnsListener.serviceRemoved(event);
        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        // then
        verify(listener).onServiceRemoved(any(DiscoveryProvider.class), any(ServiceDescription.class));
    }

    @Test
    public void testStateAfterConstruction() {
        Assert.assertNotNull(dp.foundServices);
        Assert.assertNotNull(dp.serviceFilters);
        Assert.assertNotNull(dp.serviceListeners);
        Assert.assertTrue(dp.foundServices.isEmpty());
        Assert.assertTrue(dp.serviceFilters.isEmpty());
        Assert.assertTrue(dp.serviceListeners.isEmpty());
        Assert.assertNotNull(dp.srcAddress);
    }
}
