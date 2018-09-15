package knf.kuma.tv.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.tv.anime.AnimePresenter
import knf.kuma.tv.details.TVAnimesDetails

class TVSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider, SpeechRecognitionCallback, OnItemViewClickedListener {
    private var arrayObjectAdapter: ArrayObjectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        arrayObjectAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
        setOnItemViewClickedListener(this)
        if (context!!.packageManager.hasSystemFeature("amazon.hardware.fire_tv"))
            setSpeechRecognitionCallback(this)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 55498)
    }

    override fun getResultsAdapter(): ObjectAdapter? {
        return arrayObjectAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        setResult(newQuery)
        return true
    }

    override fun recognizeSpeech() {

    }

    override fun onQueryTextSubmit(query: String): Boolean {
        setResult(query)
        return true
    }

    private fun setResult(query: String) {
        val liveData = CacheDB.INSTANCE.animeDAO().getSearchList("%$query%")
        if (activity != null) {
            liveData.observe(activity!!, Observer { animeObjects ->
                liveData.removeObservers(activity!!)
                arrayObjectAdapter!!.clear()
                val objectAdapter = ArrayObjectAdapter(AnimePresenter())
                for (`object` in animeObjects)
                    objectAdapter.add(`object`)
                val headerItem = HeaderItem(if (animeObjects.isNotEmpty()) "Resultados para '$query'" else "Sin resultados")
                arrayObjectAdapter!!.add(ListRow(headerItem, objectAdapter))
            })
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        val animeObject = item as AnimeObject
        TVAnimesDetails.start(context!!, animeObject.link!!)
    }
}
