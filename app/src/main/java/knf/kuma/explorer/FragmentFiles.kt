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
import knf.kuma.R
import knf.kuma.commons.doOnUI
import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import org.jetbrains.anko.find

class FragmentFiles : Fragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var error: View
    lateinit var progressBar: ProgressBar
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
            adapter?.update(explorerObjects)
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
        recyclerView = view.find(R.id.recycler)
        error = view.find(R.id.error)
        progressBar = view.find(R.id.progress)
        state = view.find(R.id.state)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ExplorerFilesAdapter(this, listener)
        recyclerView.adapter = adapter
    }

    fun onEmpty() {
        doOnUI {
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
