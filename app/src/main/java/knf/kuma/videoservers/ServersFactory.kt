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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.isNull
import knf.kuma.commons.iterator
import knf.kuma.commons.safeShow
import knf.kuma.commons.showProgressSnackbar
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.snackbar.SnackProgressBarManager
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManagerCentral
import knf.kuma.download.DownloadService
import knf.kuma.download.FileAccessHelper
import knf.kuma.download.MultipleDownloadManager
import knf.kuma.download.service
import knf.kuma.player.openWebPlayer
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.ChapterWID
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.queue.QueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import kotlin.math.abs


class ServersFactory {
    private var context: Context
    private var url: String
    private var chapter: ChapterWID? = null
    private var downloadObject: DownloadObject
    private var isStream: Boolean = false
    private var isCasting: Boolean = false
    private var serversInterface: ServersInterface
    private var snackBarManager: SnackProgressBarManager? = null
    private var servers: MutableList<Server> = ArrayList()
    private var selected = 0

    private constructor(context: Context, url: String, chapter: ChapterWID, isStream: Boolean, addQueue: Boolean, serversInterface: ServersInterface) {
        this.context = context
        this.url = url
        this.chapter = chapter
        this.downloadObject = chapter.asDownload(addQueue)
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
                    when {
                        server.option.needTabs -> {
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
                        text.endsWith("(WEB)") -> {
                            openWebPlayer(context, server.option.url!!, downloadObject.title)
                            callOnFinish(false, false)
                        }
                        else -> {
                            when {
                                isCasting -> callOnCast(server.option.url)
                                isStream -> startStreaming(server.option, servers[selected] is WebServer)
                                else -> startDownload(server.option)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showServerList(useLast: Boolean = true) {
        doOnUIGlobal {
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
        doOnUIGlobal {
            try {
                MaterialDialog(this@ServersFactory.context).safeShow {
                    title(text = server.name)
                    listItemsSingleChoice(items = server.options.map { it.name ?: "" }, initialSelection = 0) { _, index, _ ->
                        saveLastServer(server.name)
                        when {
                            isCast -> callOnCast(server.options[index].url)
                            isStream -> startStreaming(server.options[index], false)
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
        runBlocking {
            try {
                serversInterface.onProgressIndicator(true)
                showSnack("Obteniendo servidores...")
                val response = JsExtractor.processLinkMultiple(url, listOf("embeds", "downloads"))
                var subServers = mutableListOf<Server>()
                var dubServers = mutableListOf<Server>()
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
                                val server = Server.check(context, url)
                                if (subServers.find { it.baseLink.substringAfterLast("/") == url.substringAfterLast("/") } == null) {
                                    subServers.add(server?: WebServer(context, url, name))
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
                                val server = Server.check(context, url)
                                if (dubServers.find { it.baseLink.substringAfterLast("/") == url.substringAfterLast("/") } == null) {
                                    dubServers.add(server?: WebServer(context, url, name))
                                }
                            }
                        }
                    }
                }
                subServers = subServers.sortedWith(
                    compareBy(
                        { it.name.contains("(WEB)") },
                        {it.name}
                    )
                ).toMutableList()
                dubServers = dubServers.sortedWith(
                    compareBy(
                        { it.name.contains("(WEB)") },
                        {it.name}
                    )
                ).toMutableList()
                val langSelect: (Int) -> Unit = { index ->
                    servers = if (index == 0) {
                        subServers
                    } else {
                        dubServers
                    }.filter {
                        when {
                            downloadObject.addQueue || isCasting -> it.canDownload
                            isStream -> it.canStream
                            else -> it.canDownload
                        }
                    }.toMutableList()
                    showServerList()
                }
                if (dubServers.isNotEmpty()) {
                    if (!MultipleDownloadManager.isLoading || MultipleDownloadManager.langSelected == -1)
                        launch(Dispatchers.Main) {
                            MaterialDialog(context).safeShow {
                                listItems(items = listOf("Subtitulado", "Doblado")) { _, index, _ ->
                                    langSelect(index)
                                }
                                setOnCancelListener {
                                    callOnFinish(false, false)
                                }
                            }
                        }
                    else
                        langSelect(MultipleDownloadManager.langSelected)
                } else {
                    langSelect(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
                servers = ArrayList()
                callOnFinish(false, false)
                Toaster.toast("Error al obtener servidores: ${e.message}")
            }
        }
    }

    private fun startStreaming(option: Option, isWeb: Boolean) {
        if (chapter != null && downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, chapter)
        } else {
            AchievementManager.onPlayChapter()
            try {
                if (isWeb) {
                    openWebPlayer(context, option.url!!, downloadObject.title)
                } else if (PreferenceManager.getDefaultSharedPreferences(App.context).getString("player_type", "0") == "0") {
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
            if (chapter != null && CacheDB.INSTANCE.queueDAO().isInQueue(chapter?.eid?.toString() ?: "0")) {
                CacheDB.INSTANCE.queueDAO().add(QueueObject(Uri.fromFile(FileAccessHelper.getFile(chapter?.filePath()
                        ?: "null")), true, chapter?.asCompatChapter()))
                syncData { queue() }
            }
            downloadObject.link = option.url
            downloadObject.headers = option.headers
            if (PrefsUtil.downloaderType == 0) {
                CacheDB.INSTANCE.downloadsDAO().insert(downloadObject)
                doOnUIGlobal {
                    context.service(Intent(App.context, DownloadService::class.java).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)))
                    callOnFinish(true, true)
                }
            } else
                GlobalScope.launch(Dispatchers.Main) {
                    callOnFinish(true, withContext(Dispatchers.IO) { DownloadManagerCentral.start(downloadObject) })
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
        getSnackManager()?.showProgressSnackbar(text, 10000)
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
                chapter: ChapterWID,
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
            anime: DirectoryAV1,
            chapter: Chapter,
            isStream: Boolean = false,
            addQueue: Boolean = false,
            serversInterface: ServersInterface
        ) {
            if (!isRunning())
                GlobalScope.launch(Dispatchers.Main) {
                    val chapter = ChapterWID(
                        chapter.eid,
                        chapter.number,
                        anime.aid,
                        anime.slug,
                        anime.name
                    )
                    INSTANCE = if (isStream) {
                        ServersFactory(
                            context,
                            url,
                            chapter,
                            isStream,
                            addQueue,
                            serversInterface
                        ).also { doAsync { it.start() } }
                    } else {
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
