package knf.kuma.animeinfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AnimeViewModel : ViewModel() {
    val infoFlow: MutableStateFlow<DirectoryAV1?> = MutableStateFlow(null)

    fun init(link: String?, persist: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e("Details", "On load $link")
            val slug = link?.substringAfterLast("/")
            if (Network.isConnected) {
                val info = link?.let {
                    try {
                        val data = JsExtractor.processLink(link)?.getJSONObject(0) ?: return@let null
                        DirectoryAV1.fromJson(data)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        CacheDB.INSTANCE.directoryDAO().findBySlug(slug ?: ":none:")
                    }
                }
                if (info != null && persist) {
                    CacheDB.INSTANCE.directoryDAO().add(info)
                }
                infoFlow.value = info
            } else {
                infoFlow.value = CacheDB.INSTANCE.directoryDAO().findBySlug(slug ?: ":none:")
            }
        }
    }

    fun init(aid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e("Details", "On load ID $aid")
            if (aid < 0) {
                infoFlow.value = null
            } else {
                val animeObject = CacheDB.INSTANCE.directoryDAO().findByAid(aid)
                infoFlow.value = animeObject?.also {
                    init(animeObject.animeUrl, true)
                }
            }

        }
    }

    fun reload(link: String?, persist: Boolean) {
        init(link, persist)
    }
}
