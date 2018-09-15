package knf.kuma.commons

import fi.iki.elonen.NanoHTTPD
import knf.kuma.download.FileAccessHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import xdroid.toaster.Toaster
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object SelfServer {
    var HTTP_PORT = 6991
    private var INSTANCE: Server? = null

    fun start(data: String, isFile: Boolean): String? {
        return try {
            stop()
            INSTANCE = Server(data, isFile)
            "http://" + Network.ipAddress + ":" + HTTP_PORT
        } catch (e: Exception) {
            e.printStackTrace()
            Toaster.toast("Error al iniciar server")
            null
        }

    }

    fun stop() {
        if (INSTANCE != null && INSTANCE!!.isAlive)
            INSTANCE!!.stop()
    }

    private class Server @Throws(Exception::class)
    constructor(private val data: String, private val isFile: Boolean) : NanoHTTPD(HTTP_PORT) {

        init {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        }

        override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response? {
            return if (isFile)
                serveFile(session.headers, data)
            else
                serveWeb(session.headers, data)
        }

        private fun getSize(url: String): Long {
            return try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                connection.contentLength.toLong()
            } catch (e: Exception) {
                0
            }

        }

        private fun serveWeb(header: Map<String, String>, url: String): NanoHTTPD.Response {
            var res: NanoHTTPD.Response?
            val mime = "video/mp4"
            try {
                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .url(url)
                var startFrom: Long = 0
                var endAt: Long = -1
                var range = header["range"]
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length)
                        val minus = range.indexOf('-')
                        try {
                            if (minus > 0) {
                                startFrom = java.lang.Long.parseLong(range.substring(0, minus))
                                endAt = java.lang.Long.parseLong(range.substring(minus + 1))
                            }
                        } catch (ignored: NumberFormatException) {
                        }

                    }
                }
                val fileLen = getSize(url)
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = createResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "")
                        res.addHeader("Content-Range", "bytes 0-0/$fileLen")
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1
                        }
                        var newLen = endAt - startFrom + 1
                        if (newLen < 0) {
                            newLen = 0
                        }

                        val dataLen = newLen
                        request.addHeader("Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen)
                        val response = okHttpClient.newCall(request.build()).execute()
                        val fis = response.body()!!.byteStream()
                        fis.skip(startFrom)

                        res = createResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mime, fis, dataLen)
                        res.addHeader("Content-Length", "" + dataLen)
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen)
                    }
                } else {
                    val response = okHttpClient.newCall(request.build()).execute()
                    val fis = response.body()!!.byteStream()
                    res = createResponse(NanoHTTPD.Response.Status.OK, mime, fis, fileLen)
                    res.addHeader("Content-Length", "" + fileLen)
                }
            } catch (e: Exception) {
                res = getResponse("Forbidden: Reading file failed")
            }

            return res ?: getResponse("Error 404: File not found")
        }

        private fun serveFile(header: Map<String, String>, file_name: String): NanoHTTPD.Response? {
            var res: NanoHTTPD.Response?
            val mime = "video/mp4"
            val file = FileAccessHelper.INSTANCE.getFile(file_name)
            try {
                // Calculate etag
                val etag = Integer.toHexString((file.absolutePath +
                        file.lastModified() + "" + file.length()).hashCode())

                // Support (simple) skipping:
                var startFrom: Long = 0
                var endAt: Long = -1
                var range = header["range"]
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length)
                        val minus = range.indexOf('-')
                        try {
                            if (minus > 0) {
                                startFrom = java.lang.Long.parseLong(range.substring(0, minus))
                                endAt = java.lang.Long.parseLong(range.substring(minus + 1))
                            }
                        } catch (ignored: NumberFormatException) {
                        }

                    }
                }

                // Change return code and add Content-Range header when skipping is requested
                val fileLen = file.length()
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = createResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "")
                        res.addHeader("Content-Range", "bytes 0-0/$fileLen")
                        res.addHeader("ETag", etag)
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1
                        }
                        var newLen = endAt - startFrom + 1
                        if (newLen < 0) {
                            newLen = 0
                        }

                        val dataLen = newLen
                        val fis = FileAccessHelper.INSTANCE.getInputStream(file_name)
                        fis!!.skip(startFrom)

                        res = createResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mime, fis, dataLen)
                        res.addHeader("Content-Length", "" + dataLen)
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen)
                        res.addHeader("ETag", etag)
                    }
                } else {
                    if (etag == header["if-none-match"])
                        res = createResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "")
                    else {
                        res = createResponse(NanoHTTPD.Response.Status.OK, mime, FileAccessHelper.INSTANCE.getInputStream(file_name), fileLen)
                        res.addHeader("Content-Length", "" + fileLen)
                        res.addHeader("ETag", etag)
                    }
                }
            } catch (ioe: IOException) {
                res = getResponse("Forbidden: Reading file failed")
            }

            return res ?: getResponse("Error 404: File not found")
        }

        // Announce that the file server accepts partial content requests
        private fun createResponse(status: Response.Status, mimeType: String, message: InputStream?, lenght: Long): NanoHTTPD.Response {
            val res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message, lenght)
            res.addHeader("Accept-Ranges", "bytes")
            return res
        }

        // Announce that the file server accepts partial content requests
        private fun createResponse(status: Response.Status, mimeType: String, message: String): NanoHTTPD.Response {
            val res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message)
            res.addHeader("Accept-Ranges", "bytes")
            return res
        }

        private fun getResponse(message: String): NanoHTTPD.Response {
            return createResponse(NanoHTTPD.Response.Status.OK, "text/plain", message)
        }
    }
}
