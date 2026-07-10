package knf.kuma.explorer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.roundedString
import knf.kuma.commons.safeContext
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.download.UnifiedFile
import knf.kuma.pojos.ExplorerObject
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.io.File
import java.net.URL
import java.util.Locale

object ExplorerCreator {
    var IS_CREATED = false
    var IS_FILES = true
    var FILES_NAME: ExplorerObject? = null

    private var isMigratingDownloads = false
    private val STATE_LISTENER = MutableLiveData<String?>()

    internal val stateListener: LiveData<String?>
        get() = STATE_LISTENER

    fun start(model: ExplorerFilesModel, listener: EmptyListener) {
        if (IS_CREATED) return
        IS_CREATED = true
        doAsync {
            if (!FileAccessHelper.isStoragePermissionEnabled()) {
                Toaster.toastLong("Permiso de almacenamiento no concedido")
                listener.onPermissionFailed()
                postState(null)
                IS_CREATED = false
                return@doAsync
            }
            try {
                postState("Iniciando busqueda")
                Log.e("ExplorerCreator", "On start search")
                val creator = FileAccessHelper.downloadExplorerCreator
                if (creator.exist()) {
                    postState("Buscando animes")
                    Log.e("ExplorerCreator", "On search animes - ${creator.createSlugList()}")
                    val list = creator.createDirectoryList { progress, total ->
                        postState(String.format(Locale.getDefault(), "Procesando animes %d/%d", progress, total))
                    }
                    list.onEach {
                        Log.e("ExplorerCreator", "Found dir: ${it.fileName}")
                    }
                    postState("Creando lista")
                    model.setData(list.sortedBy { it.name })
                    if (list.isEmpty()) {
                        listener.onEmpty()
                    }
                    postState(null)
                } else {
                    model.setData(emptyList())
                    listener.onEmpty()
                    postState(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                IS_CREATED = false
                model.setData(emptyList())
                listener.onEmpty()
                postState(null)
            }
        }
    }

    fun migrateDownloads() {
        if (isMigratingDownloads || !Network.isConnected || PrefsUtil.isAV1DownloadsMigrated || !FileAccessHelper.isStoragePermissionEnabled()) {
            Log.e("Files Migration", "Desactivated")
            return
        }
        isMigratingDownloads = true
        GlobalScope.launch(Dispatchers.IO) {
            doMigrateDownloads()
            isMigratingDownloads = false
        }
    }

    suspend fun doMigrateDownloads() {
        withContext(Dispatchers.IO) {
            val slugsTableFile = File(safeContext.getExternalFilesDir(null), "slugs-table.json")
            val table = JSONObject(
                if (slugsTableFile.exists()) {
                    slugsTableFile.readText()
                } else {
                    slugsTableFile.createNewFile()
                    URL("https://cdn.jsdelivr.net/gh/jordyamc/UKIKU@master/static_data/migration/slugs-table.json").readText().also {
                        slugsTableFile.writeText(it)
                    }
                }
            )
            val root = UnifiedFile.getRoot()
            val rootFiles = root.listFiles()
            rootFiles.forEach { file ->
                if (file.isDirectory() && file.listFiles().isEmpty()) {
                    Log.e("Files Migration", "Directory empty: ${file.name()}")
                    file.delete()
                    return@forEach
                }
                if (file.isDirectory() && file.name().isNotBlank() && !file.name().startsWith("$")) {
                    if (table.has(file.name())) {
                        val migratedSlug = table.getString(file.name())
                        if (file.name() != migratedSlug) {
                            val targetDir = root.child(migratedSlug)
                            if (targetDir.exist()) {
                                file.listFiles().forEach { subFile ->
                                    val targetFile = targetDir.child(subFile.name())
                                    if (!targetFile.exist()) {
                                        subFile.move(targetDir.name())
                                    } else {
                                        subFile.delete()
                                    }
                                }
                            } else {
                                file.rename(migratedSlug)
                            }
                        }
                        val migratedFile = root.child(migratedSlug)
                        if (migratedFile.exist()) {
                            val directory = CacheDB.INSTANCE.directoryDAO().findBySlug(migratedSlug) ?: Repository.getDirectory(migratedSlug)
                            if (directory != null) {
                                migratedFile.listFiles().forEach { subFile ->
                                    if (subFile.isFile() && subFile.name().endsWith(".mp4")){
                                        try {
                                            val subNumber = subFile.name().substringAfterLast("-").replace(".mp4", "").toDouble()
                                            val chapter = directory.chapters.find { it.number == subNumber }
                                            if (chapter != null) {
                                                val newName = "${chapter.eid}$${migratedSlug}-${chapter.number.roundedString()}.mp4"
                                                if (subFile.name() == newName) {
                                                    Log.e("Files Migration", "File correctly named: $newName")
                                                    return@forEach
                                                }
                                                if (migratedFile.child(newName).exist()) {
                                                    Log.e("Files Migration", "File already exists: $newName")
                                                    subFile.delete()
                                                }else {
                                                    subFile.rename(newName)
                                                        .also {
                                                            Log.e(
                                                                "Files Migration",
                                                                "Renamed file: $newName ; $it"
                                                            )
                                                        }
                                                }
                                            } else {
                                                Log.e("Files Migration", "Chapter not found: ${subFile.name()}")
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Log.e("Files Migration", "Error while processing file: ${subFile.name()}")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val result = CacheDB.INSTANCE.directoryDAO().findBySlug(file.name()) ?: Repository.getDirectory(file.name())
                        if (result == null) {
                            Log.e("Files Migration", "Folder not found in table: ${file.name()}")
                            file.rename("$${file.name()}")
                        } else {
                            Log.e("Files Migration", "Folder is valid: ${file.name()}")
                        }
                    }
                }
            }
            PrefsUtil.isAV1DownloadsMigrated = true
        }
    }

    fun onDestroy() {
        IS_CREATED = false
        IS_FILES = true
        FILES_NAME = null
    }

    private fun postState(state: String?) {
        doOnUIGlobal { STATE_LISTENER.value = state }
    }

    interface EmptyListener {
        fun onEmpty()
        fun onPermissionFailed()
    }

    class DirectoryEmptyException(slug: String): IllegalStateException("Directory empty: $slug")
}
