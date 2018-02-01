/*
 * WebOSTVServiceTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 27 May 2015
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

import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceCommandError;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebOSWebAppSession;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class WebOSTVServiceTest {

    private WebOSTVService service;

    private ServiceDescription serviceDescription;

    private WebOSTVServiceSocketClient socket;

    @Before
    public void setUp() {
        serviceDescription = Mockito.mock(ServiceDescription.class);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        Mockito.when(serviceDescription.getVersion()).thenReturn("5.0.0");
        Mockito.when(serviceDescription.getResponseHeaders()).thenReturn(headers);
        service = new WebOSTVService(serviceDescription, Mockito.mock(ServiceConfig.class));
        this.socket = Mockito.mock(WebOSTVServiceSocketClient.class);
        service.socket = this.socket;
        Mockito.when(socket.isConnected()).thenReturn(Boolean.TRUE);
    }

    @After
    public void tearDown() {
        DiscoveryManager.getInstance().getAllDevices().clear();
    }

    @Test
    public void testCapabilitiesShouldContainToastControlWhenPairingOn() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        service.updateCapabilities();

        Assert.assertTrue(service.hasCapabilities(ToastControl.Capabilities));
    }

    @Test
    public void testCapabilitiesShouldNotContainToastControlWhenPairingOff() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        service.updateCapabilities();
        
        Assert.assertFalse(service.hasCapabilities(ToastControl.Capabilities));
    }

    @Test
    public void testCapabilitiesShouldContainSubtitlesForWebOsWithWebAppSupport() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        setWebOSVersion("5.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldContainOnlySrtSubtitlesForWebOsWithDLNA() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        injectDLNAService();
        setWebOSVersion("4.0.0");

        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldNotContainSubtitlesForWebOsV4() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        setWebOSVersion("4.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldContainSubtitlesForWebOsWithWebAppSupportAndPairingOn() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        setWebOSVersion("5.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldContainOnlySrtSubtitlesForWebOsWithDLNAAndPairingOn() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        injectDLNAService();
        setWebOSVersion("4.0.0");

        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldNotContainSubtitlesForWebOsV4AndPairingOn() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        setWebOSVersion("4.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldContainSubtitlesForWebOsWithWebAppSupportAndDLNA() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.OFF);
        injectDLNAService();
        setWebOSVersion("5.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testCapabilitiesShouldContainSubtitlesForWebOsWithWebAppSupportAndDLNAPairingOn() {
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        injectDLNAService();
        setWebOSVersion("5.0.0");

        Assert.assertFalse(service.hasCapabilities(MediaPlayer.Subtitle_SRT));
        Assert.assertTrue(service.hasCapabilities(MediaPlayer.Subtitle_WebVTT));
    }

    @Test
    public void testJumpToTrack() {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        service.jumpToTrack(1, listener);
        Mockito.verify(listener).onError(Mockito.isA(NotSupportedServiceCommandError.class));
    }

    @Test
    public void testNext() {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        service.next(listener);
        Mockito.verify(listener).onError(Mockito.isA(NotSupportedServiceCommandError.class));
    }

    @Test
    public void testPrevious() {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        service.previous(listener);
        Mockito.verify(listener).onError(Mockito.isA(NotSupportedServiceCommandError.class));
    }

    @Test
    public void testLaunchInputPickerForOldTV() throws JSONException {
        Launcher.AppLaunchListener listener = Mockito.mock(Launcher.AppLaunchListener.class);
        service.launchInputPicker(listener);

        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket).sendCommand(argCommand.capture());
        ServiceCommand command = argCommand.getValue();
        command.getResponseListener().onSuccess(new JSONObject());

        Mockito.verify(listener).onSuccess(Mockito.any(LaunchSession.class));
        JSONObject payload = (JSONObject)command.getPayload();
        Assert.assertEquals("com.webos.app.inputpicker", payload.getString("id"));
    }

    @Test
    public void testLaunchInputPickerForNewTV() throws JSONException {
        Launcher.AppLaunchListener listener = Mockito.mock(Launcher.AppLaunchListener.class);
        service.launchInputPicker(listener);

        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket).sendCommand(argCommand.capture());

        ServiceCommand command = argCommand.getValue();
        command.getResponseListener().onError(new ServiceCommandError());

        argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket, Mockito.times(2)).sendCommand(argCommand.capture());
        command = argCommand.getValue();
        command.getResponseListener().onSuccess(new JSONObject());

        Mockito.verify(listener).onSuccess(Mockito.any(LaunchSession.class));
        JSONObject payload = (JSONObject)command.getPayload();
        Assert.assertEquals("com.webos.app.inputmgr", payload.getString("id"));
    }

    @Test
    public void testLaunchInputPickerForNewTVFailure() throws JSONException {
        Launcher.AppLaunchListener listener = Mockito.mock(Launcher.AppLaunchListener.class);
        service.launchInputPicker(listener);

        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket).sendCommand(argCommand.capture());
        ServiceCommand command = argCommand.getValue();
        command.getResponseListener().onError(new ServiceCommandError());

        argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket, Mockito.times(2)).sendCommand(argCommand.capture());
        command = argCommand.getValue();
        command.getResponseListener().onError(new ServiceCommandError());

        Mockito.verify(listener).onError(Mockito.any(ServiceCommandError.class));
    }

    @Test
    public void testPlayMediaDeprecatedWithRequiredParametersOnTheLatestWebOS() {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        WebOSWebAppSession webAppSession = Mockito.mock(WebOSWebAppSession.class);
        service.mWebAppSessions.put("MediaPlayer", webAppSession);

        MediaInfo mediaInfo = new MediaInfo.Builder("url", "mimetype").build();
        boolean shouldLoop = true;
        service.playMedia(mediaInfo.getUrl(), mediaInfo.getMimeType(), null, null, null,
                shouldLoop, listener);

        verifyPlayMediaOnTheLatestWebOS(mediaInfo, shouldLoop, listener, webAppSession);
    }

    @Test
    public void testPlayMediaDeprecatedWithAllParametersOnTheLatestWebOS() {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        WebOSWebAppSession webAppSession = Mockito.mock(WebOSWebAppSession.class);
        service.mWebAppSessions.put("MediaPlayer", webAppSession);

        MediaInfo mediaInfo = new MediaInfo.Builder("url", "mimetype")
                .setTitle("title")
                .setDescription("description")
                .setIcon("icon")
                .build();
        boolean shouldLoop = true;
        service.playMedia(mediaInfo.getUrl(), mediaInfo.getMimeType(), mediaInfo.getTitle(),
                mediaInfo.getDescription(), mediaInfo.getImages().get(0).getUrl(), shouldLoop,
                listener);

        verifyPlayMediaOnTheLatestWebOS(mediaInfo, shouldLoop, listener, webAppSession);
    }

    @Test
    public void testPlayMediaWithRequiredParametersOnTheLatestWebOS() {
        MediaInfo mediaInfo = createBasicMediaInfo();
        callAndVerifyPlayMediaOnTheLatestWebOS(mediaInfo, false);
    }

    @Test
    public void testPlayMediaWithSubtitlesOnTheLatestWebOS() {
        MediaInfo mediaInfo = createMediaInfoWithSubtitles();
        callAndVerifyPlayMediaOnTheLatestWebOS(mediaInfo, true);
    }

    @Test
    public void testPlayMediaWithRequiredParametersOnTheWebOSV4AndDlna() {
        MediaInfo mediaInfo = createBasicMediaInfo();
        verifyPlayMediaOnTheWebOSV4(mediaInfo, false);
    }

    @Test
    public void testPlayMediaWithSubtitlesOnTheWebOSV4AndDlna() {
        MediaInfo mediaInfo = createMediaInfoWithSubtitles();
        verifyPlayMediaOnTheWebOSV4(mediaInfo, false);
    }

    @Test
    public void testPlayMediaWithRequiredParametersOnTheWebOSV4WithoutDlna() throws JSONException {
        MediaInfo mediaInfo = createMediaInfoWithSubtitles();
        verifyPlayMediaOnTheWebOSV4WithoutDlna(mediaInfo, false);
    }

    @Test
    public void testSetChannelWithIdArgument() throws JSONException {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setId("id");

        JSONObject payload = verifySetChannel(channelInfo);
        Assert.assertEquals("id", payload.getString("channelId"));
        Assert.assertFalse(payload.has("channelNumber"));
    }

    @Test
    public void testSetChannelWithNumberArgument() throws JSONException {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setNumber("number");

        JSONObject payload = verifySetChannel(channelInfo);
        Assert.assertEquals("number", payload.getString("channelNumber"));
        Assert.assertFalse(payload.has("channelId"));
    }

    @Test
    public void testSetChannelWithIdAndNumberArguments() throws JSONException {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setNumber("number");
        channelInfo.setId("id");

        JSONObject payload = verifySetChannel(channelInfo);
        Assert.assertEquals("number", payload.getString("channelNumber"));
        Assert.assertEquals("id", payload.getString("channelId"));
    }

    @Test
    public void testSetChannelWithEmptyArguments() throws JSONException {
        ChannelInfo channelInfo = new ChannelInfo();

        JSONObject payload = verifySetChannel(channelInfo);
        Assert.assertEquals(0, payload.length());
    }

    @Test(expected = NullPointerException.class)
    public void testSetChannelWithNullChannelInfo() throws JSONException {
        JSONObject payload = verifySetChannel(null);
        Assert.assertEquals(0, payload.length());
    }

    private JSONObject verifySetChannel(ChannelInfo channelInfo) {
        ResponseListener<Object> response = Mockito.mock(ResponseListener.class);
        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);

        service.setChannel(channelInfo, response);
        Mockito.verify(socket).sendCommand(argCommand.capture());
        ServiceCommand command = argCommand.getValue();
        JSONObject payload = (JSONObject) command.getPayload();
        Assert.assertEquals("ssap://tv/openChannel", command.getTarget());
        return payload;
    }


    private MediaInfo createBasicMediaInfo() {
        return new MediaInfo.Builder("http://media", "video/mp4").build();
    }

    private MediaInfo createMediaInfoWithSubtitles() {
        SubtitleInfo subtitle = new SubtitleInfo.Builder("http://subtitle")
                .build();

        return new MediaInfo.Builder("http://media", "video/mp4")
                .setTitle("title")
                .setDescription("description")
                .setIcon("icon")
                .setSubtitleInfo(subtitle)
                .build();
    }

    private void verifyPlayMediaOnTheWebOSV4WithoutDlna(MediaInfo mediaInfo, boolean shouldLoop) throws JSONException {
        Mockito.when(serviceDescription.getVersion()).thenReturn("4.0.0");
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);

        service.playMedia(mediaInfo, shouldLoop, listener);

        ArgumentCaptor<ServiceCommand> argCommand = ArgumentCaptor.forClass(ServiceCommand.class);
        Mockito.verify(socket).sendCommand(argCommand.capture());

        ServiceCommand command = argCommand.getValue();
        JSONObject payload = (JSONObject) command.getPayload();

        Assert.assertEquals("ssap://media.viewer/open", command.getTarget());
        Assert.assertEquals(mediaInfo.getUrl(), payload.getString("target"));
        Assert.assertEquals(mediaInfo.getTitle(), payload.getString("title"));
        Assert.assertEquals(mediaInfo.getDescription(), payload.getString("description"));
        Assert.assertEquals(mediaInfo.getMimeType(), payload.getString("mimeType"));
        Assert.assertEquals(shouldLoop, payload.getBoolean("loop"));

        Assert.assertEquals(mediaInfo.getImages().get(0).getUrl(), payload.getString("iconSrc"));
    }

    private void verifyPlayMediaOnTheWebOSV4(MediaInfo mediaInfo, boolean shouldLoop) {
        Mockito.when(serviceDescription.getVersion()).thenReturn("4.0.0");
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);

        DLNAService dlnaService = injectDLNAService();
        MediaPlayer mediaPlayer = Mockito.mock(MediaPlayer.class);
        Mockito.when(dlnaService.getAPI(MediaPlayer.class)).thenReturn(mediaPlayer);

        service.playMedia(mediaInfo, shouldLoop, listener);

        ArgumentCaptor<MediaInfo> argMediaInfo = ArgumentCaptor.forClass(MediaInfo.class);
        ArgumentCaptor<Boolean> argShouldLoop = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(mediaPlayer).playMedia(argMediaInfo.capture(), argShouldLoop.capture(),
                Mockito.same(listener));

        MediaInfo capturedMediaInfo = argMediaInfo.getValue();
        Assert.assertEquals(mediaInfo.getDescription(), capturedMediaInfo.getDescription());
        Assert.assertEquals(mediaInfo.getMimeType(), capturedMediaInfo.getMimeType());
        Assert.assertEquals(mediaInfo.getTitle(), capturedMediaInfo.getTitle());
        Assert.assertEquals(mediaInfo.getUrl(), capturedMediaInfo.getUrl());
        Assert.assertEquals(mediaInfo.getImages(), capturedMediaInfo.getImages());
        Assert.assertEquals(mediaInfo.getSubtitleInfo(), capturedMediaInfo.getSubtitleInfo());
        Assert.assertEquals(shouldLoop, argShouldLoop.getValue().booleanValue());
    }

    private void callAndVerifyPlayMediaOnTheLatestWebOS(final MediaInfo mediaInfo, boolean shouldLoop) {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        WebOSWebAppSession webAppSession = Mockito.mock(WebOSWebAppSession.class);
        service.mWebAppSessions.put("MediaPlayer", webAppSession);

        service.playMedia(mediaInfo, shouldLoop, listener);
        verifyPlayMediaOnTheLatestWebOS(mediaInfo, shouldLoop, listener, webAppSession);
    }

    private void verifyPlayMediaOnTheLatestWebOS(MediaInfo mediaInfo, boolean shouldLoop, MediaPlayer.LaunchListener listener, WebOSWebAppSession webAppSession) {
        // should try to join to the web app
        ArgumentCaptor<ResponseListener> argListener = ArgumentCaptor.forClass(ResponseListener
                .class);
        Mockito.verify(webAppSession).join(argListener.capture());

        // run join success
        ResponseListener webAppListener = argListener.getValue();
        webAppListener.onSuccess(null);
        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        // should delegate playing media to the WebAppSession
        ArgumentCaptor<MediaInfo> argMediaInfo = ArgumentCaptor.forClass(MediaInfo.class);
        ArgumentCaptor<Boolean> argShouldLoop = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(webAppSession).playMedia(argMediaInfo.capture(), argShouldLoop.capture(),
                Mockito.same(listener));

        MediaInfo capturedMediaInfo = argMediaInfo.getValue();
        Assert.assertEquals(mediaInfo.getDescription(), capturedMediaInfo.getDescription());
        Assert.assertEquals(mediaInfo.getMimeType(), capturedMediaInfo.getMimeType());
        Assert.assertEquals(mediaInfo.getTitle(), capturedMediaInfo.getTitle());
        Assert.assertEquals(mediaInfo.getUrl(), capturedMediaInfo.getUrl());
        Assert.assertEquals(mediaInfo.getImages(), capturedMediaInfo.getImages());
        Assert.assertEquals(mediaInfo.getSubtitleInfo(), capturedMediaInfo.getSubtitleInfo());
        Assert.assertEquals(shouldLoop, argShouldLoop.getValue().booleanValue());
    }

    private DLNAService injectDLNAService() {
        DLNAService dlnaService = Mockito.mock(DLNAService.class);
        ConnectableDevice dlnaDevice = Mockito.mock(ConnectableDevice.class);
        Mockito.when(dlnaDevice.getServiceByName(DLNAService.ID)).thenReturn(dlnaService);
        String ipAddress = "127.0.0.1";
        DiscoveryManager.getInstance().getAllDevices().put(ipAddress, dlnaDevice);
        Mockito.when(serviceDescription.getIpAddress()).thenReturn(ipAddress);
        return dlnaService;
    }

    private void setWebOSVersion(String version) {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Server", Arrays.asList("server/" + version + " server"));
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setIpAddress("127.0.0.1");
        serviceDescription.setResponseHeaders(headers);
        service.setServiceDescription(serviceDescription);
    }
}
