/*
 * WebOSWebAppSessionTest
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
package com.connectsdk.service.sessions;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.support.annotation.NonNull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class WebOSWebAppSessionTest {

    private WebOSWebAppSession session;
    private LaunchSession launchSession;
    private DeviceService service;
    private WebOSTVServiceSocketClient socket;

    @Before
    public void setUp() {
        socket = Mockito.mock(WebOSTVServiceSocketClient.class);
        Mockito.when(socket.isConnected()).thenReturn(Boolean.TRUE);
        launchSession = Mockito.mock(LaunchSession.class);
        service = Mockito.mock(WebOSTVService.class);
        session = new WebOSWebAppSession(launchSession, service);
        session.setConnected(Boolean.TRUE);
        session.socket = socket;
        session.mFullAppId = "com.webos.app.webapphost.MediaPlayer";
    }

    @Test
    public void testPrevious() throws JSONException {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        session.previous(listener);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        ArgumentCaptor<JSONObject> argPacket = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONObject> argPayload = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.verify(socket).sendMessage(argPacket.capture(), argPayload.capture());
        Mockito.verify(listener).onSuccess(null);

        JSONObject packet = argPacket.getValue();
        JSONObject payload = argPayload.getValue();
        Assert.assertNull(payload);
        Assert.assertTrue(packet.has("payload"));
        Assert.assertEquals("playPrevious", packet.getJSONObject("payload")
                .getJSONObject("mediaCommand").getString("type"));
        Assert.assertEquals("connectsdk.mediaCommand", packet.getJSONObject("payload")
                .getString  ("contentType"));
    }

    @Test
    public void testNext() throws JSONException {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        session.next(listener);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        ArgumentCaptor<JSONObject> argPacket = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONObject> argPayload = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.verify(socket).sendMessage(argPacket.capture(), argPayload.capture());
        Mockito.verify(listener).onSuccess(null);

        JSONObject packet = argPacket.getValue();
        JSONObject payload = argPayload.getValue();
        Assert.assertNull(payload);
        Assert.assertTrue(packet.has("payload"));
        Assert.assertEquals("playNext", packet.getJSONObject("payload")
                .getJSONObject("mediaCommand").getString("type"));
        Assert.assertEquals("connectsdk.mediaCommand", packet.getJSONObject("payload")
                .getString("contentType"));
    }

    @Test
    public void testJumpToTrack() throws JSONException {
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        session.jumpToTrack(7, listener);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        ArgumentCaptor<JSONObject> argPacket = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONObject> argPayload = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.verify(socket).sendMessage(argPacket.capture(), argPayload.capture());
        Mockito.verify(listener).onSuccess(null);

        JSONObject packet = argPacket.getValue();
        JSONObject payload = argPayload.getValue();
        Assert.assertNull(payload);
        Assert.assertTrue(packet.has("payload"));
        Assert.assertEquals("jumpToTrack", packet.getJSONObject("payload")
                .getJSONObject("mediaCommand").getString("type"));
        Assert.assertEquals(7, packet.getJSONObject("payload")
                .getJSONObject("mediaCommand").getInt("index"));
        Assert.assertEquals("connectsdk.mediaCommand", packet.getJSONObject("payload")
                .getString("contentType"));
    }

    @Test
    public void testGetPlaylistControl() {
        Assert.assertSame(session, session.getPlaylistControl());
    }

    @Test
    public void testGetPlaylistControlCapability() {
        Assert.assertEquals(CapabilityMethods.CapabilityPriorityLevel.HIGH,
                session.getPlaylistControlCapabilityLevel());
    }

    @Test
    public void testSendMessageWithEmptySocketShouldNotCrash() {
        ServiceDescription description = Mockito.mock(ServiceDescription.class);
        Mockito.when(description.getIpAddress()).thenReturn("127.0.0.1");
        Mockito.when(service.getServiceDescription()).thenReturn(description);
        ServiceConfig config = Mockito.mock(WebOSTVServiceConfig.class);
        Mockito.when(service.getServiceConfig()).thenReturn(config);

        session.socket = null;
        session.setConnected(true);
        ResponseListener<Object> listener = Mockito.mock(ResponseListener.class);
        try {
            session.sendMessage("message", listener);
        } catch (RuntimeException e) {
            Assert.fail("sendMessage should not throw an exception");
        }
    }

    @Test
    public void testPlayMediaDeprecatedWithRequiredParameters() throws JSONException {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        final boolean shouldLoop = true;
        final MediaInfo mediaInfo = new MediaInfo.Builder("url", "type").build();

        session.playMedia(mediaInfo.getUrl(), mediaInfo.getMimeType(), null, null, null, shouldLoop,
                listener);

        verifyPlayMedia(shouldLoop, null, mediaInfo);
    }

    @Test(expected = NullPointerException.class)
    public void testPlayMediaWithNullParametersShouldThrowException() throws JSONException {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        final boolean shouldLoop = true;

        session.playMedia(null, shouldLoop, listener);
    }

    @Test
    public void testPlayMediaWithRequiredParameters() throws JSONException {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        final boolean shouldLoop = true;
        final MediaInfo mediaInfo = new MediaInfo.Builder("url", "type").build();

        session.playMedia(mediaInfo, shouldLoop, listener);

        verifyPlayMedia(shouldLoop, null, mediaInfo);
    }

    @Test
    public void testPlayMediaWithSubtitles() throws JSONException {
        MediaPlayer.LaunchListener listener = Mockito.mock(MediaPlayer.LaunchListener.class);
        final boolean shouldLoop = true;
        final SubtitleInfo subtitleInfo = new SubtitleInfo.Builder("subtitleurl")
                .setLabel("label")
                .setLanguage("en")
                .setMimeType("subtitletype")
                .build();
        final MediaInfo mediaInfo = new MediaInfo.Builder("url", "type")
                .setIcon("icon")
                .setTitle("title")
                .setDescription("description")
                .setSubtitleInfo(subtitleInfo)
                .build();

        session.playMedia(mediaInfo, shouldLoop, listener);

        verifyPlayMedia(shouldLoop, subtitleInfo, mediaInfo);
    }

    private void verifyPlayMedia(boolean shouldLoop, SubtitleInfo subtitleInfo, MediaInfo
            mediaInfo) throws JSONException {
        ArgumentCaptor<JSONObject> argPacket = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.verify(socket).sendMessage(argPacket.capture(), Mockito.isNull(JSONObject.class));
        JSONObject capturedPacket = argPacket.getValue();
        JSONObject expectedPacket = getPlayMediaExpectedRequest(shouldLoop, subtitleInfo, mediaInfo);

        Assert.assertEquals(expectedPacket.toString(), capturedPacket.toString());
    }

    @NonNull
    private JSONObject getPlayMediaExpectedRequest(final boolean shouldLoop, final SubtitleInfo
            subtitleInfo, final MediaInfo mediaInfo) throws JSONException {
        return new JSONObject() {{
            put("type", "p2p");
            put("to", "com.webos.app.webapphost.MediaPlayer");
            put("payload", new JSONObject() {{
                putOpt("contentType", "connectsdk.mediaCommand");
                putOpt("mediaCommand", new JSONObject() {{
                    putOpt("type", "playMedia");
                    putOpt("mediaURL", mediaInfo.getUrl());
                    if (mediaInfo.getImages() != null) {
                        putOpt("iconURL", mediaInfo.getImages().get(0).getUrl());
                    }
                    putOpt("title", mediaInfo.getTitle());
                    putOpt("description", mediaInfo.getDescription());
                    putOpt("mimeType", mediaInfo.getMimeType());
                    putOpt("shouldLoop", shouldLoop);
                    put("requestId", "req1");
                    if (subtitleInfo != null) {
                        putOpt("subtitles", new JSONObject() {{
                            putOpt("default", "1");
                            putOpt("enabled", "1");
                            putOpt("tracks", new JSONArray() {{
                                put(new JSONObject() {{
                                    putOpt("id", "1");
                                    putOpt("language", subtitleInfo.getLanguage());
                                    putOpt("source", subtitleInfo.getUrl());
                                    putOpt("label", subtitleInfo.getLabel());
                                }});

                            }});

                        }});
                    }

                }});

            }});

        }};
    }


}
