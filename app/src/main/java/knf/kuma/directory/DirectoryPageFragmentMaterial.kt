package knf.kuma.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.verifyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.anko.find

class DirectoryPageFragmentMaterial : BottomFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var progress: ProgressBar
    private var manager: RecyclerView.LayoutManager? = null
    private var adapter: DirectoryPageAdapterMaterial? = null
    private var isFirst = true
    private var waitingScroll = false
    private var listUpdated = false
    private val model: DirectoryViewModel by viewModels()

    private var dataJob: Job? = null

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_dir
        } else {
            R.layout.recycler_dir_grid
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            getData { animeObjects ->
                hideProgress()
                adapter?.submitData(animeObjects)
                makeAnimation()
            }
        }
    }

    private fun getData(callback: suspend (PagingData<DirObject>) -> Unit) {
        dataJob?.cancel()
        dataJob = lifecycleScope.launch {
            when (arguments?.getInt("type", 0) ?: 0) {
                1 -> model.getOvas()
                2 -> model.getMovies()
                else -> model.getAnimes()
            }.collectLatest {
                callback(it)
            }
        }
    }

    fun onChangeOrder() {
        activity?.let {
            waitingScroll = true
            lifecycleScope.launch {
                adapter?.submitData(PagingData.empty())
                showProgress()
                getData { animeObjects ->
                    hideProgress()
                    listUpdated = true
                    adapter?.submitData(animeObjects)
                    makeAnimation()
                }
            }
        }
    }

    private fun hideProgress() {
        progress.post { progress.visibility = View.GONE }
    }

    private fun showProgress() {
        isFirst = true
        progress.post { progress.visibility = View.VISIBLE }
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
        adapter = DirectoryPageAdapterMaterial(this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0 && waitingScroll) {
                    scrollTop()
                    waitingScroll = false
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0 && waitingScroll) {
                    scrollTop()
                    waitingScroll = false
                }
            }
        })
        recyclerView.verifyManager()
        recyclerView.adapter = adapter
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

        operator fun get(type: DirType): DirectoryPageFragmentMaterial {
            val bundle = Bundle()
            bundle.putInt("type", type.value)
            val fragment = DirectoryPageFragmentMaterial()
            fragment.arguments = bundle
            return fragment
        }
    }
}
