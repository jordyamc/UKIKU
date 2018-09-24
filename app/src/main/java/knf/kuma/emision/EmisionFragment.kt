package knf.kuma.emision

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.android.synthetic.main.recycler_emision.*
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import pl.droidsonroids.jspoon.Jspoon
import java.util.*

class EmisionFragment : Fragment() {
    private val dao = CacheDB.INSTANCE.animeDAO()
    private var adapter: EmisionAdapter? = null
    private var isFirst = true

    private val blacklist: Set<String>?
        get() = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden", false))
            LinkedHashSet()
        else
            PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", LinkedHashSet())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.recycler_emision, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = EmisionAdapter(this)
        recycler.adapter = adapter
        if (context != null)
            CacheDB.INSTANCE.animeDAO().getByDay(arguments!!.getInt("day", 1), blacklist!!).observe(this, Observer { animeObjects ->
                progress.visibility = View.GONE
                if (isFirst && animeObjects != null && animeObjects.isNotEmpty()) {
                    isFirst = false
                    adapter?.update(animeObjects as MutableList<AnimeObject>) { smoothScroll() }
                    recycler.scheduleLayoutAnimation()
                    checkStates(animeObjects)
                }
                error.visibility = if (animeObjects == null || animeObjects.isEmpty()) View.VISIBLE else View.GONE
            })
    }

    private fun smoothScroll() {
        //recycler.layoutManager?.smoothScrollToPosition(recycler,null,0)
    }

    private fun checkStates(animeObjects: MutableList<AnimeObject>?) {
        doAsync {
            try {
                for (animeObject in animeObjects!!) {
                    try {
                        val document = Jsoup.connect(animeObject.link).cookie("device", "computer").get()
                        val animeObject1 = AnimeObject(animeObject.link!!, Jspoon.create().adapter(AnimeObject.WebInfo::class.java).fromHtml(document.outerHtml()))
                        if (animeObject1.state != "En emisión") {
                            dao.updateAnime(animeObject1)
                            adapter?.remove(adapter!!.list.indexOf(animeObject))
                            WEmisionProvider.update(context!!)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun reloadList() {
        if (context != null)
            CacheDB.INSTANCE.animeDAO().getByDay(arguments!!.getInt("day", 1), blacklist!!).observe(this, Observer { animeObjects ->
                error!!.visibility = View.GONE
                if (animeObjects != null && animeObjects.isNotEmpty())
                    adapter?.update(animeObjects as MutableList<AnimeObject>) { smoothScroll() }
                else
                    adapter?.update(ArrayList()) { smoothScroll() }
                if (animeObjects == null || animeObjects.isEmpty())
                    error!!.visibility = View.VISIBLE
            })
    }

    companion object {

        operator fun get(day: AnimeObject.Day): EmisionFragment {
            val bundle = Bundle()
            bundle.putInt("day", day.value)
            val fragment = EmisionFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
