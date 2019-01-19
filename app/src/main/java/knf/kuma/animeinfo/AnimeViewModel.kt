package knf.kuma.animeinfo

import android.content.Context

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.retrofit.Repository

class AnimeViewModel : ViewModel() {
    private val repository = Repository()
    var liveData: LiveData<AnimeObject>? = null
        private set

    fun init(context: Context, link: String?, persist: Boolean) {
        if (liveData != null || link == null)
            return
        liveData = repository.getAnime(context, link, persist)
    }

    fun init(aid: String?) {
        if (liveData != null || aid == null)
            return
        liveData = CacheDB.INSTANCE.animeDAO().getAnimeByAid(aid)
    }

    fun reload(context: Context, link: String, persist: Boolean) {
        liveData = repository.getAnime(context, link, persist)
    }

    enum class ModeState {
        NORMAL
    }
}
