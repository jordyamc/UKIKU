package com.connectsdk.service;

import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Created by Oleksii Frolov on 3/19/2015.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AirPlayServiceTest {

    private StubAirPlayService service;

    class StubAirPlayService extends AirPlayService {

        private Object response;

        public StubAirPlayService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) throws IOException {
            super(serviceDescription, serviceConfig);
        }

        public void setResponse(Object response) {
            this.response = response;
        }

        @Override
        public void sendCommand(ServiceCommand<?> serviceCommand) {
            serviceCommand.getResponseListener().onSuccess(response);
        }

    }

    @Before
    public void setUp() throws IOException {
        service = new StubAirPlayService(Mockito.mock(ServiceDescription.class), Mockito.mock(ServiceConfig.class));
    }

    @Test
    public void testGetPlayStateFinished() throws InterruptedException {
        service.setResponse(
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">" +
                "<plist version=\"1.0\">" +
                "<dict>" +
                "</dict>" +
                "</plist>"
        );
        service.getPlayState(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                Assert.assertEquals(MediaControl.PlayStateStatus.Finished, object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testGetPlayStatePlaying() throws InterruptedException {
        service.setResponse(
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">" +
                "<plist version=\"1.0\">" +
                "<dict>" +
                "<key>rate</key>" +
                "<real>1</real>" +
                "</dict>" +
                "</plist>"
        );
        service.getPlayState(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                Assert.assertEquals(MediaControl.PlayStateStatus.Playing, object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testDigestAuthentication() {
        Assert.assertEquals(null, service.digestAuthentication(null));
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", service.digestAuthentication(""));
        Assert.assertEquals("202cb962ac59075b964b07152d234b70", service.digestAuthentication("123"));
        Assert.assertEquals("7b613f0aafa3e72b11d5e08c8c51f03f", service.digestAuthentication("526b828b08a7b3e36498d2ecec4b5e49"));
    }

    @Test
    public void testGetAuthenticate() {
        // Assume that a password is AirPlay
        service.password = "AirPlay";
        Assert.assertEquals("Digest username=\"AirPlay\", realm=\"AirPlay\", nonce=\"MTMzMTMwODI0MCDEJP5Jo7HFo81rbAcKNKw2\", uri=\"/play\", response=\"85c25341d6e62d402f6600340fc44ce0\"",
                service.getAuthenticate("Digest", "/play", "Digest realm=\"AirPlay\", nonce=\"MTMzMTMwODI0MCDEJP5Jo7HFo81rbAcKNKw2\""));
    }

    @Test
    public void testInitialPairingType() {
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, service.getPairingType());
    }

    @Test
    public void testPairingTypeSetter() {
        service.setPairingType(DeviceService.PairingType.PIN_CODE);
        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, service.getPairingType());
    }

    @Test
    public void testGetDuration() {
        service.setResponse(
                "duration: 83.124794\n" +
                "position: 14.467000");
        service.getDuration(new MediaControl.DurationListener() {
            @Override
            public void onSuccess(Long duration) {
                Assert.assertEquals(83000, duration.longValue());
            }

            @Override
            public void onError(ServiceCommandError error) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testGetDurationWithComma() {
        service.setResponse(
                "duration: 83,124794\n" +
                "position: 14,467000");
        service.getDuration(new MediaControl.DurationListener() {
            @Override
            public void onSuccess(Long duration) {
                Assert.assertEquals(0, duration.longValue());
            }

            @Override
            public void onError(ServiceCommandError error) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testGetDurationWithWrongData() {
        service.setResponse("zxcmnb");
        service.getDuration(new MediaControl.DurationListener() {
            @Override
            public void onSuccess(Long duration) {
                Assert.assertEquals(0, duration.longValue());
            }

            @Override
            public void onError(ServiceCommandError error) {
                Assert.fail();
            }
        });
    }

}
