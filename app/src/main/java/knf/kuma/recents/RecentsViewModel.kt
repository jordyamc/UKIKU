package knf.kuma.recents

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecentObject
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.flow.Flow

class RecentsViewModel : ViewModel() {
    private val repository = Repository()

    val dbLiveData: LiveData<MutableList<RecentObject>>
        get() = CacheDB.INSTANCE.recentsDAO().objects

    val dbFlow: Flow<List<RecentObject>>
        get() = CacheDB.INSTANCE.recentsDAO().objectsFlow

    fun reload() {
        repository.reloadAllRecents()
    }
}
