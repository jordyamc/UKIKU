package com.connectsdk.discovery;

import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.DLNAService;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Objects;

/**
 * Created by oleksii.frolov on 2/16/2015.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class DiscoveryManagerTest {
    
    DiscoveryManager discovery;
    
    @Before
    public void setUp() {
        discovery = new DiscoveryManager(Robolectric.application);
    }
    
    @Test
    public void testUnregisterDeviceServiceWithWrongArguments() {
        discovery.deviceClasses.put("service", DIALService.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());
        
        discovery.unregisterDeviceService(Objects.class, Object.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());
        
        discovery.unregisterDeviceService(DLNAService.class, SSDPDiscoveryProvider.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());

        discovery.unregisterDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());
    }

    @Test
    public void testUnregisterDeviceServiceWithWrongProvider() {
        discovery.discoveryProviders.add(new SSDPDiscoveryProvider(Robolectric.application));
        discovery.deviceClasses.put(DIALService.ID, DIALService.class);
        Assert.assertEquals(1, discovery.discoveryProviders.size());
        Assert.assertEquals(1, discovery.deviceClasses.size());

        discovery.unregisterDeviceService(DIALService.class, ZeroconfDiscoveryProvider.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());
        Assert.assertEquals(1, discovery.discoveryProviders.size());
    }

    @Test
    public void testUnregisterDeviceServiceWithWrongServiceID() {
        discovery.discoveryProviders.add(new SSDPDiscoveryProvider(Robolectric.application));
        discovery.deviceClasses.put(DLNAService.ID, DIALService.class);
        Assert.assertEquals(1, discovery.discoveryProviders.size());
        Assert.assertEquals(1, discovery.deviceClasses.size());

        discovery.unregisterDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
        Assert.assertEquals(1, discovery.deviceClasses.size());
        Assert.assertEquals(1, discovery.discoveryProviders.size());
    }
    
    @Test
    public void testUnregisterDeviceService() {
        discovery.discoveryProviders.add(new SSDPDiscoveryProvider(Robolectric.application));
        discovery.deviceClasses.put(DIALService.ID, DIALService.class);
        Assert.assertEquals(1, discovery.discoveryProviders.size());
        Assert.assertEquals(1, discovery.deviceClasses.size());

        discovery.unregisterDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
        Assert.assertEquals(0, discovery.deviceClasses.size());
        Assert.assertEquals(0, discovery.discoveryProviders.size());
    }
    
    @Test
    public void testRegisterDeviceService() {
        Assert.assertEquals(0, discovery.discoveryProviders.size());
        Assert.assertEquals(0, discovery.deviceClasses.size());
        
        discovery.registerDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
        Assert.assertEquals(1, discovery.discoveryProviders.size());
        Assert.assertEquals(1, discovery.deviceClasses.size());
    }

    @Test
    public void testRegisterDeviceServiceWithNullValues() {
        Assert.assertEquals(0, discovery.discoveryProviders.size());
        Assert.assertEquals(0, discovery.deviceClasses.size());

        try {
            discovery.registerDeviceService(null, SSDPDiscoveryProvider.class);
            Assert.fail("NullPointerException must be casted");
        } catch (NullPointerException e) {}

        try {
            discovery.registerDeviceService(DLNAService.class, null);
            Assert.fail("NullPointerException must be casted");
        } catch (NullPointerException e) {}

        Assert.assertEquals(0, discovery.discoveryProviders.size());
        Assert.assertEquals(0, discovery.deviceClasses.size());
    }
}
