package com.connectsdk.shadow;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowWifiInfo;

import android.net.wifi.WifiInfo;

@Implements(WifiInfo.class)
public class WifiInfoShadow extends ShadowWifiInfo {

    public int getIpAddress() {
        try {
            byte[] addr = InetAddress.getLocalHost().getAddress();
            return addr[0] + (addr[1] << 8) + (addr[2] << 16) + (addr[3] << 24);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
