/*
 * SubtitleTrackTest
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
public class SubtitleInfoTest {

    @Test
    public void testCreateSubtitleWithRequiredParameters() {
        String url = "http://127.0.0.1/";
        SubtitleInfo subtitle = new SubtitleInfo.Builder(url).build();

        Assert.assertEquals(url, subtitle.getUrl());
        Assert.assertNull(subtitle.getMimeType());
        Assert.assertNull(subtitle.getLabel());
        Assert.assertNull(subtitle.getLanguage());
    }

    @Test
    public void testCreateSubtitleWithAllParameters() {
        String url = "http://127.0.0.1/";
        String mimetype = "text/vtt";
        String label = "label";
        String language = "en";
        SubtitleInfo subtitle = new SubtitleInfo
                .Builder(url)
                .setMimeType(mimetype)
                .setLabel(label)
                .setLanguage(language)
                .build();

        Assert.assertEquals(url, subtitle.getUrl());
        Assert.assertEquals(mimetype, subtitle.getMimeType());
        Assert.assertEquals(label, subtitle.getLabel());
        Assert.assertEquals(language, subtitle.getLanguage());
    }
}
