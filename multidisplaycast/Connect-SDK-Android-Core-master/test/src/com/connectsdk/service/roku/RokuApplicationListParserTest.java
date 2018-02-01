/*
 * RokuApplicationListParserTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 08 Sep 2015
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
package com.connectsdk.service.roku;

import com.connectsdk.core.AppInfo;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RokuApplicationListParserTest {

    RokuApplicationListParser parser;
    private SAXParser saxParser;

    @Before
    public void setUp() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParser = saxParserFactory.newSAXParser();
        parser = new RokuApplicationListParser();
    }

    @Test
    public void testParseListWithEmptyInput() throws IOException, ParserConfigurationException, SAXException {
        String msg = "<root></root>";
        InputStream stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));

        saxParser.parse(stream, parser);

        List<AppInfo> appList = parser.getApplicationList();
        Assert.assertNotNull(appList);
        Assert.assertTrue(appList.isEmpty());
    }

    @Test
    public void testParseListWithOneItem() throws IOException, ParserConfigurationException, SAXException {
        String msg = "<app id=\"youtube\">YouTube</app>";
        InputStream stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));

        saxParser.parse(stream, parser);

        List<AppInfo> appList = parser.getApplicationList();
        Assert.assertEquals(1, appList.size());
        Assert.assertEquals("youtube", appList.get(0).getId());
    }

    @Test
    public void testParseListWithSeveralItem() throws IOException, ParserConfigurationException, SAXException {
        String msg = "<root><app id=\"youtube\">YouTube</app><app id=\"netflix\">Netflix</app></root>";
        InputStream stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));

        saxParser.parse(stream, parser);

        List<AppInfo> appList = parser.getApplicationList();
        Assert.assertEquals(2, appList.size());
        Assert.assertEquals("youtube", appList.get(0).getId());
        Assert.assertEquals("YouTube", appList.get(0).getName());
        Assert.assertEquals("netflix", appList.get(1).getId());
        Assert.assertEquals("Netflix", appList.get(1).getName());
    }
}
