package knf.kuma.emision

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.distinct
import knf.kuma.commons.verifyManager
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.search.SearchObject
import knf.kuma.search.forFav
import kotlinx.android.synthetic.main.recycler_emision.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmissionFragmentMaterial : Fragment(), RemoveListener {
    private var adapter: EmissionAdapterMaterial? = null
    private var isFirst = true

    private lateinit var liveData: LiveData<MutableList<SearchObject>>
    private lateinit var observer: Observer<MutableList<SearchObject>>

    private val blacklist: Set<String>
        get() = if (PrefsUtil.emissionShowHidden)
            LinkedHashSet()
        else
            PrefsUtil.emissionBlacklist

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.recycler_emision, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            adContainer.implBanner(AdsType.EMISSION_BANNER, true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = EmissionAdapterMaterial(this)
        recycler.verifyManager()
        recycler.adapter = adapter
        if (context != null)
            observeList { animeObjects ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    adapter?.update(withContext(Dispatchers.IO) { animeObjects.map { it.forFav() }.toMutableList() }, false) { smoothScroll() }
                    if (isFirst) {
                        isFirst = false
                        recycler.scheduleLayoutAnimation()
                        //checkStates(animeObjects)
                    }
                    error.visibility = if (animeObjects.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun observeList(obs: Observer<MutableList<SearchObject>>) {
        if (::liveData.isInitialized && ::observer.isInitialized)
            liveData.removeObserver(observer)
        liveData = CacheDB.INSTANCE.animeDAO().getByDay(arguments?.getInt("day", 1)
                ?: 1, blacklist).distinct
        observer = obs
        liveData.observe(viewLifecycleOwner, observer)
    }

    override fun onRemove(showError: Boolean) {
        if (showError) lifecycleScope.launch(Dispatchers.Main) { error.visibility = View.VISIBLE }
    }

    private fun smoothScroll() {
        //recycler.layoutManager?.smoothScrollToPosition(recycler,null,0)
    }

    fun updateChanges() {
        lifecycleScope.launch(Dispatchers.Main) { adapter?.notifyDataSetChanged() }
    }

    internal fun reloadList() {
        if (context != null)
            observeList { animeObjects ->
                lifecycleScope.launch(Dispatchers.Main) {
                    error?.visibility = View.GONE
                    if (animeObjects != null && animeObjects.isNotEmpty())
                        adapter?.update(withContext(Dispatchers.IO) { animeObjects.map { it.forFav() }.toMutableList() }) { smoothScroll() }
                    else
                        adapter?.update(ArrayList()) { smoothScroll() }
                    if (animeObjects == null || animeObjects.isEmpty())
                        error?.visibility = View.VISIBLE
                }
            }
    }

    companion object {

        operator fun get(day: AnimeObject.Day): EmissionFragmentMaterial {
            val bundle = Bundle()
            bundle.putInt("day", day.value)
            val fragment = EmissionFragmentMaterial()
            fragment.arguments = bundle
            return fragment
        }
    }
}
