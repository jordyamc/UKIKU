package knf.kuma.videoservers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.android.material.snackbar.Snackbar
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.download.DownloadService
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.QueueObject
import knf.kuma.queue.QueueManager
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import xdroid.toaster.Toaster
import java.util.*

class ServersFactory {
    private var context: Context
    private var url: String
    private var chapter: AnimeObject.WebInfo.AnimeChapter? = null
    private var downloadObject: DownloadObject
    private var isStream: Boolean = false
    private var isCasting: Boolean = false
    private var serversInterface: ServersInterface
    private var snackbar: Snackbar? = null
    private var servers: MutableList<Server> = ArrayList()
    private var selected = 0

    private constructor(context: Context, url: String, chapter: AnimeObject.WebInfo.AnimeChapter, isStream: Boolean, addQueue: Boolean, serversInterface: ServersInterface) {
        this.context = context
        this.url = url
        this.chapter = chapter
        this.downloadObject = DownloadObject.fromChapter(chapter, addQueue)
        this.isStream = isStream
        this.isCasting = isStream && CastUtil.get().connected()
        this.serversInterface = serversInterface
    }

    private constructor(context: Context, url: String, downloadObject: DownloadObject, isStream: Boolean, serversInterface: ServersInterface) {
        this.context = context
        this.url = url
        this.downloadObject = downloadObject
        this.isStream = isStream
        this.isCasting = isStream && CastUtil.get().connected()
        this.serversInterface = serversInterface
    }

