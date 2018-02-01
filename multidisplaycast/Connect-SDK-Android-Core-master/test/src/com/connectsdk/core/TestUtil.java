package com.connectsdk.core;

import org.apache.tools.ant.filters.StringInputStream;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by oleksii.frolov on 1/30/2015.
 */
public final class TestUtil {

    public static URL getMockUrl(final String content, String applicationUrl) throws IOException {
        final URLConnection mockConnection = Mockito.mock(URLConnection.class);
        Mockito.when(mockConnection.getInputStream()).thenReturn(new StringInputStream(content));
        Mockito.when(mockConnection.getHeaderField("Application-URL")).thenReturn(applicationUrl);

        final URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL arg0)
                    throws IOException {
                return mockConnection;
            }
        };
        return new URL("http", "hostname", 80, "", handler);
    }

    public static void runUtilBackgroundTasks() {
        ExecutorService executor = (ExecutorService) Util.getExecutor();
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Util.createExecutor();
    }

    /**
     * Compare 2 URLs with custom parameters order
     * @param expectedUrl
     * @param targetUrl
     * @return true if URLs equal
     */
    public static boolean compareUrls(String expectedUrl, String targetUrl) {
        URI expectedURI = URI.create(expectedUrl).normalize();
        URI targetURI = URI.create(targetUrl).normalize();

        String[] expectedQuery = expectedURI.getQuery().split("&");
        List<String> targetQuery = new LinkedList<String>(
                Arrays.asList(targetURI.getQuery().split("&")));

        for (String item : expectedQuery) {
            if (!targetQuery.remove(item)) {
                return false;
            }
        }

        if (!targetQuery.isEmpty()) {
            return false;
        }

        String schemeExpected = expectedURI.getScheme();
        String scheme = targetURI.getScheme();

        String hostExpected = expectedURI.getHost();
        String host = targetURI.getHost();

        String pathExpected = expectedURI.getPath();
        String path = targetURI.getPath();

        int portExpected = expectedURI.getPort();
        int port = targetURI.getPort();

        return schemeExpected.equals(scheme) && hostExpected.equals(host)
                && pathExpected.equals(path) && portExpected == port;
    }
}
