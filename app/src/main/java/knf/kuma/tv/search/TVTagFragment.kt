package knf.kuma.tv.search

import android.os.Bundle
import android.view.View
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.TVRepository
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync

class TVTagFragment : VerticalGridSupportFragment(), OnItemViewClickedListener {

    val slug: String by lazy { arguments?.getString("slug") ?: "" }
    val resultsAdapter by lazy { PagingDataAdapter(AnimePresenter(), DirectoryAV1Min.DIFF) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString("name")
        setGridPresenter(
            VerticalGridPresenter().apply {
                numberOfColumns = 4
            }
        )
        onItemViewClickedListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = resultsAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            TVRepository.searchQuery("", slug).collectLatest {
                resultsAdapter.submitData(it)
            }
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val anime = item as? DirectoryAV1Min
        if (anime != null)
            context?.let { TVAnimesDetails.start(it, anime.animeUrl) }
    }
}