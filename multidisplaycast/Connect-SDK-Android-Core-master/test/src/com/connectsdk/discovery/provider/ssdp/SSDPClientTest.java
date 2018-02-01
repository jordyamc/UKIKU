package com.connectsdk.discovery.provider.ssdp;

import com.connectsdk.core.Util;
import com.connectsdk.shadow.WifiInfoShadow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE ,shadows = { WifiInfoShadow.class })
public class SSDPClientTest {

    InetAddress localAddress;
    SSDPClient ssdpClient;

    private DatagramSocket wildSocket = Mockito.mock(DatagramSocket.class);
    private MulticastSocket mLocalSocket = Mockito.mock(MulticastSocket.class);

    public SSDPClientTest() {
        super();
    }

    @Before
    public void setUp() throws IOException {
        localAddress = Util.getIpAddress(Robolectric.application);
        ssdpClient = new SSDPClient(localAddress, mLocalSocket, wildSocket);
    }

    @Test
    public void testSend() throws Exception {
        //Verify is ssdpClient.send() is sending correct SSDP packet to DatagramSocket

        String stringData = "some data";
        DatagramPacket dp = new DatagramPacket(stringData.getBytes(),
                stringData.length(), new InetSocketAddress("239.255.255.250",1900));
        ssdpClient.send(stringData);

        ArgumentCaptor<DatagramPacket> argument = ArgumentCaptor.forClass(DatagramPacket.class);
        Mockito.verify(wildSocket).send(argument.capture());

        Assert.assertEquals(dp.getAddress(), argument.getValue().getAddress());
        Assert.assertEquals(dp.getPort(), argument.getValue().getPort());
        Assert.assertEquals(new String(dp.getData()), new String(argument.getValue().getData()));
    }

    @Test
    public void testResponseRecieve() throws IOException {
        //Verify is ssdpClient.responseReceive() receive SSDP Response packet to DatagramSocket

        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        ssdpClient.responseReceive();

        ArgumentCaptor<DatagramPacket> argument = ArgumentCaptor.forClass(DatagramPacket.class);
        Mockito.verify(wildSocket).receive(argument.capture());
        Assert.assertEquals(dp.getLength(), argument.getValue().getLength());
        Assert.assertEquals(new String(dp.getData()), new String(argument.getValue().getData()));

    }

    @Test
    public void testNotifyReceive() throws IOException {
        //Verify is ssdpClient.NotifyReceive() receive SSDP Notify packet to DatagramSocket.

        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        ssdpClient.multicastReceive();

        ArgumentCaptor<DatagramPacket> argument = ArgumentCaptor.forClass(DatagramPacket.class);
        Mockito.verify(mLocalSocket).receive(argument.capture());
        Assert.assertEquals(dp.getLength(), argument.getValue().getLength());
        Assert.assertEquals(new String(dp.getData()), new String(argument.getValue().getData()));

    }


    @Test
    public void testClose() throws IOException {
        wildSocket.connect(localAddress, 1903);
        mLocalSocket.connect(localAddress, 1904);
        ssdpClient.close();

        Mockito.verify(mLocalSocket, Mockito.times(1)).leaveGroup(Mockito.any(SocketAddress.class),Mockito.any(NetworkInterface.class));
        Mockito.verify(mLocalSocket, Mockito.times(1)).close();
        Mockito.verify(wildSocket, Mockito.times(1)).disconnect();
        Mockito.verify(wildSocket, Mockito.times(1)).close();


    }

    @Test
    public void testSetTimeout() throws Exception {
        Integer testTimeout = 1000;
        ssdpClient.setTimeout(testTimeout);

        ArgumentCaptor<Integer> argument = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(wildSocket, Mockito.times(1)).setSoTimeout(argument.capture());
        Assert.assertEquals(testTimeout, new Integer(argument.getValue()));

    }


}
