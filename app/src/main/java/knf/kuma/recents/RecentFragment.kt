package knf.kuma.recents

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.recents.viewholders.RecyclerRefreshHolder
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class RecentFragment : BottomFragment(), SwipeRefreshLayout.OnRefreshListener {
    private var viewModel: RecentsViewModel? = null
    private var holder: RecyclerRefreshHolder? = null
    private var adapter: RecentsAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RecentsViewModel::class.java)
        viewModel?.dbLiveData?.observe(this, Observer { objects ->
            launch(UI) {
                if (objects != null) {
                    holder?.setError(objects.isEmpty())
                    holder?.setRefreshing(false)
                    adapter?.updateList(objects) { holder?.recyclerView?.scheduleLayoutAnimation() }
                }
            }
        })
        updateList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recycler_refresh_fragment, container, false)
        holder = RecyclerRefreshHolder(view)
        holder?.refreshLayout?.setOnRefreshListener(this@RecentFragment)
        adapter = RecentsAdapter(this@RecentFragment, holder!!.recyclerView)
        holder?.recyclerView?.adapter = adapter
        holder?.setRefreshing(true)
        EAHelper.enter1("R")
        return view
    }

    override fun onRefresh() {
        updateList()
    }

    private fun updateList() {
        if (!Network.isConnected) {
            holder?.setRefreshing(false)
        } else {
            viewModel?.reload(context!!)
        }
    }

    override fun onReselect() {
        EAHelper.enter1("R")
        if (holder != null) holder!!.scrollToTop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ServersFactory.clear()
        (activity as AppCompatActivity?)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    companion object {
        fun get(): RecentFragment {
            return RecentFragment()
        }
    }
}
