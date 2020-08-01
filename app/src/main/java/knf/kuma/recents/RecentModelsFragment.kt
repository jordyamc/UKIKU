package knf.kuma.recents

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.preload
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.home.HomeFragment
import knf.kuma.recents.viewholders.RecyclerRefreshHolder
import knf.kuma.videoservers.ServersFactory

class RecentModelsFragment : BottomFragment(), SwipeRefreshLayout.OnRefreshListener {
    private val viewModel: RecentModelsViewModel by viewModels()
    private lateinit var holder: RecyclerRefreshHolder
    private val adapter: RecentModelsAdapter by lazy { RecentModelsAdapter(this) }
    private var isFirstLoad = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.dbLiveData.observe(viewLifecycleOwner, Observer { objects ->
            holder.setError(objects.isEmpty())
            holder.setRefreshing(false)
            if (adapter.itemCount == 0 || objects.isNotEmpty() && objects[0].hashCode().toLong() != adapter.getItemId(0)) {
                requireActivity().preload(objects)
                adapter.updateList(objects) {
                    if (isFirstLoad) {
                        holder.recyclerView.scheduleLayoutAnimation()
                        isFirstLoad = false
                    }
                }
                scrollByKey(objects)
            }
        })
        updateList()
    }

    private fun scrollByKey(list: List<RecentModel>) {
        if (list.isEmpty()) return
        val initial = arguments?.getInt("initial", -1) ?: -1
        if (initial == -1) return
        val find = list.find { it.key == initial } ?: return
        holder.scrollTo(list.indexOf(find))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recycler_refresh_fragment, container, false)
        holder = RecyclerRefreshHolder(view).also {
            it.refreshLayout.setOnRefreshListener(this@RecentModelsFragment)
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
            holder.setRefreshing(false)
        } else {
            viewModel.reload()
        }
    }

    override fun onReselect() {
        EAHelper.enter1("R")
        holder.scrollToTop()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        ServersFactory.clear()
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
                HomeFragment()
            else
                RecentModelsFragment()
        }
    }
}
