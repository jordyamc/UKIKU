package knf.kuma.tv.search

import android.os.Bundle
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails
import org.jetbrains.anko.doAsync

class TVTagFragment : VerticalGridSupportFragment(), OnItemViewClickedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString("genre")
        gridPresenter = VerticalGridPresenter().apply {
            numberOfColumns = 4
        }
        onItemViewClickedListener = this
        CacheDB.INSTANCE.animeDAO().getAllGenreLive("%" + arguments?.getString("genre") + "%").observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                doAsync {
                    val arrayAdapter = ArrayObjectAdapter(AnimePresenter()).apply {
                        addAll(0, it)
                    }
                    doOnUI { adapter = arrayAdapter }
                }
            }
        })
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val anime = item as? BasicAnimeObject
        if (anime != null)
            context?.let { TVAnimesDetails.start(it, anime.link) }
    }
}