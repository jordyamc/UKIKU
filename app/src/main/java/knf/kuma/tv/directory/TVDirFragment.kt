package knf.kuma.tv.directory

import android.os.Bundle
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.Observer
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.tv.details.TVAnimesDetails
import org.jetbrains.anko.doAsync

class TVDirFragment : VerticalGridSupportFragment(), OnItemViewClickedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Directorio"
        gridPresenter = VerticalGridPresenter().apply {
            numberOfColumns = 4
        }
        onItemViewClickedListener = this
        CacheDB.INSTANCE.animeDAO().allLive.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                doAsync {
                    val arrayAdapter = ArrayObjectAdapter(DirAdvPresenter()).apply {
                        addAll(0, it)
                    }
                    doOnUI { adapter = arrayAdapter }
                }
            }
        })
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is DirObject)
            context?.let { TVAnimesDetails.start(it, item.link) }
    }
}