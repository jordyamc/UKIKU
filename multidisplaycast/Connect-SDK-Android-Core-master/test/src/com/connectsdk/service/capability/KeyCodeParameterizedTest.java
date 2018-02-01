/*
 * KeyCodeParameterizedTest
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Oleksii Frolov on 20 Aug 2015
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
package com.connectsdk.service.capability;

import com.connectsdk.service.capability.KeyControl.KeyCode;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class KeyCodeParameterizedTest {

    private final int value;
    private final KeyCode keyCode;

    public static Object[][] data = new Object[][] {
        {0, KeyCode.NUM_0},
        {1, KeyCode.NUM_1},
        {2, KeyCode.NUM_2},
        {3, KeyCode.NUM_3},
        {4, KeyCode.NUM_4},
        {5, KeyCode.NUM_5},
        {6, KeyCode.NUM_6},
        {7, KeyCode.NUM_7},
        {8, KeyCode.NUM_8},
        {9, KeyCode.NUM_9},
        {10, KeyCode.DASH},
        {11, KeyCode.ENTER},
        {12, null},
        {-1, null},
        {999, null},
    };

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {0},
                {1},
                {2},
                {3},
                {4},
                {5},
                {6},
                {7},
                {8},
                {9},
                {10},
                {11},
                {12},
                {13},
                {14},
            }
        );
    }

    public KeyCodeParameterizedTest(int index) {
        this.value = (Integer)data[index][0];
        this.keyCode = (KeyCode)data[index][1];
    }

    @Test
    public void testGetKeyCodeFromInt() {
        Assert.assertEquals(keyCode, KeyCode.createFromInteger(value));
    }

}
