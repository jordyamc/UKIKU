/*
 * DIALServiceTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 06 Aug 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service;

import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class DIALServiceTest {

    private static final String APPLICATION_URL = "http://applicationurl";

    private DIALService service;
    private ServiceDescription serviceDescription;
    private ServiceConfig serviceConfig;
    private ServiceCommand.ServiceCommandProcessor commandProcessor;

    @Before
    public void setUp() {
        serviceDescription = Mockito.mock(ServiceDescription.class);
        Mockito.when(serviceDescription.getApplicationURL()).thenReturn(APPLICATION_URL);
        serviceConfig = Mockito.mock(ServiceConfig.class);
        commandProcessor = Mockito.mock(ServiceCommand.ServiceCommandProcessor.class);
        service = new DIALService(serviceDescription, serviceConfig);
        service.setCommandProcessor(commandProcessor);
    }

    @Test
    public void testLaunchNetflixWithContentParameter() {
        Launcher.AppLaunchListener listener = Mockito.mock(Launcher.AppLaunchListener.class);
        String content = "123";
        String expectedPayload = "{\"v\":\""+content+"\"}";

        service.launchNetflix(content, listener);

        verifyNetflixCommand(expectedPayload);
    }

    @Test
    public void testLaunchNetflixWithoutContentParameter() {
        service.launchNetflix(null, Mockito.mock(Launcher.AppLaunchListener.class));

        verifyNetflixCommand(null);
    }

    @Test
    public void testLaunchNetflixWithEmptyContentParameter() {
        service.launchNetflix("", Mockito.mock(Launcher.AppLaunchListener.class));

        verifyNetflixCommand(null);
    }

    private void verifyNetflixCommand(String expectedPayload) {
        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(commandProcessor).sendCommand(argCommand.capture());
        ServiceCommand command = argCommand.getValue();

        Assert.assertEquals(APPLICATION_URL + "/Netflix", command.getTarget());
        Assert.assertEquals(ServiceCommand.TYPE_POST, command.getHttpMethod());
        Assert.assertSame(commandProcessor, command.getCommandProcessor());
        if (expectedPayload != null) {
            Assert.assertEquals(expectedPayload, command.getPayload().toString());
        } else {
            Assert.assertNull(command.getPayload());
        }
    }

}
