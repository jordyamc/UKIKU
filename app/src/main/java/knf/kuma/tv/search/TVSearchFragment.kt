package knf.kuma.tv.search

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
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
import knf.kuma.database.CacheDB
import knf.kuma.search.SearchFragment
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails

class TVSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider, OnItemViewClickedListener {
    private var arrayObjectAdapter: ArrayObjectAdapter? = null
    private lateinit var liveData: LiveData<MutableList<BasicAnimeObject>>
    private lateinit var observer: Observer<MutableList<BasicAnimeObject>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arrayObjectAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
        setOnItemViewClickedListener(this)
        val headerItem = HeaderItem("Géneros")
        val objectAdapter = ArrayObjectAdapter(TagPresenter()).also {
            it.addAll(0, SearchFragment.genres)
        }
        arrayObjectAdapter?.clear()
        arrayObjectAdapter?.add(ListRow(headerItem, objectAdapter))
        setResult("")
    }

    override fun getResultsAdapter(): ObjectAdapter? {
        return arrayObjectAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        setResult(newQuery.trim())
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        setResult(query.trim())
        return true
    }

    private fun setResult(query: String) {
        if (::liveData.isInitialized && ::observer.isInitialized)
            liveData.removeObserver(observer)
        activity?.let {
            liveData = CacheDB.INSTANCE.animeDAO().getSearchList("%$query%")
            observer = Observer { animeObjects ->
                liveData.removeObservers(it)
                if ((arrayObjectAdapter?.size() ?: 0) > 1)
                    arrayObjectAdapter?.removeItems(1, 1)
                val objectAdapter = ArrayObjectAdapter(AnimePresenter())
                for (animeObject in animeObjects)
                    objectAdapter.add(animeObject)
                val headerItem = HeaderItem(
                        when {
                            query.isEmpty() -> "Todos los animes"
                            animeObjects.isNotEmpty() -> "Resultados para '$query'"
                            else -> "Sin resultados"
                        }
                )
                arrayObjectAdapter?.add(ListRow(headerItem, objectAdapter))
            }
            liveData.observe(it, observer)
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        when (item) {
            is BasicAnimeObject -> context?.let { TVAnimesDetails.start(it, item.link) }
            is String -> context?.let { TVTag.start(it, item) }
        }
    }
}
