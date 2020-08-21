package knf.kuma.explorer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.RecordObject
import knf.kuma.queue.QueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster


class FragmentChapters : Fragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var fab: FloatingActionButton
    internal var adapter: ExplorerChapsAdapter? = null
    private var clearInterface: ClearInterface? = null
    private var isFirst = true

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_explorer_chaps
        } else {
            R.layout.recycler_explorer_chaps_grid
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout, container, false)
        recyclerView = view.find(R.id.recycler)
        recyclerView.verifyManager(170)
        progressBar = view.find(R.id.progress)
        fab = view.find(R.id.fab)
        view.find<FrameLayout>(R.id.adContainer).implBanner(AdsType.EXPLORER_BANNER, true)
        return view
    }

    private fun playAll(list: List<ExplorerObject.FileDownObj>) {
        lifecycleScope.launch(Dispatchers.Main){
            noCrashSuspend {
                withContext(Dispatchers.IO) {
                    CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromDownloaded(list.last()))
                    syncData { history() }
                    QueueManager.startQueueDownloaded(context, list)
                }
                adapter?.apply {
                    explorerObject.fileList.forEach {
                        it.isSeen = true
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun setObject(explorerObject: ExplorerObject?) {
        noCrash {
            fab.internalSetVisibility(View.INVISIBLE, true)
            fab.hide()
        }
        clear()
        explorerObject?.let {
            it.getLiveData(context)
                    .observe(this@FragmentChapters, Observer { fileDownObjs ->
                        if (fileDownObjs.isEmpty()) {
                            Toaster.toast("Directorio vacio")
                            lifecycleScope.launch(Dispatchers.IO){
                                CacheDB.INSTANCE.explorerDAO().delete(explorerObject)
                                launch(Dispatchers.Main) {
                                    clearInterface?.onClear()
                                }
                            }
                        } else {
                            explorerObject.chapters = fileDownObjs as MutableList<ExplorerObject.FileDownObj>
                            lifecycleScope.launch(Dispatchers.Main){
                                progressBar.visibility = View.GONE
                                adapter = ExplorerChapsAdapter(this@FragmentChapters, recyclerView, withContext(Dispatchers.IO) { ExplorerObjectWrap(explorerObject) }, clearInterface)
                                recyclerView.adapter = adapter
                                if (isFirst) {
                                    isFirst = false
                                    recyclerView.scheduleLayoutAnimation()
                                }
                                if (!CastUtil.get().connected()) {
                                    fab.show()
                                    fab.onClick { playAll(fileDownObjs) }
                                }
                            }
                        }
                    })
        }
    }

    internal fun deleteAll() {
        adapter?.deleteAll()
    }

    private fun clear() {
        isFirst = true
        adapter = null
        doOnUI {
            progressBar.visibility = View.VISIBLE
            recyclerView.adapter = null
        }
    }

    fun setInterface(clearInterface: ClearInterface) {
        this.clearInterface = clearInterface
        adapter?.setInterface(clearInterface)
    }

    interface ClearInterface {
        fun onClear()
    }

    companion object {
        const val TAG = "Chapters"

        operator fun get(clearInterface: ClearInterface): FragmentChapters {
            val fragmentChapters = FragmentChapters()
            fragmentChapters.setInterface(clearInterface)
            return fragmentChapters
        }
    }
}