    private fun showServerList() {
        doOnUI {
            try {
                if (servers.size == 0) {
                    Toaster.toast("Sin servidores disponibles")
                    callOnFinish(false, false)
                } else {
                    dismissSnack()
                    MaterialDialog(this@ServersFactory.context).safeShow {
                        title(text = "Selecciona servidor")
                        listItemsSingleChoice(items = Server.getNames(servers), initialSelection = selected) { _, index, text ->
                            selected = index
                            doAsync {
                                try {
                                    showSnack("Obteniendo link...")
                                    val server = servers[selected].verified
                                    dismissSnack()
                                    if (server == null && servers.size == 1) {
                                        Toaster.toast("Error en servidor, intente mas tarde")
                                        callOnFinish(false, false)
                                    } else if (server == null) {
                                        servers.removeAt(selected)
                                        selected = 0
                                        Toaster.toast("Error en servidor")
                                        showServerList()
                                    } else if (server.options.size == 0) {
                                        servers.removeAt(selected)
                                        selected = 0
                                        Toaster.toast("Error en servidor")
                                        showServerList()
                                    } else if (server.haveOptions()) {
                                        showOptions(server, isCasting)
                                    } else {
                                        when (text.toLowerCase()) {
                                            "mega 1", "mega 2" -> {
                                                this@ServersFactory.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(server.option.url)))
                                                callOnFinish(false, false)
                                            }
                                            else ->
                                                when {
                                                    isCasting -> callOnCast(server.option.url)
                                                    isStream -> startStreaming(server.option)
                                                    else -> startDownload(server.option)
                                                }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        positiveButton(text =
                        when {
                            downloadObject.addQueue -> "AÑADIR"
                            isCasting -> "CAST"
                            else -> "INICIAR"
                        })
                        negativeButton(text = "CANCELAR") { callOnFinish(false, false) }
                        setOnCancelListener { callOnFinish(false, false) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun showOptions(server: VideoServer, isCast: Boolean) {
        doOnUI {
            try {
                MaterialDialog(this@ServersFactory.context).safeShow {
                    title(text = server.name)
                    listItemsSingleChoice(items = Option.getNames(server.options), initialSelection = 0) { _, index, _ ->
                        when {
                            isCast -> callOnCast(server.options[index].url)
                            isStream -> startStreaming(server.options[index])
                            else -> startDownload(server.options[index])
                        }
                    }
                    positiveButton(text =
                    when {
                        downloadObject.addQueue -> "AÑADIR"
                        isCasting -> "CAST"
                        else -> "INICIAR"
                    })
                    negativeButton(text = "ATRAS") { showServerList() }
                    setOnCancelListener { showServerList() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun start() {
        try {
            serversInterface.onProgressIndicator(true)
            showSnack("Obteniendo servidores...")
            val main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get()
            val downloads = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download")
            val servers = ArrayList<Server>()
            for (e in downloads) {
                var z = e.attr("href")
                z = z.substring(z.lastIndexOf("http"))
                val server = Server.check(context, z)
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
                val server = Server.check(context, baseLink)
                if (server != null)
                    servers.add(server)
            }
            servers.sort()
            this@ServersFactory.servers = servers
            showServerList()
        } catch (e: Exception) {
            e.printStackTrace()
            this@ServersFactory.servers = ArrayList()
            callOnFinish(false, false)
        }
    }

    private fun startStreaming(option: Option) {
        if (chapter != null && downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, chapter!!)
        } else {
            Answers.getInstance().logCustom(CustomEvent("Streaming").putCustomAttribute("Server", option.server))
            AchievementManager.onPlayChapter()
            try {
                if (PreferenceManager.getDefaultSharedPreferences(App.context).getString("player_type", "0") == "0") {
                    App.context.startActivity(
                            PrefsUtil.getPlayerIntent()
                                    .setData(Uri.parse(option.url))
                                    .putExtra("title", downloadObject.title)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(option.url), "video/mp4")
                            .putExtra("title", downloadObject.title)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.context.startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                App.context.startActivity(
                        PrefsUtil.getPlayerIntent()
                                .setData(Uri.parse(option.url))
                                .putExtra("title", downloadObject.title)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        callOnFinish(false, true)
    }

    private fun startDownload(option: Option) {
        if (BuildConfig.DEBUG) Log.e("Download " + option.server, option.url)
        if (chapter != null && CacheDB.INSTANCE.queueDAO().isInQueue(chapter!!.eid))
            CacheDB.INSTANCE.queueDAO().add(QueueObject(Uri.fromFile(FileAccessHelper.INSTANCE.getFile(chapter!!.fileName)), true, chapter!!))
        Answers.getInstance().logCustom(CustomEvent("Download").putCustomAttribute("Server", option.server))
        downloadObject.link = option.url
        downloadObject.headers = option.headers
        if (PrefsUtil.downloaderType == 0) {
            CacheDB.INSTANCE.downloadsDAO().insert(downloadObject)
            ContextCompat.startForegroundService(App.context, Intent(App.context, DownloadService::class.java).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)))
            callOnFinish(true, true)
        } else
            callOnFinish(true, DownloadManager.start(downloadObject))
    }

    private fun callOnFinish(started: Boolean, success: Boolean) {
        serversInterface.onProgressIndicator(false)
        dismissSnack()
        serversInterface.onFinish(started, success)
        INSTANCE = null
    }

    private fun callOnCast(url: String?) {
        serversInterface.onProgressIndicator(false)
        dismissSnack()
        serversInterface.onCast(url)
        INSTANCE = null
    }

    private fun showSnack(text: String) {
        dismissSnack()
        snackbar = serversInterface.getView()?.showSnackbar(text, duration = Snackbar.LENGTH_INDEFINITE)
    }

    private fun dismissSnack() {
        snackbar?.dismiss()
    }

    interface ServersInterface {
        fun onFinish(started: Boolean, success: Boolean)

        fun onCast(url: String?)

        fun onProgressIndicator(boolean: Boolean)

        fun getView(): View?
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: ServersFactory? = null

        private fun isRunning(): Boolean = INSTANCE != null

        fun start(
                context: Context,
                url: String,
                chapter: AnimeObject.WebInfo.AnimeChapter,
                isStream: Boolean = false,
                addQueue: Boolean = false,
                serversInterface: ServersInterface
        ) {
            if (!isRunning())
                INSTANCE = ServersFactory(context, url, chapter, isStream, addQueue, serversInterface).also { doAsync { it.start() } }
            else {
                serversInterface.onFinish(false, false)
                Toaster.toast("Solo una petición a la vez")
            }
        }

        fun start(
                context: Context,
                url: String,
                downloadObject: DownloadObject,
                isStream: Boolean = false,
                serversInterface: ServersInterface
        ) {
            if (!isRunning())
                INSTANCE = ServersFactory(context, url, downloadObject, isStream, serversInterface).also { doAsync { it.start() } }
            else {
                serversInterface.onFinish(false, false)
                Toaster.toast("Solo una petición a la vez")
            }
        }

        fun clear() {
            INSTANCE = null
        }

        fun startPlay(context: Context, title: String, file_name: String) {
            AchievementManager.onPlayChapter()
            val file = FileAccessHelper.INSTANCE.getFile(file_name)
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0") {
                context.startActivity(PrefsUtil.getPlayerIntent()
                        .setData(Uri.fromFile(file))
                        .putExtra("isFile", true)
                        .putExtra("title", title))
            } else {
                val intent = Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                        .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra("title", title)
                context.startActivity(intent)
            }
        }

        private fun getEpTitle(title: String, file: String): String {
            return title + " " + file.substring(file.lastIndexOf("-") + 1, file.lastIndexOf("."))
        }

        fun getPlayIntent(context: Context, title: String, file_name: String): PendingIntent {
            val file = FileAccessHelper.INSTANCE.getFile(file_name)
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0") {
                PendingIntent.getActivity(context, Math.abs(file_name.hashCode()),
                        PrefsUtil.getPlayerIntent()
                                .setData(Uri.fromFile(file)).putExtra("isFile", true)
                                .putExtra("title", getEpTitle(title, file_name))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                        .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("title", getEpTitle(title, file_name))
                PendingIntent.getActivity(context, Math.abs(file_name.hashCode()), intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
    }
}
