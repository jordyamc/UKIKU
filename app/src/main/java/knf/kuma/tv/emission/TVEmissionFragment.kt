package knf.kuma.tv.emission

import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import knf.kuma.commons.distinct
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.AnimeRow
import knf.kuma.tv.TVRepository
import knf.kuma.tv.details.TVAnimesDetails
import knf.kuma.tv.directory.CalendarPresenter
import kotlinx.coroutines.launch

class TVEmissionFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    private val mRows: SparseArray<AnimeRow> = SparseArray()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        title = "Emisión"
        brandColor = "#424242".toColorInt()
        createDataRows()
        prepareEntranceTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchData()
    }

    private fun createDataRows() {
        mRows.put(1, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Lunes")
                .setPage(1))
        mRows.put(2, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Martes")
                .setPage(1))
        mRows.put(3, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Miercoles")
                .setPage(1))
        mRows.put(4, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Jueves")
                .setPage(1))
        mRows.put(5, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Viernes")
                .setPage(1))
        mRows.put(6, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Sabado")
                .setPage(1))
        mRows.put(7, AnimeRow()
                .setAdapter(ArrayObjectAdapter(CalendarPresenter()))
                .setTitle("Domingo")
                .setPage(1))
        createRows()
    }

    private fun createRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        (1..7).forEach {
            Log.e("Emission", "Key: $it")
            val row = mRows.get(it)
            rowsAdapter.add(ListRow(HeaderItem(row.id.toLong(), row.title), row.adapter))
        }
        adapter = rowsAdapter
        onItemViewClickedListener = this
    }

    private fun fetchData() {
        lifecycleScope.launch {
            val map = TVRepository.calendarMap()
            (1..7).forEach {
                mRows.get(it)?.apply {
                    page = page.plus(1)
                    setList(map[it] ?: emptyList())
                }
                startEntranceTransition()
            }
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is DirectoryAV1Calendar) {
            context?.let { TVAnimesDetails.start(it, item.animeUrl) }
        }
    }
}