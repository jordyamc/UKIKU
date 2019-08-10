package knf.kuma.tv.emission

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import knf.kuma.commons.distinct
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.pojos.AnimeObject
import knf.kuma.tv.AnimeRow
import knf.kuma.tv.details.TVAnimesDetails
import knf.kuma.tv.directory.DirPresenter

class TVEmissionFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    private val mRows: SparseArray<AnimeRow> = SparseArray()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        title = "Emisión"
        brandColor = Color.parseColor("#424242")
        createDataRows()
        prepareEntranceTransition()
        fetchData()
    }

    private fun createDataRows() {
        mRows.put(AnimeObject.Day.MONDAY.value, AnimeRow()
                .setId(AnimeObject.Day.MONDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Lunes")
                .setPage(1))
        mRows.put(AnimeObject.Day.TUESDAY.value, AnimeRow()
                .setId(AnimeObject.Day.TUESDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Martes")
                .setPage(1))
        mRows.put(AnimeObject.Day.WEDNESDAY.value, AnimeRow()
                .setId(AnimeObject.Day.WEDNESDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Miercoles")
                .setPage(1))
        mRows.put(AnimeObject.Day.THURSDAY.value, AnimeRow()
                .setId(AnimeObject.Day.THURSDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Jueves")
                .setPage(1))
        mRows.put(AnimeObject.Day.FRIDAY.value, AnimeRow()
                .setId(AnimeObject.Day.FRIDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Viernes")
                .setPage(1))
        mRows.put(AnimeObject.Day.SATURDAY.value, AnimeRow()
                .setId(AnimeObject.Day.SATURDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Sabado")
                .setPage(1))
        mRows.put(AnimeObject.Day.SUNDAY.value, AnimeRow()
                .setId(AnimeObject.Day.SUNDAY.value)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Domingo")
                .setPage(1))
        createRows()
    }

    private fun createRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        for (i in AnimeObject.Day.values()) {
            Log.e("Emission", "Key: ${i.value}")
            val row = mRows.get(i.value) ?: continue
            rowsAdapter.add(ListRow(HeaderItem(row.id.toLong(), row.title), row.adapter))
        }
        adapter = rowsAdapter
        onItemViewClickedListener = this
    }

    private fun fetchData() {
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.MONDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.MONDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.TUESDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.TUESDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.WEDNESDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.WEDNESDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.THURSDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.THURSDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.FRIDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.FRIDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.SATURDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.SATURDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
        CacheDB.INSTANCE.animeDAO().getByDayDir(AnimeObject.Day.SUNDAY.value).distinct.observe(this, Observer {
            mRows.get(AnimeObject.Day.SUNDAY.value)?.apply {
                page = page.plus(1)
                setList(it)
            }
            startEntranceTransition()
        })
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is DirObject) {
            context?.let { TVAnimesDetails.start(it, item.link) }
        }
    }
}