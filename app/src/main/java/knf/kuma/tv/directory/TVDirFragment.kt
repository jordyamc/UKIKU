package knf.kuma.tv.directory

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
import knf.kuma.directory.DirObject
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.TVRepository
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync

class TVDirFragment : VerticalGridSupportFragment(), OnItemViewClickedListener {

    val resultsAdapter by lazy { PagingDataAdapter(AnimePresenter(), DirectoryAV1Min.DIFF) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Directorio"
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
            TVRepository.searchQuery("").collectLatest {
                resultsAdapter.submitData(it)
            }
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is DirectoryAV1Min)
            context?.let { TVAnimesDetails.start(it, item.animeUrl) }
    }
}