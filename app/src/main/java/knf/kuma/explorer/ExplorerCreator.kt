package knf.kuma.explorer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.ExplorerObject
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
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
        if (!FileAccessHelper.isStoragePermissionEnabled()) {
            Toaster.toastLong("Permiso de almacenamiento no concedido")
            listener.onEmpty()
            postState(null)
            return
        }
        postState("Iniciando busqueda")
        doAsync {
            val creator = FileAccessHelper.downloadExplorerCreator
            if (creator.exist()) {
                postState("Buscando animes")
                val list = creator.createDirectoryList { progress, total ->
                    postState(String.format(Locale.getDefault(), "Procesando animes %d/%d", progress, total))
                }
                postState("Creando lista")
                explorerDAO.insert(list)
                if (list.isEmpty()) {
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
        doOnUI { STATE_LISTENER.setValue(state) }
    }

    interface EmptyListener {
        fun onEmpty()
    }
}
