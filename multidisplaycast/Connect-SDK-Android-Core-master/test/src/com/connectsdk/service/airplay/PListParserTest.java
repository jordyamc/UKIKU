/*
 * PListParserTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 19 Mar 2015
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
package com.connectsdk.service.airplay;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class PListParserTest {

    @Test
    public void testSimplePlistParsing() throws JSONException, XmlPullParserException, IOException {
        String rawString = "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" " +
                "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                "<plist version=\"1.0\">\n" +
                "<dict>\n" +
                "\t<key>duration</key>\n" +
                "\t<real>52.209000000000003</real>\n" +
                "\t<key>loadedTimeRanges</key>\n" +
                "\t<array>\n" +
                "\t\t<dict>\n" +
                "\t\t\t<key>duration</key>\n" +
                "\t\t\t<real>52.209000000000003</real>\n" +
                "\t\t\t<key>start</key>\n" +
                "\t\t\t<real>0.0</real>\n" +
                "\t\t</dict>\n" +
                "\t</array>\n" +
                "\t<key>playbackBufferEmpty</key>\n" +
                "\t<true/>\n" +
                "\t<key>playbackBufferFull</key>\n" +
                "\t<false/>\n" +
                "\t<key>playbackLikelyToKeepUp</key>\n" +
                "\t<true/>\n" +
                "\t<key>position</key>\n" +
                "\t<real>4.6505421629999999</real>\n" +
                "\t<key>rate</key>\n" +
                "\t<real>1</real>\n" +
                "\t<key>readyToPlay</key>\n" +
                "\t<true/>\n" +
                "\t<key>seekableTimeRanges</key>\n" +
                "\t<array>\n" +
                "\t\t<dict>\n" +
                "\t\t\t<key>duration</key>\n" +
                "\t\t\t<real>52.209000000000003</real>\n" +
                "\t\t\t<key>start</key>\n" +
                "\t\t\t<real>0.0</real>\n" +
                "\t\t</dict>\n" +
                "\t</array>\n" +
                "\t<key>stallCount</key>\n" +
                "\t<integer>0</integer>\n" +
                "\t<key>uuid</key>\n" +
                "\t<string>D6E86A89-82F0-41F5-B680-B27AB83656F6-25-0000000E81A3E1CF</string>\n" +
                "</dict>\n" +
                "</plist>\n";

        JSONObject json = new PListParser().parse(rawString);
        Assert.assertTrue(json.has("duration"));
        Assert.assertTrue(json.has("loadedTimeRanges"));
        Assert.assertTrue(json.has("playbackBufferEmpty"));
        Assert.assertTrue(json.has("playbackBufferFull"));
        Assert.assertTrue(json.has("playbackLikelyToKeepUp"));
        Assert.assertTrue(json.has("position"));
        Assert.assertTrue(json.has("rate"));
        Assert.assertTrue(json.has("readyToPlay"));
        Assert.assertTrue(json.has("seekableTimeRanges"));
        Assert.assertTrue(json.has("uuid"));
        Assert.assertTrue(json.has("rate"));
        Assert.assertTrue(json.getJSONArray("seekableTimeRanges").getJSONObject(0).has("start"));

        Assert.assertEquals(1, json.getInt("rate"));
        Assert.assertEquals("D6E86A89-82F0-41F5-B680-B27AB83656F6-25-0000000E81A3E1CF",
                json.getString("uuid"));
    }

    @Test
    public void testHLSPlistParsing() throws JSONException, XmlPullParserException, IOException {
        String rawString = "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" " +
                "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                "    <plist version=\"1.0\">\n" +
                "    <dict>\n" +
                "    <key>duration</key>\n" +
                "    <real>0.0</real>\n" +
                "    <key>estimatedDate</key>\n" +
                "    <date>2015-07-14T17:55:59Z</date>\n" +
                "    <key>loadedTimeRanges</key>\n" +
                "    <array>\n" +
                "    <dict>\n" +
                "    <key>duration</key>\n" +
                "    <real>15.952108843537415</real>\n" +
                "    <key>start</key>\n" +
                "    <real>0.0</real>\n" +
                "    </dict>\n" +
                "    </array>\n" +
                "    <key>playbackBufferEmpty</key>\n" +
                "    <true/>\n" +
                "    <key>playbackBufferFull</key>\n" +
                "    <false/>\n" +
                "    <key>playbackLikelyToKeepUp</key>\n" +
                "    <true/>\n" +
                "    <key>position</key>\n" +
                "    <real>3.4013898230000001</real>\n" +
                "    <key>rate</key>\n" +
                "    <real>1</real>\n" +
                "    <key>readyToPlay</key>\n" +
                "    <true/>\n" +
                "    <key>seekableTimeRanges</key>\n" +
                "    <array>\n" +
                "    <dict>\n" +
                "    <key>duration</key>\n" +
                "    <real>0.0</real>\n" +
                "    <key>start</key>\n" +
                "    <real>0.0</real>\n" +
                "    </dict>\n" +
                "    </array>\n" +
                "    <key>stallCount</key>\n" +
                "    <integer>0</integer>\n" +
                "    <key>uuid</key>\n" +
                "    <string>792FE533-1CC6-474B-84BD-A0D3D9081626-40-00001A5906010248</string>\n" +
                "    </dict>\n" +
                "    </plist>";

        JSONObject json = new PListParser().parse(rawString);
        Assert.assertTrue(json.has("loadedTimeRanges"));
        Assert.assertEquals(1, json.getInt("rate"));
        Assert.assertTrue(json.getBoolean("readyToPlay"));
        Assert.assertEquals("792FE533-1CC6-474B-84BD-A0D3D9081626-40-00001A5906010248",
                json.getString("uuid"));
    }
}
