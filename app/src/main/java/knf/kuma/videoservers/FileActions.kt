package knf.kuma.videoservers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.achievements.AchievementManager
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isNull
import knf.kuma.commons.iterator
import knf.kuma.commons.safeShow
import knf.kuma.commons.showProgressSnackbar
import knf.kuma.commons.toArray
import knf.kuma.custom.snackbar.SnackProgressBarManager
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManagerCentral
import knf.kuma.download.DownloadService
import knf.kuma.download.FileAccessHelper
import knf.kuma.download.MultipleDownloadManager
import knf.kuma.download.service
import knf.kuma.player.openWebPlayer
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.ChapterWID
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.queue.QueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster

object FileActions {
    private var isExecuting = false
    private var snackBarManager: SnackProgressBarManager? = null
    private var servers: MutableList<Server> = ArrayList()
    private var selected = 0

    fun stream(fragment: Fragment, anime: DirectoryAV1, chapter: Chapter, isQueued: Boolean = false, callback: ActionCallback) {
        if (isQueued)
            queuedStream(fragment.requireContext(), fragment.viewLifecycleOwner, anime, chapter, fragment.view, callback)
        else
            stream(fragment.requireContext(), fragment.viewLifecycleOwner, anime, chapter, fragment.view, callback)
    }

    fun download(activity: AppCompatActivity, anime: DirectoryAV1, chapter: Chapter, isQueued: Boolean = false, callback: ActionCallback) {
        if (isQueued)
            queuedDownload(activity, activity, anime, chapter, activity.window.decorView, callback)
        else
            download(activity, activity, anime, chapter, activity.window.decorView, callback)
    }

    fun download(fragment: Fragment, anime: DirectoryAV1, chapter: Chapter, isQueued: Boolean = false, callback: ActionCallback) {
        if (isQueued)
            queuedDownload(fragment.requireContext(), fragment.viewLifecycleOwner, anime, chapter, fragment.view, callback)
        else
            download(fragment.requireContext(), fragment.viewLifecycleOwner, anime, chapter, fragment.view, callback)
    }

