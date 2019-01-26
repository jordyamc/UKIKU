package knf.kuma.tv.search

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import knf.kuma.commons.noCrash
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
        if (context?.packageManager?.hasSystemFeature("amazon.hardware.fire_tv") == true)
            setSpeechRecognitionCallback(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null)
            startRecognition()
    }

    private fun checkPermissions() {
        context?.let {
            if (ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 55498)
        }
    }

    override fun getResultsAdapter(): ObjectAdapter? {
        return arrayObjectAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        setResult(newQuery)
        return true
    }

    override fun recognizeSpeech() {
        noCrash {
            startActivityForResult(recognizerIntent, 5589)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        setResult(query)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            5589 -> if (resultCode == Activity.RESULT_OK)
                setSearchQuery(data, true)
        }
    }

    private fun setResult(query: String) {
        val liveData = CacheDB.INSTANCE.animeDAO().getSearchList("%$query%")
        activity?.let {
            liveData.observe(it, Observer { animeObjects ->
                liveData.removeObservers(it)
                arrayObjectAdapter?.clear()
                val objectAdapter = ArrayObjectAdapter(AnimePresenter())
                for (animeObject in animeObjects)
                    objectAdapter.add(animeObject)
                val headerItem = HeaderItem(if (animeObjects.isNotEmpty()) "Resultados para '$query'" else "Sin resultados")
                arrayObjectAdapter?.add(ListRow(headerItem, objectAdapter))
            })
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        val animeObject = item as AnimeObject
        context?.let { TVAnimesDetails.start(it, animeObject.link) }
    }
}
