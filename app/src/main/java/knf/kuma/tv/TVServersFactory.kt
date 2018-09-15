package knf.kuma.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.leanback.widget.Presenter
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import knf.kuma.commons.BypassUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.tv.exoplayer.TVPlayer
import knf.kuma.tv.streaming.TVServerSelection
import knf.kuma.tv.streaming.TVServerSelectionFragment
import knf.kuma.videoservers.Option
import knf.kuma.videoservers.Server
import knf.kuma.videoservers.VideoServer
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import xdroid.toaster.Toaster
import java.util.*

class TVServersFactory private constructor(private val activity: Activity, private val url: String, private val chapter: AnimeObject.WebInfo.AnimeChapter, val viewHolder: Presenter.ViewHolder, private val serversInterface: ServersInterface) {
    private val downloadObject: DownloadObject = DownloadObject.fromChapter(chapter, false)

    private var servers: MutableList<Server> = ArrayList()

    private var current: VideoServer? = null

    fun showServerList() {
        launch(UI) {
            try {
                if (servers.isEmpty()) {
                    Toaster.toast("Sin servidores disponibles")
                    serversInterface.onFinish(false, false)
                } else {
                    val bundle = Bundle()
                    bundle.putStringArrayList(TVServerSelectionFragment.SERVERS_DATA, Server.getNames(servers) as ArrayList<String>)
                    activity.startActivityForResult(Intent(activity, TVServerSelection::class.java)
                            .putExtras(bundle), REQUEST_CODE_LIST)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        if (current != null)
            startStreaming(current!!.options[position])
    }

    private fun showOptions(server: VideoServer) {
        this.current = server
        val bundle = Bundle()
        bundle.putStringArrayList(TVServerSelectionFragment.VIDEO_DATA, Option.getNames(server.options) as ArrayList<String>)
        bundle.putString("name", server.name)
        activity.startActivityForResult(Intent(activity, TVServerSelection::class.java)
                .putExtras(bundle), REQUEST_CODE_OPTION)
    }

    private fun startStreaming(option: Option) {
        CacheDB.INSTANCE.chaptersDAO().addChapter(chapter)
        CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(chapter))
        Answers.getInstance().logCustom(CustomEvent("Streaming").putCustomAttribute("Server", option.server))
        activity.startActivity(Intent(activity, TVPlayer::class.java)
                .putExtra("url", option.url)
                .putExtra("title", downloadObject.name)
                .putExtra("chapter", downloadObject.chapter))
        serversInterface.onFinish(false, true)
    }

    fun get() {
        try {
            Log.e("Url", url)
            val main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(activity)).userAgent(BypassUtil.userAgent).get()
            val downloads = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download")
            val servers = ArrayList<Server>()
            for (e in downloads) {
                var z = e.attr("href")
                z = z.substring(z.lastIndexOf("http"))
                val server = Server.check(activity, z)
                if (server != null)
                    servers.add(server)
            }
            val sScript = main.select("script")
            var j = ""
            for (element in sScript) {
                val sEl = element.outerHtml()
                if (sEl.contains("var video = [];")) {
                    j = sEl
                    break
                }
            }
            val parts = j.substring(j.indexOf("var video = [];") + 14, j.indexOf("$(document).ready(function()")).split("video\\[[^a-z]*]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (baseLink in parts) {
                val server = Server.check(activity, baseLink)
                if (server != null)
                    servers.add(server)
            }
            servers.sort()
            this.servers = servers
            showServerList()
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

        fun start(activity: Activity, url: String, chapter: AnimeObject.WebInfo.AnimeChapter, serversInterface: ServersInterface) {
            start(activity, url, chapter, null, serversInterface)
        }

        fun start(activity: Activity, url: String, chapter: AnimeObject.WebInfo.AnimeChapter, viewHolder: Presenter.ViewHolder?, serversInterface: ServersInterface) {
            doAsync {
                val factory = TVServersFactory(activity, url, chapter, viewHolder!!, serversInterface)
                serversInterface.onReady(factory)
                factory.get()
            }
        }
    }
}
