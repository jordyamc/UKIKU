package knf.kuma.recents

import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.flow.Flow

class RecentsViewModel : ViewModel() {
    private val repository = Repository()

    val dbFlowData: Flow<List<RecentAV1>>
        get() = CacheDB.INSTANCE.recentAV1DAO().allFlow

    fun reload() {
        repository.reloadRecents()
    }
}
