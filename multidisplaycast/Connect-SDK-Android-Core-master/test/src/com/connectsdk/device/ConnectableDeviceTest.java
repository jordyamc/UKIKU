package com.connectsdk.device;

import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.AirPlayService;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.NetcastTVService;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ConnectableDeviceTest {

    private ConnectableDevice device;

    @Before
    public void setUp() {
        DiscoveryManager.init(Robolectric.application);
        device = new ConnectableDevice();
    }

    @Test
    public void testHasCapabilityWithEmptyServices() {
        Assert.assertFalse(device.hasCapability(MediaPlayer.Display_Image));
    }

    @Test
    public void testHasCapabilityWithServices() {
        DeviceService service = Mockito.mock(DeviceService.class);
        Mockito.when(service.hasCapability(MediaPlayer.Display_Image)).thenReturn(Boolean.TRUE);
        device.services.put("service", service);
        Assert.assertTrue(device.hasCapability(MediaPlayer.Display_Image));
    }

    @Test
    public void testHasAnyCapabilities() {
        DeviceService service = Mockito.mock(DeviceService.class);
        String[] capabilities = {Launcher.Browser, Launcher.YouTube};
        Mockito.when(service.hasAnyCapability(capabilities)).thenReturn(Boolean.TRUE);
        device.services.put("service", service);
        Assert.assertTrue(device.hasAnyCapability(capabilities));
    }

    @Test
    public void testHasAnyCapabilitiesWithoutServices() {
        DeviceService service = Mockito.mock(DeviceService.class);
        String[] capabilities = {Launcher.Browser, Launcher.YouTube};
        Mockito.when(service.hasAnyCapability(capabilities)).thenReturn(Boolean.FALSE);
        device.services.put("service", service);
        Assert.assertFalse(device.hasAnyCapability(capabilities));
    }

    @Test
    public void testHasCapabilities() {
        DeviceService service = Mockito.mock(DeviceService.class);
        Mockito.when(service.hasCapability(Launcher.Browser)).thenReturn(Boolean.TRUE);
        Mockito.when(service.hasCapability(Launcher.YouTube)).thenReturn(Boolean.TRUE);
        device.services.put("service", service);

        List<String> capabilities = new ArrayList<String>();
        capabilities.add(Launcher.Browser);
        capabilities.add(Launcher.YouTube);

        Assert.assertTrue(device.hasCapabilities(capabilities));
    }

    @Test
    public void testSetPromptPairingType() throws IOException {
        // given
        addAllCoreServicesToDevice();

        // when
        device.setPairingType(DeviceService.PairingType.FIRST_SCREEN);

        // then
        Assert.assertEquals(DeviceService.PairingType.FIRST_SCREEN, device.getServiceByName(WebOSTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(NetcastTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID).getPairingType());
    }

    @Test
    public void testSetPinPairingType() throws IOException {
        // given
        addAllCoreServicesToDevice();

        // when
        device.setPairingType(DeviceService.PairingType.PIN_CODE);

        // then
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(WebOSTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(NetcastTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID).getPairingType());
    }

    @Test
    public void testNonePairingType() throws IOException {
        // given
        addAllCoreServicesToDevice();

        // when
        device.setPairingType(DeviceService.PairingType.NONE);

        // then
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(WebOSTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(NetcastTVService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID).getPairingType());
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID).getPairingType());
    }

    private void addAllCoreServicesToDevice() throws IOException {
        DeviceService webOSService = new WebOSTVService(createServiceDescription(WebOSTVService.ID), Mockito.mock(ServiceConfig.class));
        DeviceService netCastService = new NetcastTVService(createServiceDescription(NetcastTVService.ID), Mockito.mock(ServiceConfig.class));
        DeviceService dialService = new DIALService(createServiceDescription(DIALService.ID), Mockito.mock(ServiceConfig.class));
        DeviceService dlnaSrevice = new DLNAService(createServiceDescription(DLNAService.ID), Mockito.mock(ServiceConfig.class));
        DeviceService rokuService = new RokuService(createServiceDescription(RokuService.ID), Mockito.mock(ServiceConfig.class));
        DeviceService airPlayService = new AirPlayService(createServiceDescription(AirPlayService.ID), Mockito.mock(ServiceConfig.class));
        device.services.put(WebOSTVService.ID, webOSService);
        device.services.put(NetcastTVService.ID, netCastService);
        device.services.put(DIALService.ID, dialService);
        device.services.put(DLNAService.ID, dlnaSrevice);
        device.services.put(RokuService.ID, rokuService);
        device.services.put(AirPlayService.ID, airPlayService);
    }

    private ServiceDescription createServiceDescription(String serviceId) {
        ServiceDescription description = new ServiceDescription();
        description.setFriendlyName("");
        description.setManufacturer("");
        description.setUUID("");
        description.setModelDescription("");
        description.setModelName("");
        description.setModelNumber("");
        description.setServiceID(serviceId);
        return description;
    }

}
