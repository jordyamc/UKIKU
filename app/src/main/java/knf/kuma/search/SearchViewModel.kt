package knf.kuma.search

import androidx.lifecycle.*
import androidx.paging.PagedList
import knf.kuma.retrofit.Repository

class SearchViewModel : ViewModel() {
    private val repository = Repository()

    private var liveData: LiveData<PagedList<SearchObject>> = MutableLiveData()
    private var observer: Observer<PagedList<SearchObject>>? = null

    fun setSearch(query: String, genres: String, owner: LifecycleOwner, observer: Observer<PagedList<SearchObject>>) {
        this.observer?.let { liveData.removeObserver(it) }
        this.observer = observer
        liveData = if (query == "" && genres == "")
            repository.search
        else if (genres == "")
            repository.getSearch(query)
        else
            repository.getSearch(query, genres)
        liveData.observe(owner, observer)
    }
}
