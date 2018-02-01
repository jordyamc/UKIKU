package com.connectsdk.service.upnp;

import com.connectsdk.service.command.URLServiceSubscription;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by oleksii on 4/27/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class DLNAHttpServerTest {

    DLNAHttpServer server;

    @Before
    public void setUp() {
        server = new DLNAHttpServer();
    }

    @Test
    public void testUnsubscribeOnDisconnect() {
        URLServiceSubscription<?> subscribtion = Mockito.mock(URLServiceSubscription.class);
        server.getSubscriptions().add(subscribtion);
        server.running = true;

        server.stop();
        Mockito.verify(subscribtion).unsubscribe();
    }

}
