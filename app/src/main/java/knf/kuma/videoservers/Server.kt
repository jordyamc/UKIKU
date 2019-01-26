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
        val client = OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true).build()
        for (option in ArrayList(videoServer.options))
            try {
                val request = Request.Builder()
                        .url(option.url ?: "")
                if (option.headers != null)
                    for (pair in option.headers?.headers ?: arrayListOf())
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

        private fun getServers(context: Context, base: String): MutableList<Server> {
            return Arrays.asList(
                    FireServer(context, base),
                    NatsukiServer(context, base),
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

        private fun findPosition(servers: MutableList<Server>, name: String): Int {
            for ((i, server) in servers.withIndex()) {
                if (server.name == name)
                    return i
            }
            return 0
        }

        fun existServer(servers: MutableList<Server>, position: Int): Boolean {
            val name = VideoServer.Names.downloadServers[position - 1]
            for (server in servers) {
                if (server.name == name)
                    return true
            }
            return false
        }

        fun findServer(servers: MutableList<Server>, position: Int): Server {
            val name = VideoServer.Names.downloadServers[position - 1]
            return servers[findPosition(servers, name)]
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
