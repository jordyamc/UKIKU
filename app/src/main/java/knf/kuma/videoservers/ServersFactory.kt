package knf.kuma.videoservers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.achievements.AchievementManager
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.custom.exceptions.EJNFException
import knf.kuma.custom.snackbar.SnackProgressBarManager
import knf.kuma.database.CacheDB
import knf.kuma.download.*
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.QueueObject
import knf.kuma.queue.QueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.net.URLDecoder
import java.util.*
import kotlin.math.abs


class ServersFactory {
    private var context: Context
    private var url: String
    private var chapter: AnimeObject.WebInfo.AnimeChapter? = null
    private var downloadObject: DownloadObject
    private var isStream: Boolean = false
    private var isCasting: Boolean = false
    private var serversInterface: ServersInterface
    private var snackBarManager: SnackProgressBarManager? = null
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

    private fun saveLastServer(name: String) {
        PrefsUtil.lastServer = name
    }

    private fun processSelectedServer(index: Int, text: String, showName: Boolean = false) {
        selected = index
        doAsync {
            try {
                showSnack("Obteniendo link${if (showName) " $text" else ""}...")
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
                    saveLastServer(text)
                    when (text.lowercase(Locale.getDefault())) {
                        "mega d", "mega s" -> {
                            try {
                                CustomTabsIntent.Builder()
                                    .setToolbarColor(Color.parseColor("#DA252D"))
                                    .setShowTitle(true).build()
                                    .launchUrl(context, Uri.parse(server.option.url))
                            } catch (e: Exception) {
                                this@ServersFactory.context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(server.option.url)
                                    )
                                )
                            }
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

    private fun showServerList(useLast: Boolean = true) {
        doOnUI {
            try {
                if (servers.size == 0) {
                    Toaster.toast("Sin servidores disponibles")
                    callOnFinish(false, false)
                } else {
                    dismissSnack()
                    val names = Server.getNames(servers)
                    val lasServer = PrefsUtil.lastServer
                    if (PrefsUtil.rememberServer && lasServer != null && names.contains(lasServer) && useLast)
                        processSelectedServer(names.indexOf(lasServer), lasServer, true)
                    else
                        MaterialDialog(this@ServersFactory.context).safeShow {
                            title(text = "Selecciona servidor")
                            listItemsSingleChoice(items = names, initialSelection = selected) { _, index, text ->
                                processSelectedServer(index, text.toString())
                            }
                            checkBoxPrompt(text = "Recordar selección", isCheckedDefault = PrefsUtil.rememberServer) {
                                PrefsUtil.rememberServer = it
                                if (!it) PrefsUtil.lastServer = null
                            }
                            positiveButton(text =
                            when {
                                downloadObject.addQueue -> "AÑADIR"
                                isCasting -> "CAST"
                                else -> "INICIAR"
                            })
                            negativeButton(text = "CANCELAR") {
                                callOnFinish(false, false)
                                if (PrefsUtil.lastServer.isNull()) PrefsUtil.rememberServer = false
                            }
                            setOnCancelListener {
                                callOnFinish(false, false)
                                if (PrefsUtil.lastServer.isNull()) PrefsUtil.rememberServer = false
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al mostrar lista de servidores")
                callOnFinish(false, false)
            }
        }

    }

    private fun showOptions(server: VideoServer, isCast: Boolean) {
        doOnUI {
            try {
                MaterialDialog(this@ServersFactory.context).safeShow {
                    title(text = server.name)
                    listItemsSingleChoice(items = Option.getNames(server.options), initialSelection = 0) { _, index, _ ->
                        saveLastServer(server.name)
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
                    negativeButton(text = "ATRAS") { showServerList(false) }
                    setOnCancelListener { showServerList(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al mostrar lista de opciones")
                showServerList(false)
            }
        }
    }

    fun start() {
        try {
            serversInterface.onProgressIndicator(true)
            showSnack("Obteniendo servidores...")
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
            val jsonObject = JSONObject("\\{\"[SUBLAT]+\":\\[.*\\]\\}".toRegex().find(j)?.value
                    ?: throw EJNFException())
            if (jsonObject.length() > 1) {
                doOnUI {
                    MaterialDialog(context).safeShow {
                        listItems(items = listOf("Subtitulado", "Latino")) { _, index, _ ->
                            doAsync {
                                val downloads = main.select("table.RTbl.Dwnl tr:contains(${if (index == 0) "SUB" else "LAT"}) a.Button.Sm.fa-download")
                                for (e in downloads) {
                                    var z = e.attr("href")
                                    z = z.substring(z.lastIndexOf("http"))
                                    val server = Server.check(context, z)
                                    if (server != null)
                                        servers.add(server)
                                }
                                val jsonArray =
                                        when (index) {
                                            1 -> jsonObject.getJSONArray("LAT")
                                            else -> jsonObject.getJSONArray("SUB")
                                        }
                                for (baseLink in jsonArray) {
                                    val server = Server.check(context, baseLink.optString("code"))
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
                                this@ServersFactory.servers = servers
                                showServerList()
                            }
                        }
                        setOnCancelListener { callOnFinish(false, false) }
                    }
                }
            } else {
                val downloads = main.select("table.RTbl.Dwnl tr:contains(SUB) a.Button.Sm.fa-download")
                for (e in downloads) {
                    var z = e.attr("href")
                    z = URLDecoder.decode(z.substring(z.lastIndexOf("http")), "utf-8")
                    val server = Server.check(context, z)
                    if (server != null)
                        servers.add(server)
                }
                val jsonArray = jsonObject.getJSONArray(if (jsonObject.has("SUB")) "SUB" else "LAT")
                for (baseLink in jsonArray) {
                    val server = Server.check(context, baseLink.optString("code"))
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
                this@ServersFactory.servers = servers
                showServerList()
            }
        } catch (e: EJNFException) {
            e.printStackTrace()
            this@ServersFactory.servers = ArrayList()
            Toaster.toast("Sin json de capitulos")
            callOnFinish(false, false)
        } catch (e: Exception) {
            e.printStackTrace()
            this@ServersFactory.servers = ArrayList()
            callOnFinish(false, false)
        }
    }

    private fun startStreaming(option: Option) {
        if (chapter != null && downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, chapter)
        } else {
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
        doAsync{
            if (BuildConfig.DEBUG) Log.e("Download " + option.server, "${option.url}")
            downloadObject.server = option.server ?: ""
            if (chapter != null && CacheDB.INSTANCE.queueDAO().isInQueue(chapter?.eid ?: "0")) {
                CacheDB.INSTANCE.queueDAO().add(QueueObject(Uri.fromFile(FileAccessHelper.getFile(chapter?.fileName
                        ?: "null")), true, chapter))
                syncData { queue() }
            }
            downloadObject.link = option.url
            downloadObject.headers = option.headers
            if (PrefsUtil.downloaderType == 0) {
                CacheDB.INSTANCE.downloadsDAO().insert(downloadObject)
                doOnUI {
                    context.service(Intent(App.context, DownloadService::class.java).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)))
                    callOnFinish(true, true)
                }
            } else
                GlobalScope.launch(Dispatchers.Main) {
                    callOnFinish(true, withContext(Dispatchers.IO) { DownloadManager.start(downloadObject) })
                }
        }
    }

    private fun callOnFinish(started: Boolean, success: Boolean) {
        serversInterface.onProgressIndicator(false)
        dismissSnack()
        clear()
        serversInterface.onFinish(started, success)
    }

    private fun callOnCast(url: String?) {
        serversInterface.onProgressIndicator(false)
        dismissSnack()
        clear()
        serversInterface.onCast(url)
    }

    private fun getSnackManager(): SnackProgressBarManager? {
        val view = serversInterface.getView() ?: return null
        return snackBarManager ?: SnackProgressBarManager(view)
                .setProgressBarColor(EAHelper.getThemeColor())
                .setOverlayLayoutAlpha(0.4f)
                .setOverlayLayoutColor(android.R.color.background_dark).also { snackBarManager = it }
    }

    private fun showSnack(text: String) {
        dismissSnack()
        //snackbar = serversInterface.getView()?.showSnackbar(text, duration = Snackbar.LENGTH_INDEFINITE)
        getSnackManager()?.showProgressSnackbar(text, SnackProgressBarManager.LENGTH_INDEFINITE)
    }

    private fun dismissSnack() {
        //snackbar?.dismiss()
        getSnackManager()?.dismissAll()
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
                GlobalScope.launch(Dispatchers.Main) {
                    INSTANCE = if (isStream)
                        ServersFactory(context, url, chapter, isStream, addQueue, serversInterface).also { doAsync { it.start() } }
                    else {
                        if (!FileAccessHelper.isStoragePermissionEnabledAsync()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || PrefsUtil.downloadType == "1")
                                FileAccessHelper.openTreeChooser(context)
                            else
                                Toaster.toastLong("¡Se necesita permiso de almacenamiento!")
                            serversInterface.onFinish(false, false)
                            return@launch
                        }
                        if (!MultipleDownloadManager.isSpaceAvailable(1)) {
                            serversInterface.getView()?.showSnackbar("Sin espacio suficiente")
                            serversInterface.onFinish(false, false)
                            return@launch
                        }
                        ServersFactory(context, url, chapter, isStream, addQueue, serversInterface).also { doAsync { it.start() } }
                    }
                }
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
                GlobalScope.launch(Dispatchers.Main) {
                    INSTANCE = if (isStream)
                        ServersFactory(context, url, downloadObject, isStream, serversInterface).also { doAsync { it.start() } }
                    else {
                        if (!FileAccessHelper.isStoragePermissionEnabledAsync()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || PrefsUtil.downloadType == "1")
                                FileAccessHelper.openTreeChooser(context)
                            else
                                Toaster.toastLong("¡Se necesita permiso de almacenamiento!")
                            serversInterface.onFinish(false, false)
                            return@launch
                        }
                        if (!MultipleDownloadManager.isSpaceAvailable(1)) {
                            serversInterface.getView()?.showSnackbar("Sin espacio suficiente")
                            serversInterface.onFinish(false, false)
                            return@launch
                        }
                        ServersFactory(context, url, downloadObject, isStream, serversInterface).also { doAsync { it.start() } }
                    }
                }
            else {
                serversInterface.onFinish(false, false)
                Toaster.toast("Solo una petición a la vez")
            }
        }

        fun clear() {
            INSTANCE?.snackBarManager?.dismissAll()
            INSTANCE = null
        }

        fun startPlay(context: Context?, title: String, file_name: String) {
            if (context == null) return
            Log.e("Video", "On play")
            AchievementManager.onPlayChapter()
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0") {
                context.startActivity(PrefsUtil.getPlayerIntent()
                    .setData(FileAccessHelper.getDataUri(file_name))
                        .putExtra("isFile", true)
                        .putExtra("title", title))
            } else {
                val intent = Intent(Intent.ACTION_VIEW, FileAccessHelper.getDataUri(file_name))
                        .setDataAndType(FileAccessHelper.getDataUri(file_name), "video/mp4")
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra("title", title)
                context.startActivity(intent)
            }
        }

        private fun getEpTitle(title: String, file: String): String {
            return title + " " + file.substring(file.lastIndexOf("-") + 1, file.lastIndexOf("."))
        }

        fun getPlayIntent(context: Context, title: String, file_name: String): PendingIntent {
            val file = FileAccessHelper.getFile(file_name)
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0") {
                PendingIntent.getActivity(
                    context,
                    abs(file_name.hashCode()),
                    PrefsUtil.getPlayerIntent()
                        .setData(FileAccessHelper.getFileUri(file_name)).putExtra("isFile", true)
                        .putExtra("title", getEpTitle(title, file_name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                val intent = Intent(Intent.ACTION_VIEW, FileAccessHelper.getDataUri(file_name))
                    .setDataAndType(FileAccessHelper.getDataUri(file_name), "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("title", getEpTitle(title, file_name))
                PendingIntent.getActivity(
                    context,
                    abs(file_name.hashCode()),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }
}
