package knf.kuma.explorer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.ExplorerObject
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import java.io.FileFilter
import java.util.*

object ExplorerCreator {
    var IS_CREATED = false
    var IS_FILES = true
    var FILES_NAME: ExplorerObject? = null
    private val STATE_LISTENER = MutableLiveData<String>()

    internal val stateListener: LiveData<String>
        get() = STATE_LISTENER

    fun start(listener: EmptyListener) {
        IS_CREATED = true
        val explorerDAO = CacheDB.INSTANCE.explorerDAO()
        postState("Iniciando busqueda")
        doAsync {
            val animeDAO = CacheDB.INSTANCE.animeDAO()
            val root = FileAccessHelper.INSTANCE.downloadsDirectory
            if (root.exists()) {
                postState("Buscando animes")
                val list = ArrayList<ExplorerObject>()
                val files = root.listFiles(FileFilter { it.isDirectory })
                if (files != null) {
                    val names = ArrayList<String>()
                    var progress = 0
                    for (file in files) {
                        names.add(file.name)
                    }
                    for (animeObject in animeDAO.getAllByFile(names))
                        try {
                            progress++
                            postState(String.format(Locale.getDefault(), "Procesando animes %d/%d", progress, files.size))
                            list.add(ExplorerObject(animeObject))
                        } catch (e: IllegalStateException) {
                            e.printStackTrace()
                        }

                    postState("Creando lista")
                    explorerDAO.insert(list)
                }
                if (list.size == 0) {
                    listener.onEmpty()
                    postState(null)
                }
            } else {
                explorerDAO.deleteAll()
                listener.onEmpty()
                postState(null)
            }
        }
    }

    fun onDestroy() {
        IS_CREATED = false
        IS_FILES = true
        FILES_NAME = null
        CacheDB.INSTANCE.explorerDAO().deleteAll()
    }

    private fun postState(state: String?) {
        launch(UI) { STATE_LISTENER.setValue(state) }
    }

    interface EmptyListener {
        fun onEmpty()
    }
}
