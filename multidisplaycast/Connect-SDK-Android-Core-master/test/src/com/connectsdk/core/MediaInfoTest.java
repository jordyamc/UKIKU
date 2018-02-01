/*
 * MediaInfoTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 20 Jul 2015
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
package com.connectsdk.core;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MediaInfoTest {

    @Test
    public void testMediaInfoBuilderWithRequiredParameters() {
        String url = "http://127.0.0.1/";
        String mimeType = "video/mp4";
        MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType).build();

        Assert.assertEquals(url, mediaInfo.getUrl());
        Assert.assertEquals(mimeType, mediaInfo.getMimeType());
        Assert.assertNull(mediaInfo.getDescription());
        Assert.assertEquals(0, mediaInfo.getDuration());
        Assert.assertNull(mediaInfo.getImages());
        Assert.assertNull(mediaInfo.getSubtitleInfo());
        Assert.assertNull(mediaInfo.getTitle());
    }

    @Test
    public void testMediaInfoBuilderWithAllParameters() {
        String url = "http://127.0.0.1/";
        String mimeType = "video/mp4";
        String description = "description";
        String iconUrl = "http://iconurl";

        SubtitleInfo subtitle = new SubtitleInfo.Builder("").build();
        String title = "title";
        MediaInfo mediaInfo = new MediaInfo
                .Builder(url, mimeType)
                .setDescription(description)
                .setIcon(iconUrl)
                .setSubtitleInfo(subtitle)
                .setTitle(title)
                .build();

        Assert.assertEquals(url, mediaInfo.getUrl());
        Assert.assertEquals(mimeType, mediaInfo.getMimeType());
        Assert.assertEquals(description, mediaInfo.getDescription());
        Assert.assertEquals(iconUrl, mediaInfo.getImages().get(0).getUrl());
        Assert.assertEquals(1, mediaInfo.getImages().size());
        Assert.assertEquals(subtitle, mediaInfo.getSubtitleInfo());
        Assert.assertEquals(title, mediaInfo.getTitle());
    }

    @Test
    public void testMediaInfoBuilderWithNullIconShouldNotReturnNullImagesList() {
        String url = "http://127.0.0.1/";
        String mimeType = "video/mp4";
        MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType)
                .setIcon((String) null)
                .build();

        Assert.assertEquals(url, mediaInfo.getUrl());
        Assert.assertEquals(mimeType, mediaInfo.getMimeType());
        Assert.assertNull(mediaInfo.getDescription());
        Assert.assertEquals(0, mediaInfo.getDuration());
        Assert.assertNull(mediaInfo.getImages());
        Assert.assertNull(mediaInfo.getSubtitleInfo());
        Assert.assertNull(mediaInfo.getTitle());
    }
}
