package knf.kuma.recents

import androidx.lifecycle.ViewModel
import knf.kuma.database.CacheDB
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.flow.Flow

class RecentModelsViewModel : ViewModel() {
    private val repository = Repository()

    val dbLiveData: Flow<List<RecentModel>> = CacheDB.INSTANCE.recentModelsDAO().allFlow
    /*.onEach { list ->
        withContext(Dispatchers.IO){
            list.forEach {
                it.prepare()
                it.extras.fileWrapper.exist
            }
        }
    }*/

    fun reload() {
        repository.reloadRecentModels()
    }
}
