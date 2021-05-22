package knf.kuma.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = Repository()

    private var searchJob: Job? = null

    private var queryLive = MutableLiveData<String?>(null)

    fun sendQuery(query: String?) {
        queryLive.value = query
    }

    val queryListener: LiveData<String?> get() = queryLive

    fun setSearch(
        query: String,
        genres: String,
        callback: suspend (PagingData<SearchObject>) -> Unit
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            (if (query == "" && genres == "")
                repository.search
            else if (genres == "")
                repository.getSearch(query)
            else
                repository.getSearch(query, genres)).collectLatest {
                callback(it)
            }
        }
    }
}
