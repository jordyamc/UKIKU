package com.connectsdk.service;

import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceCommandError;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NetCastTVServiceTest {

    private NetcastTVService service;

    private ServiceDescription serviceDescription;

    private ServiceConfig serviceConfig;

    @Before
    public void setUp() {
        serviceDescription = Mockito.mock(ServiceDescription.class);
        Mockito.when(serviceDescription.getModelNumber()).thenReturn("4.0");
        serviceConfig = Mockito.mock(ServiceConfig.class);
        service = new NetcastTVService(serviceDescription, serviceConfig);
    }

    @Test
    public void testDecToHex() {
        Assert.assertEquals("0000000000000010", service.decToHex("16"));
    }

    @Test
    public void testDecToHexWithNullArgument() {
        Assert.assertEquals(null, service.decToHex(null));
    }

    @Test
    public void testDecToHexWithEmptyArgument() {
        Assert.assertEquals(null, service.decToHex(""));
    }

    @Test
    public void testDecToHexWithWrongArgument() {
        Assert.assertEquals(null, service.decToHex("Not a number"));
    }

    @Test
    public void testDecToHexWithWrongCharactersArgument() {
        Assert.assertEquals("0000000000000010", service.decToHex(" 16\r\n"));
    }

    @Test
    public void testServiceShouldHasSubtitleCapabilityWhenPairingLevelOn() {
        setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        Assert.assertTrue(service.hasCapability(MediaPlayer.Subtitle_SRT));
    }

    @Test
    public void testServiceShouldHasSubtitleCapabilityWhenPairingLevelOff() {
        setPairingLevel(DiscoveryManager.PairingLevel.ON);
        Assert.assertTrue(service.hasCapability(MediaPlayer.Subtitle_SRT));
    }

    @Test
    public void testShouldNotContainRewindCapabilityWhenPairingLevelOff() {
        setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        Assert.assertFalse(service.hasCapability(MediaControl.Rewind));
    }

    @Test
    public void testShouldNotContainRewindCapabilityWhenPairingLevelOn() {
        setPairingLevel(DiscoveryManager.PairingLevel.ON);
        Assert.assertFalse(service.hasCapability(MediaControl.Rewind));
    }

    @Test
    public void testShouldNotContainFastForwardCapabilityWhenPairingLevelOff() {
        setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        Assert.assertFalse(service.hasCapability(MediaControl.FastForward));
    }

    @Test
    public void testShouldNotContainFastForwardCapabilityWhenPairingLevelOn() {
        setPairingLevel(DiscoveryManager.PairingLevel.ON);
        Assert.assertFalse(service.hasCapability(MediaControl.FastForward));
    }

    @Test
    public void testRewindShouldSendNotSupportedError() {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        service.rewind(listener);
        verifyNotImplemented(listener);
    }

    @Test
    public void testFastForwardShouldSendNotSupportedError() {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        service.fastForward(listener);
        verifyNotImplemented(listener);
    }

    @Test
    public void testMediaPlayerPriorityShouldBeHigh() {
        Assert.assertEquals(CapabilityMethods.CapabilityPriorityLevel.HIGH,
                service.getMediaPlayerCapabilityLevel());
    }

    @Test
    public void testMediaControlPriorityShouldBeHigh() {
        Assert.assertEquals(CapabilityMethods.CapabilityPriorityLevel.HIGH,
                service.getMediaControlCapabilityLevel());
    }

    private void verifyNotImplemented(ResponseListener<Object> listener) {
        ArgumentCaptor<ServiceCommandError> argError
                = ArgumentCaptor.forClass(ServiceCommandError.class);
        Mockito.verify(listener).onError(argError.capture());
        Assert.assertTrue(argError.getValue() instanceof NotSupportedServiceCommandError);
    }

    private void setPairingLevel(DiscoveryManager.PairingLevel level) {
        DiscoveryManager.init(Robolectric.application);
        DiscoveryManager.getInstance().setPairingLevel(level);
    }

}
