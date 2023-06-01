package knf.kuma.recents

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.BannerContainerView
import knf.kuma.home.HomeFragment
import knf.kuma.pojos.RecentObject
import knf.kuma.recents.viewholders.RecyclerRefreshHolder
import knf.kuma.videoservers.FileActions
import knf.kuma.videoservers.ServersFactory
import org.jetbrains.anko.support.v4.find

class RecentFragment : BottomFragment(), SwipeRefreshLayout.OnRefreshListener {
    private val viewModel: RecentsViewModel by viewModels()
    private var holder: RecyclerRefreshHolder? = null
    private var adapter: RecentsAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.dbLiveData.observe(viewLifecycleOwner) { objects ->
            holder?.setError(objects.isEmpty())
            holder?.setRefreshing(false)
            adapter?.updateList(objects) { holder?.recyclerView?.scheduleLayoutAnimation() }
            scrollByKey(objects)
        }
        updateList()
        if (!PrefsUtil.isNativeAdsEnabled)
            find<BannerContainerView>(R.id.adContainer).implBanner(AdsType.RECENT_BANNER)
    }

    private fun scrollByKey(list: List<RecentObject>) {
        if (list.isEmpty()) return
        val initial = arguments?.getInt("initial", -1) ?: -1
        if (initial == -1) return
        val find = list.find { it.key == initial } ?: return
        holder?.scrollTo(list.indexOf(find))
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
        return view
    }

    override fun onRefresh() {
        updateList()
    }

    private fun updateList() {
        if (!Network.isConnected) {
            holder?.setRefreshing(false)
        } else {
            viewModel.reload()
        }
    }

    override fun onReselect() {
        EAHelper.enter1("R")
        holder?.scrollToTop()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        ServersFactory.clear()
        FileActions.reset()
        (activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        fun get(initialKey: Int): BottomFragment {
            val fragment = RecentFragment()
            val bundle = Bundle()
            bundle.putInt("initial", initialKey.also { Log.e("Recent", "Add argument key: $it") })
            fragment.arguments = bundle
            return fragment
        }

        fun get(): BottomFragment {
            return if (PrefsUtil.useHome)
                HomeFragment()
            else
                RecentFragment()
        }
    }
}
