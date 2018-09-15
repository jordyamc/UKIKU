package knf.kuma.explorer

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
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

class FragmentFiles : Fragment() {
    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.error)
    lateinit var error: View
    @BindView(R.id.progress)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.state)
    lateinit var state: TextView
    private var listener: SelectedListener? = null
    private var adapter: ExplorerFilesAdapter? = null
    private var isFist = true

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0") == "0") {
            R.layout.recycler_explorer
        } else {
            R.layout.recycler_explorer_grid
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        CacheDB.INSTANCE.explorerDAO().all.observe(this, Observer { explorerObjects ->
            adapter!!.update(explorerObjects)
            if (explorerObjects.isNotEmpty()) {
                progressBar.visibility = View.GONE
                state.visibility = View.GONE
                if (isFist) {
                    isFist = false
                    recyclerView.scheduleLayoutAnimation()
                }
            }
        })
        ExplorerCreator.stateListener.observe(this, Observer { s ->
            state.text = s
            state.visibility = if (s == null) View.GONE else View.VISIBLE
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout, container, false)
        ButterKnife.bind(this, view)
        adapter = ExplorerFilesAdapter(this, listener)
        recyclerView.adapter = adapter
        return view
    }

    fun onEmpty() {
        launch(UI) {
            progressBar.visibility = View.GONE
            error.visibility = View.VISIBLE
            state.visibility = View.GONE
        }
    }

    fun setListener(listener: SelectedListener) {
        this.listener = listener
        if (adapter != null)
            adapter!!.setListener(listener)
    }

    interface SelectedListener {
        fun onSelected(explorerObject: ExplorerObject)
    }

    companion object {

        const val TAG = "Files"

        operator fun get(listener: SelectedListener): FragmentFiles {
            val fragmentFiles = FragmentFiles()
            fragmentFiles.setListener(listener)
            return fragmentFiles
        }
    }
}
