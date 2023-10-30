/*
 * Copyright 2015 Bhavit Singh Sengar
 * Copyright 2015-2016 Hans-Christoph Steiner
 * Copyright 2015-2016 Nathan Freitas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * From https://stackoverflow.com/a/29946540
 */

package knf.kuma.custom;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * While making a secure connection, Android's {@link HttpsURLConnection} falls
 * back to SSLv3 from TLSv1. This is a bug in android versions &lt; 4.4. It can be
 * fixed by removing the SSLv3 protocol from Enabled Protocols list. Use this as
 * the {@link SSLSocketFactory} for
 * {@link HttpsURLConnection#setDefaultSSLSocketFactory(SSLSocketFactory)}
 *
 * @author Bhavit S. Sengar
 * @author Hans-Christoph Steiner
 * @see SSLSocket table of protocols and ciphers Android supports
 * @see <a href="https://android.googlesource.com/platform/external/boringssl/+/refs/tags/android-7.1.2_r36/src/include/openssl/ssl.h#3267">source of protocol name constants</a>
 */
public class TlsOnlySocketFactory extends SSLSocketFactory {

    public static final String TLSV1_2 = "TLSv1.2";
    public static final String TLSV1_1 = "TLSv1.1";
    public static final String TLSV1 = "TLSv1";
    public static final String SSLV3 = "SSLv3";
    public static final String SSLV2 = "SSLv2";

    private static final int HANDSHAKE_TIMEOUT = 0;
    private static final String TAG = "TlsOnlySocketFactory";
    private final SSLSocketFactory delegate;
    private final boolean compatible;

    public TlsOnlySocketFactory() {
        this.delegate = SSLCertificateSocketFactory.getDefault(HANDSHAKE_TIMEOUT, null);
        this.compatible = false;
    }

    public TlsOnlySocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
        this.compatible = false;
    }

    /**
     * Make {@link SSLSocket}s that are compatible with outdated servers.
     *
     * @param delegate
     * @param compatible
     */
    public TlsOnlySocketFactory(SSLSocketFactory delegate, boolean compatible) {
        this.delegate = delegate;
        this.compatible = compatible;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    /**
     * @see <a href="https://timtaubert.de/blog/2014/11/the-sad-state-of-server-side-tls-session-resumption-implementations/">The sad state of server-side TLS Session Resumption implementations</a>
     */
    private Socket makeSocketSafe(Socket socket, String host) {
        if (socket instanceof SSLSocket) {
            TlsOnlySSLSocket tempSocket =
                    new TlsOnlySSLSocket((SSLSocket) socket, compatible);

            if (delegate instanceof SSLCertificateSocketFactory factory &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                factory.setHostname(socket, host);
                factory.setUseSessionTickets(socket, false);
            } else {
                tempSocket.setHostname(host);
            }

            socket = tempSocket;
        }
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return makeSocketSafe(delegate.createSocket(s, host, port, autoClose), host);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return makeSocketSafe(delegate.createSocket(host, port), host);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        return makeSocketSafe(delegate.createSocket(host, port, localHost, localPort), host);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return makeSocketSafe(delegate.createSocket(host, port), host.getHostName());
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                               int localPort) throws IOException {
        return makeSocketSafe(delegate.createSocket(address, port, localAddress, localPort),
                address.getHostName());
    }

    private class TlsOnlySSLSocket extends DelegateSSLSocket {

        final boolean compatible;

        private TlsOnlySSLSocket(SSLSocket delegate, boolean compatible) {
            super(delegate);
            this.compatible = compatible;

            // badly configured servers can't handle a good config
            if (compatible) {
                ArrayList<String> protocols = new ArrayList<String>(Arrays.asList(delegate
                        .getEnabledProtocols()));
                protocols.remove(SSLV2);
                protocols.remove(SSLV3);
                super.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));

                /*
                 * Exclude extremely weak EXPORT ciphers. NULL ciphers should
                 * never even have been an option in TLS.
                 */
                ArrayList<String> enabled = new ArrayList<String>(10);
                Pattern exclude = Pattern.compile(".*(EXPORT|NULL).*");
                for (String cipher : delegate.getEnabledCipherSuites()) {
                    if (!exclude.matcher(cipher).matches()) {
                        enabled.add(cipher);
                    }
                }
                super.setEnabledCipherSuites(enabled.toArray(new String[enabled.size()]));
                return;
            } // else

            // 16-19 support v1.1 and v1.2 but only by default starting in 20+
            // https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
            ArrayList<String> protocols = new ArrayList<String>(Arrays.asList(delegate
                    .getSupportedProtocols()));
            protocols.remove(SSLV2);
            protocols.remove(SSLV3);
            if (Build.VERSION.SDK_INT >= 24) {
                protocols.remove(TLSV1);
                protocols.remove(TLSV1_1);
            }
            super.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));

            /*
             * Exclude weak ciphers, like EXPORT, MD5, DES, and DH. NULL ciphers
             * should never even have been an option in TLS.
             */
            ArrayList<String> enabledCiphers = new ArrayList<String>(10);
            Pattern exclude = Pattern.compile(".*(_DES|DH_|DSS|EXPORT|MD5|NULL|RC4|TLS_FALLBACK_SCSV).*");
            for (String cipher : delegate.getSupportedCipherSuites()) {
                if (!exclude.matcher(cipher).matches()) {
                    enabledCiphers.add(cipher);
                }
            }
            super.setEnabledCipherSuites(enabledCiphers.toArray(new String[enabledCiphers.size()]));
        }

        /**
         * This works around a bug in Android < 19 where SSLv3 is forced
         */
        @Override
        public void setEnabledProtocols(String[] protocols) {
            if (protocols != null && protocols.length == 1 && SSLV3.equals(protocols[0])) {
                List<String> systemProtocols;
                if (this.compatible) {
                    systemProtocols = Arrays.asList(delegate.getEnabledProtocols());
                } else {
                    systemProtocols = Arrays.asList(delegate.getSupportedProtocols());
                }
                List<String> enabledProtocols = new ArrayList<String>(systemProtocols);
                if (enabledProtocols.size() > 1) {
                    enabledProtocols.remove(SSLV2);
                    enabledProtocols.remove(SSLV3);
                } else {
                    Log.w(TAG, "SSL stuck with protocol available for "
                            + enabledProtocols);
                }
                protocols = enabledProtocols.toArray(new String[enabledProtocols.size()]);
            }
            super.setEnabledProtocols(protocols);
        }
    }

    public class DelegateSSLSocket extends SSLSocket {

        protected final SSLSocket delegate;

        DelegateSSLSocket(SSLSocket delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return delegate.getEnabledCipherSuites();
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            delegate.setEnabledCipherSuites(suites);
        }

        @Override
        public String[] getSupportedProtocols() {
            return delegate.getSupportedProtocols();
        }

        @Override
        public String[] getEnabledProtocols() {
            return delegate.getEnabledProtocols();
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            delegate.setEnabledProtocols(protocols);
        }

        @Override
        public SSLSession getSession() {
            return delegate.getSession();
        }

        @Override
        public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
            delegate.addHandshakeCompletedListener(listener);
        }

        @Override
        public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
            delegate.removeHandshakeCompletedListener(listener);
        }

        @Override
        public void startHandshake() throws IOException {
            delegate.startHandshake();
        }

        @Override
        public boolean getUseClientMode() {
            return delegate.getUseClientMode();
        }

        @Override
        public void setUseClientMode(boolean mode) {
            delegate.setUseClientMode(mode);
        }

        @Override
        public boolean getNeedClientAuth() {
            return delegate.getNeedClientAuth();
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            delegate.setNeedClientAuth(need);
        }

        @Override
        public boolean getWantClientAuth() {
            return delegate.getWantClientAuth();
        }

        @Override
        public void setWantClientAuth(boolean want) {
            delegate.setWantClientAuth(want);
        }

        @Override
        public boolean getEnableSessionCreation() {
            return delegate.getEnableSessionCreation();
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            delegate.setEnableSessionCreation(flag);
        }

        @Override
        public void bind(SocketAddress localAddr) throws IOException {
            delegate.bind(localAddr);
        }

        @Override
        public synchronized void close() throws IOException {
            delegate.close();
        }

        @Override
        public void connect(SocketAddress remoteAddr) throws IOException {
            delegate.connect(remoteAddr);
        }

        @Override
        public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            delegate.connect(remoteAddr, timeout);
        }

        @Override
        public SocketChannel getChannel() {
            return delegate.getChannel();
        }

        @Override
        public InetAddress getInetAddress() {
            return delegate.getInetAddress();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        @Override
        public boolean getKeepAlive() throws SocketException {
            return delegate.getKeepAlive();
        }

        @Override
        public void setKeepAlive(boolean keepAlive) throws SocketException {
            delegate.setKeepAlive(keepAlive);
        }

        @Override
        public InetAddress getLocalAddress() {
            return delegate.getLocalAddress();
        }

        @Override
        public int getLocalPort() {
            return delegate.getLocalPort();
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return delegate.getLocalSocketAddress();
        }

        @Override
        public boolean getOOBInline() throws SocketException {
            return delegate.getOOBInline();
        }

        @Override
        public void setOOBInline(boolean oobinline) throws SocketException {
            delegate.setOOBInline(oobinline);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return delegate.getOutputStream();
        }

        @Override
        public int getPort() {
            return delegate.getPort();
        }

        @Override
        public synchronized int getReceiveBufferSize() throws SocketException {
            return delegate.getReceiveBufferSize();
        }

        @Override
        public synchronized void setReceiveBufferSize(int size) throws SocketException {
            delegate.setReceiveBufferSize(size);
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return delegate.getRemoteSocketAddress();
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return delegate.getReuseAddress();
        }

        @Override
        public void setReuseAddress(boolean reuse) throws SocketException {
            delegate.setReuseAddress(reuse);
        }

        @Override
        public synchronized int getSendBufferSize() throws SocketException {
            return delegate.getSendBufferSize();
        }

        @Override
        public synchronized void setSendBufferSize(int size) throws SocketException {
            delegate.setSendBufferSize(size);
        }

        @Override
        public int getSoLinger() throws SocketException {
            return delegate.getSoLinger();
        }

        @Override
        public synchronized int getSoTimeout() throws SocketException {
            return delegate.getSoTimeout();
        }

        @Override
        public synchronized void setSoTimeout(int timeout) throws SocketException {
            delegate.setSoTimeout(timeout);
        }

        @Override
        public boolean getTcpNoDelay() throws SocketException {
            return delegate.getTcpNoDelay();
        }

        @Override
        public void setTcpNoDelay(boolean on) throws SocketException {
            delegate.setTcpNoDelay(on);
        }

        @Override
        public int getTrafficClass() throws SocketException {
            return delegate.getTrafficClass();
        }

        @Override
        public void setTrafficClass(int value) throws SocketException {
            delegate.setTrafficClass(value);
        }

        @Override
        public boolean isBound() {
            return delegate.isBound();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public boolean isConnected() {
            return delegate.isConnected();
        }

        @Override
        public boolean isInputShutdown() {
            return delegate.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return delegate.isOutputShutdown();
        }

        @Override
        public void sendUrgentData(int value) throws IOException {
            delegate.sendUrgentData(value);
        }

        @Override
        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            delegate.setPerformancePreferences(connectionTime,
                    latency, bandwidth);
        }

        @Override
        public void setSoLinger(boolean on, int timeout) throws SocketException {
            delegate.setSoLinger(on, timeout);
        }

        @Override
        public void shutdownInput() throws IOException {
            delegate.shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            delegate.shutdownOutput();
        }

        // inspired by https://github.com/k9mail/k-9/commit/54f9fd36a77423a55f63fbf9b1bcea055a239768

        public DelegateSSLSocket setHostname(String host) {
            try {
                delegate
                        .getClass()
                        .getMethod("setHostname", String.class)
                        .invoke(delegate, host);
            } catch (Exception e) {
                //throw new IllegalStateException("Could not enable SNI", e);
            }

            return (this);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }
    }
}
