package knf.kuma.tv

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.Presenter
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.iterator
import knf.kuma.database.CacheDB
import knf.kuma.player.openWebPlayer
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeenObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.pojos.av1.Record
import knf.kuma.tv.exoplayer.TVPlayer
import knf.kuma.tv.streaming.TVMultiSelection
import knf.kuma.tv.streaming.TVServerSelection
import knf.kuma.tv.streaming.TVServerSelectionFragment
import knf.kuma.videoservers.Option
import knf.kuma.videoservers.Server
import knf.kuma.videoservers.VideoServer
import knf.kuma.videoservers.WebServer
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.util.Locale


class TVServersFactory private constructor(
    private val activity: FragmentActivity,
    private val url: String,
    private val name: String,
    private val chapter: String,
    private val record: Record,
    val viewHolder: Presenter.ViewHolder?,
    private val serversInterface: ServersInterface
) {
    private var servers: MutableList<Server> = ArrayList()
    private var subServes: List<Server> = emptyList()
    private var dubServers: List<Server> = emptyList()

    private var current: VideoServer? = null

    fun showServerList() {
        doOnUIGlobal {
            try {
                if (servers.isEmpty()) {
                    Toaster.toast("Sin servidores disponibles")
                    serversInterface.onFinish(false, false)
                } else {
                    activity.startActivityForResult(
                        Intent(activity, TVServerSelection::class.java)
                            .putExtra(TVServerSelectionFragment.SERVERS_DATA, Server.getNames(servers) as ArrayList),
                        REQUEST_CODE_LIST
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun analyzeMulti(position: Int) {
        when(position) {
            0 -> this.servers = subServes.toMutableList()
            1 -> this.servers = dubServers.toMutableList()
        }
        showServerList()
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
                } else if (servers[position] is WebServer) {
                    try {
                        openWebPlayer(activity, server.option.url!!, name)
                        doAsync {
                            CacheDB.INSTANCE.recordAV1DAO().addChapter(record)
                            syncData {
                                history()
                            }
                        }
                        serversInterface.onFinish(false, true)
                    } catch (_: Exception) {
                        Toaster.toast("Error al abrir explorador web")
                        showServerList()
                    }

                } else {
                    val serverName = text.lowercase(Locale.getDefault())
                    when {
                         serverName.contains("mega") -> {
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
        activity.startActivityForResult(
            Intent(activity, TVServerSelection::class.java)
                .putExtra("name", server.name)
                .putExtra(
                    TVServerSelectionFragment.VIDEO_DATA, (server.options.map { it.name ?: "" } as? ArrayList)
                        ?: arrayListOf<String>()
                ),
            REQUEST_CODE_OPTION
        )
    }

    private fun startStreaming(option: Option) {
        doAsync {
            CacheDB.INSTANCE.recordAV1DAO().addChapter(record)
            syncData {
                history()
            }
        }
        activity.startActivity(Intent(activity, TVPlayer::class.java).apply {
            setDataAndType(Uri.parse(option.url), "video/*")
            putExtra("title", name)
            putExtra("chapter", chapter)
            putStringArrayListExtra("headers", ArrayList(option.headers?.createHeadersList()?: emptyList()))
        })
        serversInterface.onFinish(false, true)
    }

    fun get() {
        activity.lifecycleScope.launch {
            try {
                val response = JsExtractor.processLinkMultiple(url, listOf("embeds", "downloads"))
                val subServers = mutableListOf<Server>()
                val dubServers = mutableListOf<Server>()
                response.forEach { (_, jSONArray) ->
                    jSONArray?.getJSONObject(0)?.let {
                        if (it.has("SUB")) {
                            for (sub in it.getJSONArray("SUB")) {
                                val name = sub.getString("server")
                                val url = sub.getString("url")
                                if (name == "MP4Upload") {
                                    if (subServers.find { it.baseLink.contains(url.substringAfterLast("/")) } != null){
                                        continue
                                    }
                                }
                                if (name == "PDrain") {
                                    if (subServers.find { it.baseLink.substringBeforeLast("?").contains(url.substringBeforeLast("?")) } != null){
                                        continue
                                    }
                                }
                                if (name == "1Fichier") {
                                    continue
                                }
                                val server = Server.check(activity, url)
                                if (subServers.find { it.baseLink.substringAfterLast("/") == url.substringAfterLast("/") } == null) {
                                    subServers.add(server?: WebServer(activity, url, name))
                                }
                            }
                        }
                        if (it.has("DUB")) {
                            for (dub in it.getJSONArray("DUB")) {
                                val name = dub.getString("server")
                                val url = dub.getString("url")
                                if (name == "MP4Upload") {
                                    if (dubServers.find { it.baseLink.contains(url.substringAfterLast("/")) } != null){
                                        continue
                                    }
                                }
                                if (name == "PDrain") {
                                    if (dubServers.find { it.baseLink.substringBeforeLast("?").contains(url.substringBeforeLast("?")) } != null){
                                        continue
                                    }
                                }
                                if (name == "1Fichier") {
                                    continue
                                }
                                val server = Server.check(activity, url)
                                if (dubServers.find { it.baseLink.substringAfterLast("/") == url.substringAfterLast("/") } == null) {
                                    dubServers.add(server?: WebServer(activity, url, name))
                                }
                            }
                        }
                    }
                }
                this@TVServersFactory.subServes = subServers.sortedWith(
                    compareBy(
                        { it.name.contains("(WEB)") },
                        {it.name}
                    )
                ).filter { it is WebServer || it.canStream }.toMutableList()
                this@TVServersFactory.dubServers = dubServers.sortedWith(
                    compareBy(
                        { it.name.contains("(WEB)") },
                        {it.name}
                    )
                ).filter { it is WebServer || it.canStream }.toMutableList()
                if (dubServers.isNotEmpty()) {
                    activity.startActivityForResult(
                        Intent(activity, TVMultiSelection::class.java),
                        REQUEST_CODE_MULTI
                    )
                } else {
                    this@TVServersFactory.servers = subServers.toMutableList()
                    showServerList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
                this@TVServersFactory.servers = ArrayList()
                serversInterface.onFinish(false, false)
            }
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

        fun start(activity: FragmentActivity, url: String, chapter: RecentAV1, serversInterface: ServersInterface) {
            start(activity, url, chapter.name, chapter.chapter, chapter.asRecord(), null, serversInterface)
        }

        fun start(activity: FragmentActivity, url: String, name: String, chapter: String, record: Record, viewHolder: Presenter.ViewHolder?, serversInterface: ServersInterface?) {
            doAsync {
                serversInterface?.let {
                    val factory = TVServersFactory(activity, url, name, chapter, record, viewHolder, it)
                    serversInterface.onReady(factory)
                    factory.get()
                }
            }
        }
    }
}
