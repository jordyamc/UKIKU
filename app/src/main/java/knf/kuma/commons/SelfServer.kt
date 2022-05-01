package knf.kuma.commons

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import knf.kuma.App
import knf.kuma.R
import knf.kuma.download.DownloadManager
import knf.kuma.download.foreground
import knf.kuma.download.service
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class SelfServer : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground(64587, foregroundNotification())
        if (intent != null && intent.action != null && intent.action == "stop.foreground") {
            CastUtil.get().stop()
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        foreground(64587, foregroundNotification())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun foregroundNotification(): Notification {
        return NotificationCompat.Builder(App.context, DownloadManager.CHANNEL_FOREGROUND).apply {
            setSmallIcon(R.drawable.ic_server_running)
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_MIN
            setGroup("manager")
            if (PrefsUtil.collapseDirectoryNotification)
                setSubText("Servidor activo")
            else
                setContentTitle("Servidor activo")
            addAction(R.drawable.ic_stop, "Detener",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        PendingIntent.getForegroundService(
                            App.context,
                            4689,
                            Intent(
                                App.context,
                                SelfServer::class.java
                            ).setAction("stop.foreground"),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    else
                        PendingIntent.getService(
                            App.context,
                            4689,
                            Intent(
                                App.context,
                                SelfServer::class.java
                            ).setAction("stop.foreground"),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
            )
        }.build()
    }

    companion object {
        var HTTP_PORT = 6991
        private var INSTANCE: Server? = null

        fun start(data: String, isFile: Boolean = true): String? {
            return try {
                stop(true)
                App.context.service(Intent(App.context, SelfServer::class.java))
                INSTANCE = Server(data, isFile)
                "http://" + Network.ipAddress + ":" + HTTP_PORT
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al iniciar server")
                null
            }

        }

        fun stop(isRestart: Boolean = false) {
            if (INSTANCE?.isAlive == true)
                INSTANCE?.stop()
            if (!isRestart)
                App.context.service(Intent(App.context, SelfServer::class.java).setAction("stop.foreground"))
        }
    }

    private class Server @Throws(Exception::class)
    constructor(private val data: String, private val isFile: Boolean) : NanoHTTPD(HTTP_PORT) {

        init {
            start(SOCKET_READ_TIMEOUT, false)
        }

        override fun serve(session: IHTTPSession): Response? {
            return if (isFile)
                if (URLUtil.isFileUrl(data)) {
                    var file = File(Uri.parse(data).path)
                    if (!file.exists())
                        file = file.parentFile?.listFiles { f -> f.name.contains(Uri.parse(data).path!!.substringAfterLast("$")) }!![0]
                    serveFile(session.headers, file)
                } else
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

        private fun serveWeb(header: Map<String, String>, url: String): Response {
            var res: Response? = null
            val okHttpClient = OkHttpClient()
            val request = Request.Builder().url(url)
            val response = okHttpClient.newCall(request.build()).execute()
            val body = response.body
            val total = body?.contentLength() ?: 0
            val inputStream = body?.byteStream()
            val pipedIn = PipedInputStream()
            val pipedOut = PipedOutputStream(pipedIn)
            if (inputStream != null) {
                doAsync {
                    noCrash {
                        val b = ByteArray(16 * 1024)
                        var len = inputStream.read(b, 0, 16 * 1024)
                        while (len != -1) {
                            pipedOut.write(b, 0, len)
                            len = inputStream.read(b, 0, 16 * 1024)
                        }
                        pipedOut.flush()
                        response.close()
                    }
                }
                Thread.sleep(400)
                res = createResponse(Response.Status.OK, "video/mp4", pipedIn, total)
            }
            return res ?: getResponse("Error 404: File not found")
        }

        private fun serveFile(header: Map<String, String>, file_name: String): Response? {
            var res: Response?
            val mime = "video/mp4"
            val fileWrapper = FileWrapper.create(file_name)
            try {
                if (!fileWrapper.exist)
                    throw IllegalAccessException()
                // Calculate etag
                val etag = Integer.toHexString((fileWrapper.file()?.absolutePath +
                        fileWrapper.lastModified() + "" + fileWrapper.length()).hashCode())

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
                val fileLen = fileWrapper.length() ?: 0
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
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
                        val fis = fileWrapper.inputStream()
                        fis?.skip(startFrom)

                        res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis, dataLen)
                        res.addHeader("Content-Length", "" + dataLen)
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen)
                        res.addHeader("ETag", etag)
                    }
                } else {
                    if (etag == header["if-none-match"])
                        res = createResponse(Response.Status.NOT_MODIFIED, mime, "")
                    else {
                        res = createResponse(Response.Status.OK, mime, fileWrapper.inputStream(), fileLen)
                        res.addHeader("Content-Length", "" + fileLen)
                        res.addHeader("ETag", etag)
                    }
                }
            } catch (ioe: IOException) {
                res = getResponse("Forbidden: Reading file failed")
            }

            return res ?: getResponse("Error 404: File not found")
        }

        private fun serveFile(header: Map<String, String>, file: File): Response? {
            var res: Response?
            val mime = "video/mp4"
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
                        res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
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
                        val fis = FileInputStream(file)
                        fis.skip(startFrom)

                        res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis, dataLen)
                        res.addHeader("Content-Length", "" + dataLen)
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                                endAt + "/" + fileLen)
                        res.addHeader("ETag", etag)
                    }
                } else {
                    if (etag == header["if-none-match"])
                        res = createResponse(Response.Status.NOT_MODIFIED, mime, "")
                    else {
                        res = createResponse(Response.Status.OK, mime, FileInputStream(file), fileLen)
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
        private fun createResponse(status: Response.Status, mimeType: String, message: InputStream?, lenght: Long): Response {
            val res = newFixedLengthResponse(status, mimeType, message, lenght)
            res.addHeader("Accept-Ranges", "bytes")
            return res
        }

        // Announce that the file server accepts partial content requests
        private fun createResponse(status: Response.Status, mimeType: String, message: String): Response {
            val res = newFixedLengthResponse(status, mimeType, message)
            res.addHeader("Accept-Ranges", "bytes")
            return res
        }

        private fun getResponse(message: String): Response {
            return createResponse(Response.Status.OK, "text/plain", message)
        }
    }
}
