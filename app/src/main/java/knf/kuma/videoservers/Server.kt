package knf.kuma.videoservers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import knf.kuma.commons.NoSSLOkHttpClient
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Server(internal var context: Context, internal var baseLink: String) : Comparable<Server> {
    private var server: VideoServer? = null

    abstract val isValid: Boolean

    abstract val name: String

    abstract val videoServer: VideoServer?

    val verified: VideoServer?
        get() {
            if (server == null)
                server = verify(videoServer)
            return server
        }

    protected suspend fun getFinishedHtml(link: String): String {
        return suspendCoroutine {
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                }
                addJavascriptInterface(object : ZippyServer.ZippyJSInterface() {
                    @JavascriptInterface
                    override fun printHtml(string: String) {
                        it.resume(string)
                    }
                }, "HtmlViewer")
                val handler = Handler(Looper.getMainLooper())
                var isExecuted = false
                val runnable = Runnable {
                    if (!isExecuted) {
                        isExecuted = true
                        loadUrl(
                            "javascript:window.HtmlViewer.printHtml" +
                                    "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"
                        )
                    }
                }
                handler.postDelayed(runnable, 5000)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        handler.removeCallbacks(runnable)
                        runnable.run()
                    }
                }
                loadUrl(link)
            }
        }
    }

    private fun verify(videoServer: VideoServer?): VideoServer? {
        if (videoServer == null)
            return null
        if (videoServer.skipVerification) return videoServer
        for (option in ArrayList(videoServer.options))
            try {
                val request = Request.Builder()
                    .url(option.url ?: "")
                if (option.headers != null)
                    for (pair in option.headers?.createHeaders() ?: arrayListOf())
                        request.addHeader(pair.first, pair.second)
                val response = NoSSLOkHttpClient.get().newCall(request.build()).execute()
                if (!response.isSuccessful) {
                    Log.e("Remove Option", "Server: " + option.server + "\nUrl: " + option.url + "\nCode: " + response.code)
                    videoServer.options.remove(option)
                }
                if (response.body != null)
                    response.close()
            } catch (e: Exception) {
                e.printStackTrace()
                videoServer.options.remove(option)
            }

        return if (videoServer.options.size == 0) null else videoServer
    }

    override fun compareTo(other: Server): Int {
        return name.compareTo(other.name)
    }

    companion object {
        const val TIMEOUT = 10000L

        private fun getServers(context: Context, base: String): List<Server> {
            return listOf(
                FireServer(context, base),
                NatsukiServer(context, base),
                GoCDNServer(context, base),
                StapeServer(context, base),
                SBServer(context, base),
                VeryStreamServer(context, base),
                FembedServer(context, base),
                FenixServer(context, base),
                HyperionServer(context, base),
                IzanagiServer(context, base),
                MangoServer(context, base),
                MegaServer(context, base),
                OkruServer(context, base),
                RVServer(context, base),
                ZippyServer(context, base),
                    YUServer(context, base),
                    MP4UploadServer(context, base)
            )
        }

        fun check(context: Context, base: String): Server? {
            for (server in getServers(context, base)) {
                if (server.isValid)
                    return server
            }
            return null
        }

        fun getNames(servers: MutableList<Server>): MutableList<String> {
            val names = ArrayList<String>()
            for (server in servers) {
                names.add(server.name)
            }
            return names
        }
    }
}
