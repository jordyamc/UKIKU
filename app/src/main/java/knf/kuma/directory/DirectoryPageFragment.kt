package knf.kuma.directory

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.verifyManager
import knf.kuma.pojos.AnimeObject
import org.jetbrains.anko.find

class DirectoryPageFragment : BottomFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var progress: ProgressBar
    private var manager: RecyclerView.LayoutManager? = null
    private var adapter: DirectoryPageAdapter? = null
    private var isFirst = true
    private var listUpdated = false

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0") == "0") {
            R.layout.recycler_dir
        } else {
            R.layout.recycler_dir_grid
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val model = ViewModelProviders.of(activity!!).get(DirectoryViewModel::class.java)
        adapter = DirectoryPageAdapter(this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0)
                    scrollTop()
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0)
                    scrollTop()
            }
        })
        getLiveData(model).observe(this, Observer { animeObjects ->
            hideProgress()
            adapter!!.submitList(animeObjects)
            makeAnimation()
        })
        recyclerView.verifyManager()
        recyclerView.adapter = adapter
    }

    private fun getLiveData(model: DirectoryViewModel): LiveData<PagedList<AnimeObject>> {
        return when (arguments!!.getInt("type", 0)) {
            0 -> model.getAnimes()
            1 -> model.getOvas()
            2 -> model.getMovies()
            else -> model.getAnimes()
        }
    }

    fun onChangeOrder() {
        if (activity != null && adapter != null) {
            getLiveData(ViewModelProviders.of(activity!!).get(DirectoryViewModel::class.java)).observe(this, Observer { animeObjects ->
                hideProgress()
                listUpdated = true
                adapter?.submitList(animeObjects)
                makeAnimation()
                scrollTop()
            })
        }
    }

    private fun hideProgress() {
        progress.post { progress.visibility = View.GONE }
    }

    private fun makeAnimation() {
        if (isFirst) {
            recyclerView.scheduleLayoutAnimation()
            isFirst = false
        }
    }

    @UiThread
    private fun scrollTop() {
        try {
            recyclerView.smoothScrollToPosition(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout, container, false)
        recyclerView = view.find(R.id.recycler)
        progress = view.find(R.id.progress)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manager = recyclerView.layoutManager
        recyclerView.layoutManager = manager
        isFirst = true
    }

    override fun onReselect() {
        manager?.smoothScrollToPosition(recyclerView, null, 0)
    }

    enum class DirType(var value: Int) {
        ANIMES(0),
        OVAS(1),
        MOVIES(2)
    }

    companion object {

        operator fun get(type: DirType): DirectoryPageFragment {
            val bundle = Bundle()
            bundle.putInt("type", type.value)
            val fragment = DirectoryPageFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
