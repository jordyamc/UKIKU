package knf.kuma.seeing

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class SeeingPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    val fragmentList: MutableList<SeeingFragment> = mutableListOf()

    init {
        for (i in 0..4) {
            fragmentList.add(SeeingFragment[i])
        }
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentList[position].title
    }
}