package knf.kuma.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import knf.kuma.downloadservice.FileAccessHelper;
import xdroid.toaster.Toaster;

public class SelfServer {
    private static Server INSTANCE;

    public static String start(String file_name) {
        try {
            stop();
            INSTANCE = new Server(file_name);
            return "http://" + Network.getIPAddress() + ":8080";
        } catch (Exception e) {
            e.printStackTrace();
            Toaster.toast("Error al iniciar server");
            return null;
        }
    }

    public static void stop() {
        if (INSTANCE != null && INSTANCE.isAlive())
            INSTANCE.stop();
    }

    private static class Server extends NanoHTTPD {
        private String file_name;

        public Server(String file_name) throws Exception {
            super(8080);
            this.file_name = file_name;
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }

        @Override
        public Response serve(IHTTPSession session) {
            return serveFile(session.getHeaders(), file_name);
        }

        private Response serveFile(Map<String, String> header, String file_name) {
            Response res;
            String mime = "video/mp4";
            File file = FileAccessHelper.INSTANCE.getFile(file_name);
            try {
                // Calculate etag
                String etag = Integer.toHexString((file.getAbsolutePath() +
                        file.lastModified() + "" + file.length()).hashCode());

                // Support (simple) skipping:
                long startFrom = 0;
                long endAt = -1;
                String range = header.get("range");
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length());
                        int minus = range.indexOf('-');
                        try {
                            if (minus > 0) {
                                startFrom = Long.parseLong(range.substring(0, minus));
                                endAt = Long.parseLong(range.substring(minus + 1));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // Change return code and add Content-Range header when skipping is requested
                long fileLen = file.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1;
                        }
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0) {
                            newLen = 0;
                        }

                        final long dataLen = newLen;
                        InputStream fis = FileAccessHelper.INSTANCE.getInputStream(file_name);
                        fis.skip(startFrom);

                        res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis, dataLen);
                        res.addHeader("Content-Length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match")))
                        res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                    else {
                        res = createResponse(Response.Status.OK, mime, FileAccessHelper.INSTANCE.getInputStream(file_name), fileLen);
                        res.addHeader("Content-Length", "" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                }
            } catch (IOException ioe) {
                res = getResponse("Forbidden: Reading file failed");
            }

            return (res == null) ? getResponse("Error 404: File not found") : res;
        }

        // Announce that the file server accepts partial content requests
        private Response createResponse(Response.Status status, String mimeType, InputStream message, long lenght) {
            Response res = newFixedLengthResponse(status, mimeType, message, lenght);
            res.addHeader("Accept-Ranges", "bytes");
            return res;
        }

        // Announce that the file server accepts partial content requests
        private Response createResponse(Response.Status status, String mimeType, String message) {
            Response res = newFixedLengthResponse(status, mimeType, message);
            res.addHeader("Accept-Ranges", "bytes");
            return res;
        }

        private Response getResponse(String message) {
            return createResponse(Response.Status.OK, "text/plain", message);
        }
    }
}
