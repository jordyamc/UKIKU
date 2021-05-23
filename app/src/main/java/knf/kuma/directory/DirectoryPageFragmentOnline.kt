package knf.kuma.directory

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import knf.kuma.BottomFragment
import knf.kuma.DiagnosticMaterial
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.showSnackbar
import knf.kuma.commons.verifyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find

class DirectoryPageFragmentOnline : BottomFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var progress: ProgressBar
    private var manager: RecyclerView.LayoutManager? = null
    private val adapter: DirectoryPageAdapterOnline by lazy { DirectoryPageAdapterOnline(this) }
    private var isFirst = true
    private var waitingScroll = false

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_dir
        } else {
            R.layout.recycler_dir_grid
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        lifecycleScope.launch {
            createDirectoryPagedList(getType()) {
                try {
                    var snack: Snackbar? = null
                    snack = recyclerView.showSnackbar("Error al cargar directorio", Snackbar.LENGTH_INDEFINITE, "reintentar") {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (withContext(Dispatchers.IO) { BypassUtil.isNeeded(BypassUtil.testLink) }) {
                                startActivity(Intent(requireContext(), DiagnosticMaterial.FullBypass::class.java))
                            }else {
                                snack?.dismiss()
                                delay(2000)
                                adapter.retry()
                            }
                        }
                    }
                }catch (e:Exception){
                    lifecycleScope.launch {
                        delay(2000)
                        adapter.retry()
                    }
                }
            }.collect {
                adapter.submitData(it)
            }
        }
        progress.post { progress.visibility = View.GONE }
    }

    fun getType() =
            when (arguments?.getInt("type", 0)) {
                1 -> "ova"
                2 -> "movie"
                else -> "tv"
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
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
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

        operator fun get(type: Int): DirectoryPageFragmentOnline {
            val bundle = Bundle()
            bundle.putInt("type", type)
            val fragment = DirectoryPageFragmentOnline()
            fragment.arguments = bundle
            return fragment
        }
    }
}