    fun stream(context: Context, owner: LifecycleOwner, item: RecentAV1, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, if (CastUtil.get().connected()) Type.CAST else Type.STREAM, item.chapterUrl, item, item.asDownload(), anchorView, callback)
    }

    fun stream(context: Context, owner: LifecycleOwner, anime: DirectoryAV1, item: Chapter, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, if (CastUtil.get().connected()) Type.CAST else Type.STREAM, item.link(anime), item, item.asDownload(anime), anchorView, callback)
    }

    fun download(context: Context, owner: LifecycleOwner, item: RecentAV1, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, Type.DOWNLOAD, item.chapterUrl, item, item.asDownload(), anchorView, callback)
    }

    fun download(context: Context, owner: LifecycleOwner, anime: DirectoryAV1, item: Chapter, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, Type.DOWNLOAD, item.link(anime), item, item.asDownload(anime), anchorView, callback)
    }

    fun queuedStream(context: Context, owner: LifecycleOwner, anime: DirectoryAV1, chapter: Chapter, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, Type.STREAM, chapter.link(anime), chapter, chapter.asDownload(anime, true), anchorView, callback)
    }

    fun queuedDownload(context: Context, owner: LifecycleOwner, anime: DirectoryAV1, chapter: Chapter, anchorView: View? = null, callback: ActionCallback) {
        execute(context, owner, Type.DOWNLOAD, chapter.link(anime), chapter, chapter.asDownload(anime, true), anchorView, callback)
    }

    private fun execute(context: Context, owner: LifecycleOwner, type: Type, url: String, item: Any, downloadObject: DownloadObject, anchorView: View?, callback: ActionCallback) {
        if (isExecuting) {
            Toaster.toast("Solo una petición a la vez")
            callback.call(CallbackState.OPERATION_RUNNING)
            return
        }
        if (!Network.isConnected) {
            Toaster.toast("No hay internet")
            callback.call(CallbackState.UNEXPECTED_ERROR)
            return
        }
        owner.lifecycleScope.launch(Dispatchers.Main) {
            val result = checkPreconditions(context, type == Type.DOWNLOAD)
            if (result != null) {
                reset()
                callback.call(result)
                return@launch
            }
            val actionRequest = ActionRequest(context, owner, type, url, item, downloadObject, callback)
            isExecuting = true
            snackBarManager = getSnackManager(anchorView)
            snackBarManager.showSnack("Obteniendo servidores...")
            launch(Dispatchers.IO) {
                try {
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
                                downloadObject.addQueue -> it.canDownload
                                type == Type.STREAM -> it.canStream
                                else -> it.canDownload
                            }
                        }.toMutableList()
                        showServerList(actionRequest)
                    }
                    if (dubServers.isNotEmpty()) {
                        if (!MultipleDownloadManager.isLoading || MultipleDownloadManager.langSelected == -1)
                            actionRequest.owner.lifecycleScope.launch(Dispatchers.Main) {
                                MaterialDialog(context).safeShow {
                                    lifecycleOwner(actionRequest.owner)
                                    listItems(items = listOf("Subtitulado", "Doblado")) { _, index, _ ->
                                        langSelect(index)
                                        if (MultipleDownloadManager.isLoading)
                                            MultipleDownloadManager.langSelected = index
                                    }
                                    onDismiss {
                                        if (isCancelling(actionRequest.owner)) {
                                            reset()
                                            actionRequest.callback.call(CallbackState.LIFECYCLE_EXPIRED)
                                        }
                                    }
                                    setOnCancelListener {
                                        reset()
                                        callback.call(CallbackState.USER_CANCELLED)
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
                    owner.lifecycleScope.launch(Dispatchers.Main) {
                        snackBarManager.dismissSnack()
                        delay(1000)
                        reset()
                        callback.call(CallbackState.UNEXPECTED_ERROR)
                        Toaster.toast("Error al obtener servidores: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showServerList(actionRequest: ActionRequest, useLast: Boolean = true) {
        actionRequest.owner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (servers.isEmpty()) {
                    Toaster.toast("Sin servidores disponibles")
                    reset()
                    actionRequest.callback.call(CallbackState.NO_SERVERS)
                } else {
                    snackBarManager.dismissSnack()
                    val names = Server.getNames(servers)
                    val lasServer = PrefsUtil.lastServer
                    if (PrefsUtil.rememberServer && lasServer != null && names.contains(lasServer) && useLast)
                        processSelectedServer(actionRequest, names.indexOf(lasServer), lasServer, true)
                    else
                        MaterialDialog(actionRequest.context).safeShow {
                            lifecycleOwner(actionRequest.owner)
                            title(text = "Selecciona servidor")
                            listItemsSingleChoice(items = names, initialSelection = selected) { _, index, text ->
                                processSelectedServer(actionRequest, index, text.toString())
                            }
                            checkBoxPrompt(text = "Recordar selección", isCheckedDefault = PrefsUtil.rememberServer) {
                                PrefsUtil.rememberServer = it
                                if (!it) PrefsUtil.lastServer = null
                            }
                            positiveButton(text =
                            when {
                                actionRequest.downloadObject.addQueue -> "AÑADIR"
                                actionRequest.type == Type.CAST -> "CAST"
                                else -> "INICIAR"
                            })
                            onDismiss {
                                if (isCancelling(actionRequest.owner)) {
                                    reset()
                                    actionRequest.callback.call(CallbackState.LIFECYCLE_EXPIRED)
                                }
                            }
                            negativeButton(text = "CANCELAR") {
                                reset()
                                actionRequest.callback.call(CallbackState.USER_CANCELLED)
                                if (PrefsUtil.lastServer.isNull()) PrefsUtil.rememberServer = false
                            }
                            setOnCancelListener {
                                reset()
                                actionRequest.callback.call(CallbackState.USER_CANCELLED)
                                if (PrefsUtil.lastServer.isNull()) PrefsUtil.rememberServer = false
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al mostrar lista de servidores")
                reset()
                actionRequest.callback.call(CallbackState.UNEXPECTED_ERROR)
            }
        }
    }

    private fun processSelectedServer(actionRequest: ActionRequest, index: Int, text: String, showName: Boolean = false) {
        selected = index
        actionRequest.owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                snackBarManager.showSnack("Obteniendo link${if (showName) " $text" else ""}...")
                val server = servers[selected].verified
                snackBarManager.dismissSnack()
                if (server == null && servers.size == 1) {
                    Toaster.toast("Error en servidor, intente mas tarde")
                    actionRequest.callback.call(CallbackState.SERVER_ERROR)
                } else if (server == null) {
                    servers.removeAt(selected)
                    selected = 0
                    Toaster.toast("Error en servidor")
                    showServerList(actionRequest)
                } else if (server.options.isEmpty()) {
                    servers.removeAt(selected)
                    selected = 0
                    Toaster.toast("Error en servidor")
                    showServerList(actionRequest)
                } else if (server.haveOptions()) {
                    showOptions(actionRequest, server)
                } else {
                    saveLastServer(text)
                    when {
                        server.option.needTabs -> {
                            if (actionRequest.downloadObject.addQueue) {
                                Toaster.toast("Servidor no disponible para añadir a cola")
                                showServerList(actionRequest)
                            } else {
                                try {
                                    CustomTabsIntent.Builder()
                                            .setToolbarColor(Color.parseColor("#DA252D"))
                                            .setShowTitle(true).build().launchUrl(actionRequest.context, Uri.parse(server.option.url))
                                } catch (e: Exception) {
                                    actionRequest.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(server.option.url)))
                                }
                                actionRequest.callback.call(CallbackState.EXTERNAL_LINK)
                            }
                            reset()
                        }
                        text.endsWith("(WEB)") -> {
                            delay(1000)
                            openWebPlayer(actionRequest.context, server.option.url!!, actionRequest.downloadObject.title)
                            actionRequest.callback.call(CallbackState.EXTERNAL_LINK, )
                            reset()
                        }
                        else ->
                            when (actionRequest.type) {
                                Type.CAST -> {
                                    reset()
                                    actionRequest.callback.call(CallbackState.START_CAST, server.option.url)
                                }
                                Type.STREAM -> startStreaming(actionRequest, server.option)
                                else -> startDownload(actionRequest, server.option)
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                reset()
            }
        }
    }

    private fun saveLastServer(name: String) {
        PrefsUtil.lastServer = name
    }

    private fun showOptions(actionRequest: ActionRequest, server: VideoServer) {
        actionRequest.owner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                MaterialDialog(actionRequest.context).safeShow {
                    lifecycleOwner(actionRequest.owner)
                    title(text = server.name)
                    listItemsSingleChoice(items = server.options.map { it.name ?: "" }, initialSelection = 0) { _, index, _ ->
                        saveLastServer(server.name)
                        when (actionRequest.type) {
                            Type.CAST -> {
                                reset()
                                actionRequest.callback.call(CallbackState.START_CAST, server.options[index].url)
                            }
                            Type.STREAM -> startStreaming(actionRequest, server.options[index])
                            else -> startDownload(actionRequest, server.options[index])
                        }
                    }
                    positiveButton(text =
                    when {
                        actionRequest.downloadObject.addQueue -> "AÑADIR"
                        actionRequest.type == Type.CAST -> "CAST"
                        else -> "INICIAR"
                    })
                    negativeButton(text = "ATRAS") { showServerList(actionRequest, false) }
                    onDismiss {
                        if (isCancelling(actionRequest.owner)) {
                            reset()
                            actionRequest.callback.call(CallbackState.LIFECYCLE_EXPIRED)
                        }
                    }
                    setOnCancelListener { showServerList(actionRequest, false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al mostrar lista de opciones")
                showServerList(actionRequest, false)
            }
        }
    }

    private fun startStreaming(actionRequest: ActionRequest, option: Option) {
        reset()
        if (actionRequest.item is ChapterWID && actionRequest.downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, actionRequest.item)
        } else {
            AchievementManager.onPlayChapter()
            try {
                if (PreferenceManager.getDefaultSharedPreferences(App.context).getString("player_type", "0") == "0") {
                    App.context.startActivity(
                            PrefsUtil.getPlayerIntent()
                                    .setData(Uri.parse(option.url))
                                    .putExtra("title", actionRequest.downloadObject.title)
                                    .putExtra("headers", option.headers?.headers?.toArray())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(option.url), "video/mp4")
                            .putExtra("title", actionRequest.downloadObject.title)
                            .putExtra("headers", option.headers?.headers?.toArray())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.context.startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                App.context.startActivity(
                        PrefsUtil.getPlayerIntent()
                                .setData(Uri.parse(option.url))
                                .putExtra("title", actionRequest.downloadObject.title)
                                .putExtra("headers", option.headers?.headers?.toArray())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        actionRequest.callback.call(CallbackState.START_STREAM)
    }

    private fun startDownload(actionRequest: ActionRequest, option: Option) {
        if (BuildConfig.DEBUG) Log.e("Download " + option.server, "${option.url}")
        actionRequest.downloadObject.server = option.server ?: ""
        actionRequest.owner.lifecycleScope.launch(Dispatchers.IO) {
            if (actionRequest.item is AnimeObject.WebInfo.AnimeChapter && actionRequest.downloadObject.addQueue && !CacheDB.INSTANCE.queueDAO().isInQueue(actionRequest.item.eid
                            ?: "0")) {
                CacheDB.INSTANCE.queueDAO().add(QueueObject(Uri.fromFile(FileAccessHelper.getFile(actionRequest.item.fileName)), true, actionRequest.item))
                syncData { queue() }
            }
            actionRequest.downloadObject.link = option.url
            actionRequest.downloadObject.headers = option.headers
            if (PrefsUtil.downloaderType == 0) {
                CacheDB.INSTANCE.downloadsDAO().insert(actionRequest.downloadObject)
                actionRequest.context.service(Intent(App.context, DownloadService::class.java).putExtra("eid", actionRequest.downloadObject.eid).setData(Uri.parse(option.url)))
                reset()
                actionRequest.callback.call(CallbackState.START_DOWNLOAD, true)
            } else {
                reset()
                actionRequest.callback.call(CallbackState.START_DOWNLOAD, DownloadManagerCentral.start(actionRequest.downloadObject))
            }
        }
    }

    fun startPlay(context: Context, title: String, file_name: String) {
        AchievementManager.onPlayChapter()
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0") {
            context.startActivity(PrefsUtil.getPlayerIntent()
                    .setData(FileAccessHelper.getFileUri(file_name))
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

    private suspend fun checkPreconditions(context: Context, isDownload: Boolean): CallbackState? {
        if (!isDownload) return null
        return if (!FileAccessHelper.isStoragePermissionEnabledAsync()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || PrefsUtil.downloadType == "1")
                FileAccessHelper.openTreeChooser(context)
            else
                Toaster.toastLong("¡Se necesita permiso de almacenamiento!")
            CallbackState.MISSING_PERMISSION
        } else if (!MultipleDownloadManager.isSpaceAvailable(1)) {
            Toaster.toast("¡No hay espacio suficiente para descargar!")
            CallbackState.LOW_STORAGE
        } else
            null
    }

    private fun getSnackManager(anchorView: View?): SnackProgressBarManager? {
        val view = anchorView ?: return null
        return SnackProgressBarManager(view)
                .setProgressBarColor(EAHelper.getThemeColor())
                .setOverlayLayoutAlpha(0.4f)
                .setOverlayLayoutColor(android.R.color.background_dark)
    }

    private fun SnackProgressBarManager?.showSnack(text: String) {
        this ?: return
        dismissSnack()
        showProgressSnackbar(text, 5000)
    }

    private fun SnackProgressBarManager?.dismissSnack() {
        this?.dismissAll()
    }

    private fun isCancelling(owner: LifecycleOwner) = owner.lifecycle.currentState.let { it == Lifecycle.State.DESTROYED }

    fun reset() {
        isExecuting = false
        selected = 0
        snackBarManager?.dismissAll()
        snackBarManager = null
        servers = mutableListOf()
    }

    class ActionRequest(
            val context: Context,
            val owner: LifecycleOwner,
            val type: Type,
            val url: String,
            val item: Any,
            val downloadObject: DownloadObject,
            val callback: ActionCallback
    )

    enum class Type {
        STREAM, DOWNLOAD, CAST
    }

    enum class CallbackState {
        OPERATION_RUNNING,
        MISSING_PERMISSION,
        LOW_STORAGE,
        USER_CANCELLED,
        LIFECYCLE_EXPIRED,
        UNEXPECTED_ERROR,
        NO_SERVERS,
        SERVER_ERROR,
        EXTERNAL_LINK,
        START_DOWNLOAD,
        START_STREAM,
        START_CAST
    }
}

typealias ActionCallback = (state: FileActions.CallbackState, extra: Any?) -> Unit

fun ActionCallback.call(state: FileActions.CallbackState, extra: Any? = null) = this(state, extra)

val noCallback: ActionCallback = { _, _ -> }