package knf.kuma.recents

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.NativeManager
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.noCrash
import knf.kuma.home.HomeFragmentMaterial
import knf.kuma.recents.viewholders.RecyclerRefreshHolder
import knf.kuma.videoservers.FileActions
import knf.kuma.videoservers.ServersFactory
import kotlinx.android.synthetic.main.fragment_recent_material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecentModelsFragment : BottomFragment(), SwipeRefreshLayout.OnRefreshListener {
    private val viewModel: RecentModelsViewModel by viewModels()
    private val holder: RecyclerRefreshHolder by lazy {
        RecyclerRefreshHolder(requireView()).also {
            it.refreshLayout.setOnRefreshListener(this@RecentModelsFragment)
            it.recyclerView.adapter = adapter
        }
    }
    private val adapter: RecentModelsAdapter by lazy { RecentModelsAdapter(this) }
    private var isFirstLoad = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.dbLiveData.collectLatest { objects ->
                holder.setError(objects.isEmpty())
                holder.setRefreshing(false)
                if (adapter.itemCount == 0 || objects.isNotEmpty() && objects[0].hashCode().toLong() != adapter.getItemId(0)) {
                    adapter.updateList(objects) {
                        loadAds(objects)
                        if (isFirstLoad) {
                            holder.recyclerView.scheduleLayoutAnimation()
                            isFirstLoad = false
                        } else {
                            holder.scrollToTop()
                        }
                    }
                    scrollByKey(objects)
                }
            }
        }
        updateList()
    }

    private fun scrollByKey(list: List<RecentModel>) {
        if (list.isEmpty()) return
        val initial = arguments?.getInt("initial", -1) ?: -1
        if (initial == -1) {
            noCrash { holder.scrollToTop() }
            return
        }
        val find = list.find { it.key == initial } ?: return
        holder.scrollTo(list.indexOf(find))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recent_material, container, false)
        EAHelper.enter1("R")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        holder.setRefreshing(true)
    }

    private fun loadAds(list: List<RecentModel>) {
        if (PrefsUtil.isAdsEnabled) {
            if (AdsUtils.isAdmobEnabled && PrefsUtil.isNativeAdsEnabled) {
                if (adapter.hasAds()) return
                lifecycleScope.launch(Dispatchers.Main) {
                    NativeManager.take(this, 3) {
                        if (it.isEmpty())
                            adContainer.implBanner(AdsType.RECENT_BANNER, true)
                        else {
                            adapter.updateList(list, it.mapIndexed { index, unifiedNativeAd -> RecentModelAd(index, unifiedNativeAd) }) {
                                holder.scrollToTop()
                            }
                        }
                    }
                }
            } else
                adContainer.implBanner(AdsType.RECENT_BANNER, true)
        }
    }

    override fun onRefresh() {
        updateList()
    }

    private fun updateList() {
        if (!Network.isConnected) {
            holder.setRefreshing(false)
        } else {
            viewModel.reload()
        }
    }

    override fun onReselect() {
        EAHelper.enter1("R")
        noCrash { holder.scrollToTop() }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        ServersFactory.clear()
        FileActions.reset()
        (activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        fun get(initialKey: Int): BottomFragment {
            val fragment = RecentModelsFragment()
            val bundle = Bundle()
            bundle.putInt("initial", initialKey.also { Log.e("Recent", "Add argument key: $it") })
            fragment.arguments = bundle
            return fragment
        }

        fun get(): BottomFragment {
            return if (PrefsUtil.useHome)
                HomeFragmentMaterial()
            else
                RecentModelsFragment()
        }
    }
}
