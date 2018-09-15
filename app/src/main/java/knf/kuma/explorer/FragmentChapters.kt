package knf.kuma.explorer

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import xdroid.toaster.Toaster

class FragmentChapters : Fragment() {
    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.progress)
    lateinit var progressBar: ProgressBar
    internal var adapter: ExplorerChapsAdapter? = null
    private var clearInterface: ClearInterface? = null
    private var isFirst = true

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0") == "0") {
            R.layout.recycler_explorer_chaps
        } else {
            R.layout.recycler_explorer_chaps_grid
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    fun setObject(explorerObject: ExplorerObject?) {
        clear()
        explorerObject?.let {
            it.getLiveData(context)
                    .observe(this@FragmentChapters, Observer { fileDownObjs ->
                        if (fileDownObjs.isEmpty()) {
                            Toaster.toast("Directorio vacio")
                            CacheDB.INSTANCE.explorerDAO().delete(explorerObject)
                            clearInterface!!.onClear()
                        } else {
                            explorerObject.chapters = fileDownObjs as MutableList<ExplorerObject.FileDownObj>
                            progressBar.visibility = View.GONE
                            adapter = ExplorerChapsAdapter(this@FragmentChapters, recyclerView, explorerObject, clearInterface)
                            recyclerView.adapter = adapter
                            if (isFirst) {
                                isFirst = false
                                recyclerView.scheduleLayoutAnimation()
                            }
                        }
                    }) //webInfo.clearLiveData(this);
        }
    }

    internal fun deleteAll() {
        if (adapter != null)
            adapter!!.deleteAll()
    }

    private fun clear() {
        isFirst = true
        adapter = null
        launch(UI) {
            progressBar.visibility = View.VISIBLE
            recyclerView.adapter = null
        }
    }

    fun setInterface(clearInterface: ClearInterface) {
        this.clearInterface = clearInterface
        if (adapter != null)
            adapter!!.setInterface(clearInterface)
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
