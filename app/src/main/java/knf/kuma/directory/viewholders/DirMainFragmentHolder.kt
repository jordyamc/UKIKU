package knf.kuma.directory.viewholders

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import knf.kuma.BottomFragment
import knf.kuma.directory.DirPagerAdapter
import kotlinx.android.synthetic.main.fragment_directory.view.*

class DirMainFragmentHolder(view: View, manager: FragmentManager) {
    internal val tabLayout: TabLayout = view.tabs
    internal val pager: ViewPager = view.pager
    private val adapter: DirPagerAdapter

    init {
        pager.offscreenPageLimit = 3
        adapter = DirPagerAdapter(manager)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
    }

    fun onChangeOrder() {
        adapter.onChangeOrder()
    }

    fun onReselect() {
        (adapter.getItem(pager.currentItem) as BottomFragment).onReselect()
    }
}
