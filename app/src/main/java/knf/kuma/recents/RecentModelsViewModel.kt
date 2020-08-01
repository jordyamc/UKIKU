package knf.kuma.recents

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.retrofit.Repository

class RecentModelsViewModel : ViewModel() {
    private val repository = Repository()

    val dbLiveData: LiveData<List<RecentModel>>
        get() = CacheDB.INSTANCE.recentModelsDAO().allLive

    fun reload() {
        repository.reloadRecentModels()
    }
}
