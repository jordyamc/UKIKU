package knf.kuma.tv.search

import android.os.Bundle
import android.view.View
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.inmobi.media.la
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.Genre
import knf.kuma.pojos.av1.SearchDataSource
import knf.kuma.search.SearchFragmentMaterial
import knf.kuma.tv.TVRepository
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class TVSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider, OnItemViewClickedListener {
    private var arrayObjectAdapter: ArrayObjectAdapter? = null
    private val adapter by lazy { PagingDataAdapter(AnimePresenter(), DirectoryAV1Min.DIFF) }
    private var resultsRow: Row? = null

    private val currentQuery = MutableStateFlow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arrayObjectAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
        setOnItemViewClickedListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arrayObjectAdapter?.clear()
        arrayObjectAdapter?.add(ListRow(HeaderItem("Géneros"), ArrayObjectAdapter(TagPresenter()).also {
            it.addAll(0, SearchFragmentMaterial.genres)
        }))
        resultsRow = ListRow(HeaderItem("Todos los animes"), adapter).also {
            arrayObjectAdapter?.add(it)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            currentQuery.flatMapLatest {
                TVRepository.searchQuery(it)
            }.collectLatest {
                adapter.submitData(it)
                val headerItem = HeaderItem(
                    when {
                        currentQuery.value.isEmpty() -> "Todos los animes"
                        else -> "Resultados para '${currentQuery.value}'"
                    }
                )
                resultsRow?.headerItem = headerItem
            }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter? {
        return arrayObjectAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        currentQuery.value = newQuery
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        currentQuery.value = query
        return true
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        when (item) {
            is DirectoryAV1Min -> context?.let { TVAnimesDetails.start(it, item.animeUrl) }
            is Genre -> context?.let { TVTag.start(it, item) }
        }
    }
}
