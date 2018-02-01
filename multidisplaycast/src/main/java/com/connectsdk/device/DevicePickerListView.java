/*
 * DevicePickerListView
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
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

package com.connectsdk.device;

import android.content.Context;
import android.widget.ListView;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.command.ServiceCommandError;

public class DevicePickerListView extends ListView implements DiscoveryManagerListener {

    DevicePickerAdapter pickerAdapter;

    public DevicePickerListView( Context context ) {
        super( context );

        pickerAdapter = new DevicePickerAdapter( context );

        setPadding( 25, 25, 25, 25 );

        setAdapter( pickerAdapter );

        DiscoveryManager.getInstance().addListener( this );
    }

    @Override
    public void onDiscoveryFailed( final DiscoveryManager manager, ServiceCommandError error ) {
        Util.runOnUI( new Runnable() {
            @Override
            public void run() {
                pickerAdapter.set( manager.getAllDevices() );
            }
        } );
    }

    @Override
    public void onDeviceAdded( final DiscoveryManager manager, final ConnectableDevice device ) {
        Util.runOnUI( new Runnable() {
            @Override
            public void run() {
                pickerAdapter.set( manager.getAllDevices() );
            }
        } );
    }

    @Override
    public void onDeviceUpdated( final DiscoveryManager manager, final ConnectableDevice device ) {
        Util.runOnUI( new Runnable() {
            @Override
            public void run() {
                pickerAdapter.set( manager.getAllDevices() );
            }
        } );
    }

    @Override
    public void onDeviceRemoved( final DiscoveryManager manager, final ConnectableDevice device ) {
        Util.runOnUI( new Runnable() {
            @Override
            public void run() {
                pickerAdapter.set( manager.getAllDevices() );
            }
        } );
    }
}
