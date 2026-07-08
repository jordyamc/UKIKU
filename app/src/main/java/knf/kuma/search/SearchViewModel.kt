package knf.kuma.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.SearchDataSource
import knf.kuma.pojos.av1.SearchState
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel : ViewModel() {
    private var queryLive = MutableLiveData<String?>(null)

    private var searchState = SearchState()

    private val stateFlow: MutableStateFlow<SearchState> = MutableStateFlow(searchState)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = stateFlow
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { SearchDataSource(it) }
            ).flow
        }.cachedIn(viewModelScope)

    fun sendQuery(query: String?) {
        searchState = searchState.copy(query = query)
        stateFlow.value = searchState
    }

    val queryListener: LiveData<String?> get() = queryLive

    fun setSearch(
        query: String,
        genres: List<String>
    ) {
        searchState = searchState.copy(query = query, genres = genres)
        stateFlow.value = searchState
    }
}
