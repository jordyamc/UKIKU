package knf.kuma.animeinfo

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.retrofit.Repository
import org.jetbrains.anko.doAsync

class AnimeViewModel : ViewModel() {
    private val repository = Repository()
    val liveData: MutableLiveData<AnimeObject?> = MutableLiveData()

    fun init(context: Context, link: String?, persist: Boolean) {
        link?.let { repository.getAnime(context, link, persist, liveData) }
    }

    fun init(aid: String?) {
        doAsync {
            aid?.let {
                val animeObject = CacheDB.INSTANCE.animeDAO().getAnimeByAid(aid)
                doOnUI { liveData.value = animeObject }
            } ?: doOnUI { liveData.value = null }
        }
    }

    fun reload(context: Context, link: String?, persist: Boolean) {
        init(context, link, persist)
    }
}
