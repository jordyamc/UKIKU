package knf.kuma.tv

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.leanback.widget.Presenter
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.doOnUI
import knf.kuma.commons.iterator
import knf.kuma.commons.jsoupCookies
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeenObject
import knf.kuma.tv.exoplayer.TVPlayer
import knf.kuma.tv.streaming.TVMultiSelection
import knf.kuma.tv.streaming.TVServerSelection
import knf.kuma.tv.streaming.TVServerSelectionFragment
import knf.kuma.videoservers.Option
import knf.kuma.videoservers.Server
import knf.kuma.videoservers.VideoServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import xdroid.toaster.Toaster
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVServersFactory private constructor(private val activity: Activity, private val url: String, private val chapter: AnimeObject.WebInfo.AnimeChapter, val viewHolder: Presenter.ViewHolder?, private val serversInterface: ServersInterface) {
    private val downloadObject: DownloadObject = DownloadObject.fromChapter(chapter, false)

    private var jsonObject: JSONObject? = null
    private var servers: MutableList<Server> = ArrayList()

    private var current: VideoServer? = null

    fun showServerList() {
        doOnUI {
            try {
                if (servers.isEmpty()) {
                    Toaster.toast("Sin servidores disponibles")
                    serversInterface.onFinish(false, false)
                } else {
                    activity.startActivityForResult(Intent(activity, TVServerSelection::class.java)
                            .putExtra(TVServerSelectionFragment.SERVERS_DATA, Server.getNames(servers) as ArrayList),
                            REQUEST_CODE_LIST)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun analyzeMulti(position: Int) {
        doAsync {
            val main = jsoupCookies(url).get()
            val downloads = main.select("table.RTbl.Dwnl tr:contains(${if (position == 0) "SUB" else "LAT"}) a.Button.Sm.fa-download")
            for (e in downloads) {
                var z = e.attr("href")
                z = z.substring(z.lastIndexOf("http"))
                val server = Server.check(activity, z)
                if (server != null)
                    servers.add(server)
            }
            val jsonArray = jsonObject?.getJSONArray(if (position == 0) "SUB" else "LAT")
                    ?: JSONArray()
            for (baseLink in jsonArray) {
                val server = Server.check(activity, baseLink.optString("code"))
                if (server != null)
                    try {
                        var skip = false
                        servers.forEach {
                            if (it.name == server.name) {
                                skip = true
                                return@forEach
                            }
                        }
                        if (!skip)
                            servers.add(server)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
            }
            servers.sort()
            showServerList()
        }
    }

    fun analyzeServer(position: Int) {
        doAsync {
            try {
                val text = servers[position].name
                val server = servers[position].verified
                if (server == null && servers.size == 1) {
                    Toaster.toast("Error en servidor, intente mas tarde")
                    serversInterface.onFinish(false, false)
                } else if (server == null) {
                    Toaster.toast("Error en servidor")
                    showServerList()
                } else if (server.options.size == 0) {
                    Toaster.toast("Error en servidor")
                    showServerList()
                } else if (server.haveOptions()) {
                    showOptions(server)
                } else {
                    when (text.toLowerCase()) {
                        "mega" -> {
                            Toaster.toast("No se puede usar Mega en TV")
                            showServerList()
                        }
                        else -> startStreaming(server.option)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun analyzeOption(position: Int) {
        current?.let { startStreaming(it.options[position]) }
    }

    private fun showOptions(server: VideoServer) {
        this.current = server
        activity.startActivityForResult(Intent(activity, TVServerSelection::class.java)
                .putExtra("name", server.name)
                .putExtra(TVServerSelectionFragment.VIDEO_DATA, (Option.getNames(server.options) as? ArrayList)
                        ?: arrayListOf<String>()),
                REQUEST_CODE_OPTION)
    }

    private fun startStreaming(option: Option) {
        CacheDB.INSTANCE.seenDAO().addChapter(SeenObject.fromChapter(chapter))
        CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(chapter))
        syncData {
            history()
            seen()
        }
        activity.startActivity(Intent(activity, TVPlayer::class.java).apply {
            putExtra("url", option.url)
            putExtra("title", downloadObject.name)
            putExtra("chapter", downloadObject.chapter)
            putExtra("headers", option.headers?.createHeadersMap())
        })
        serversInterface.onFinish(false, true)
    }

    fun get() {
        try {
            Log.e("Url", url)
            val main = jsoupCookies(url).get()
            val servers = ArrayList<Server>()
            val sScript = main.select("script")
            var j = ""
            for (element in sScript) {
                val sEl = element.outerHtml()
                if ("\\{\"[SUBLAT]+\":\\[.*\\]\\}".toRegex().containsMatchIn(sEl)) {
                    j = sEl
                    break
                }
            }
            jsonObject = JSONObject("\\{\"[SUBLAT]+\":\\[.*\\]\\}".toRegex().find(j)?.value)
            if (jsonObject?.length() ?: 0 > 1) {
                this.servers = servers
                activity.startActivityForResult(Intent(activity, TVMultiSelection::class.java),
                        REQUEST_CODE_MULTI)
            } else {
                val downloads = main.select("table.RTbl.Dwnl tr:contains(SUB) a.Button.Sm.fa-download")
                for (e in downloads) {
                    var z = e.attr("href")
                    z = z.substring(z.lastIndexOf("http"))
                    val server = Server.check(activity, z)
                    if (server != null)
                        servers.add(server)
                }
                val jsonArray = jsonObject?.getJSONArray("SUB") ?: JSONArray()
                for (baseLink in jsonArray) {
                    val server = Server.check(activity, baseLink.optString("code"))
                    if (server != null)
                        servers.add(server)
                }
                servers.sort()
                this.servers = servers
                showServerList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            this.servers = ArrayList()
            serversInterface.onFinish(false, false)
        }

    }

    interface ServersInterface {
        fun onReady(serversFactory: TVServersFactory)

        fun onFinish(started: Boolean, success: Boolean)
    }

    companion object {
        var REQUEST_CODE_LIST = 4456
        var REQUEST_CODE_OPTION = 6157
        var REQUEST_CODE_MULTI = 6497

        fun start(activity: Activity, url: String, chapter: AnimeObject.WebInfo.AnimeChapter, serversInterface: ServersInterface) {
            start(activity, url, chapter, null, serversInterface)
        }

        fun start(activity: Activity, url: String, chapter: AnimeObject.WebInfo.AnimeChapter, viewHolder: Presenter.ViewHolder?, serversInterface: ServersInterface?) {
            doAsync {
                serversInterface?.let {
                    val factory = TVServersFactory(activity, url, chapter, viewHolder, it)
                    serversInterface.onReady(factory)
                    factory.get()
                }
            }
        }
    }
}
