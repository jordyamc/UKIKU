package knf.kuma.emision

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.verifyManager
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.search.SearchObject
import kotlinx.android.synthetic.main.recycler_emision.*
import java.util.*

class EmissionFragment : Fragment() {
    private val dao = CacheDB.INSTANCE.animeDAO()
    private var adapter: EmissionAdapter? = null
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = EmissionAdapter(this)
        recycler.verifyManager()
        recycler.adapter = adapter
        if (context != null)
            observeList(Observer { animeObjects ->
                progress.visibility = View.GONE
                adapter?.update(animeObjects, false) { smoothScroll() }
                if (isFirst) {
                    isFirst = false
                    recycler.scheduleLayoutAnimation()
                    //checkStates(animeObjects)
                }
                error.visibility = if (animeObjects.isEmpty()) View.VISIBLE else View.GONE
            })
    }

    private fun observeList(obs: Observer<MutableList<SearchObject>>) {
        if (::liveData.isInitialized && ::observer.isInitialized)
            liveData.removeObserver(observer)
        liveData = CacheDB.INSTANCE.animeDAO().getByDay(arguments?.getInt("day", 1)
                ?: 1, blacklist)
        observer = obs
        liveData.observe(this, observer)
    }

    private fun smoothScroll() {
        //recycler.layoutManager?.smoothScrollToPosition(recycler,null,0)
    }

    /*private fun checkStates(animeObjects: MutableList<SearchObject>) {
        doAsync {
            try {
                val updateList = mutableListOf<AnimeObject>()
                for (animeObject in animeObjects) {
                    try {
                        val document = jsoupCookies(animeObject.link).get()
                        val updated = AnimeObject(animeObject.link, Jspoon.create().adapter(AnimeObject.WebInfo::class.java).fromHtml(document.outerHtml()))
                        if (updated.state == "Finalizado")
                            updateList.add(updated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (updateList.isNotEmpty()) {
                    dao.updateAnimes(updateList)
                    WEmisionProvider.update(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/

    fun updateChanges() {
        doOnUI { adapter?.notifyDataSetChanged() }
    }

    internal fun reloadList() {
        if (context != null)
            observeList(Observer { animeObjects ->
                error?.visibility = View.GONE
                if (animeObjects != null && animeObjects.isNotEmpty())
                    adapter?.update(animeObjects) { smoothScroll() }
                else
                    adapter?.update(ArrayList()) { smoothScroll() }
                if (animeObjects == null || animeObjects.isEmpty())
                    error?.visibility = View.VISIBLE
            })
    }

    companion object {

        operator fun get(day: AnimeObject.Day): EmissionFragment {
            val bundle = Bundle()
            bundle.putInt("day", day.value)
            val fragment = EmissionFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
