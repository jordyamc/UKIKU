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
import com.crashlytics.android.Crashlytics
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.recents.viewholders.RecyclerRefreshHolder
import knf.kuma.videoservers.ServersFactory

class RecentFragment : BottomFragment(), SwipeRefreshLayout.OnRefreshListener {
    private var viewModel: RecentsViewModel? = null
    private var holder: RecyclerRefreshHolder? = null
    private var adapter: RecentsAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RecentsViewModel::class.java)
        viewModel?.dbLiveData?.observe(this@RecentFragment, Observer { objects ->
            holder?.setError(objects.isEmpty())
            holder?.setRefreshing(false)
            adapter?.updateList(objects) { holder?.recyclerView?.scheduleLayoutAnimation() }
        })
        updateList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recycler_refresh_fragment, container, false)
        holder = RecyclerRefreshHolder(view).also {
            it.refreshLayout.setOnRefreshListener(this@RecentFragment)
            adapter = RecentsAdapter(this@RecentFragment, it.recyclerView)
            it.recyclerView.adapter = adapter
            it.setRefreshing(true)
        }
        EAHelper.enter1("R")
        Crashlytics.setString("screen", "Recents")
        return view
    }

    override fun onRefresh() {
        updateList()
    }

    private fun updateList() {
        if (!Network.isConnected) {
            holder?.setRefreshing(false)
        } else {
            viewModel?.reload()
        }
    }

    override fun onReselect() {
        EAHelper.enter1("R")
        holder?.scrollToTop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ServersFactory.clear()
        (activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        fun get(): RecentFragment {
            return RecentFragment()
        }
    }
}
