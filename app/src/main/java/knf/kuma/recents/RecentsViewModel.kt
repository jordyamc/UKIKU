package knf.kuma.recents

import android.content.Context

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecentObject
import knf.kuma.retrofit.Repository

class RecentsViewModel : ViewModel() {
    private val repository = Repository()

    val dbLiveData: LiveData<MutableList<RecentObject>>
        get() = CacheDB.INSTANCE.recentsDAO().objects

    fun reload(context: Context) {
        repository.reloadRecents(context)
    }
}
