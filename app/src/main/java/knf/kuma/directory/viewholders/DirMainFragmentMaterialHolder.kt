package knf.kuma.directory.viewholders

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.directory.DirPagerAdapterMaterial
import knf.kuma.directory.DirPagerAdapterOnline
import org.jetbrains.anko.find

class DirMainFragmentMaterialHolder(view: View, manager: FragmentManager) {
    private val tabLayout: TabLayout = view.find(R.id.tabs)
    internal val pager: ViewPager = view.find(R.id.pager)
    private val adapter: FragmentPagerAdapter

    init {
        pager.offscreenPageLimit = 3
        adapter = if (PrefsUtil.isDirectoryFinished || !Network.isConnected)
            DirPagerAdapterMaterial(manager)
        else
            DirPagerAdapterOnline(manager)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
    }

    fun onChangeOrder() {
        (adapter as? DirPagerAdapterMaterial)?.onChangeOrder()
    }

    fun onReselect() {
        (adapter.getItem(pager.currentItem) as BottomFragment).onReselect()
    }
}
