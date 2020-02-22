package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit

abstract class Server(internal var context: Context, internal var baseLink: String) : Comparable<Server> {
    internal var TIMEOUT = 10000
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

    private fun verify(videoServer: VideoServer?): VideoServer? {
        if (videoServer == null)
            return null
        if (videoServer.skipVerification) return videoServer
        val client = OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true).build()
        for (option in ArrayList(videoServer.options))
            try {
                val request = Request.Builder()
                        .url(option.url ?: "")
                if (option.headers != null)
                    for (pair in option.headers?.createHeaders() ?: arrayListOf())
                        request.addHeader(pair.first, pair.second)
                val response = client.newCall(request.build()).execute()
                if (!response.isSuccessful) {
                    Log.e("Remove Option", "Server: " + option.server + "\nUrl: " + option.url + "\nCode: " + response.code())
                    videoServer.options.remove(option)
                }
                if (response.body() != null)
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

        private fun getServers(context: Context, base: String): List<Server> {
            return listOf(
                    FireServer(context, base),
                    NatsukiServer(context, base),
                    VeryStreamServer(context, base),
                    FembedServer(context, base),
                    FenixServer(context, base),
                    HyperionServer(context, base),
                    IzanagiServer(context, base),
                    MangoServer(context, base),
                    MegaServer(context, base),
                    OkruServer(context, base),
                    RVServer(context, base),
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
