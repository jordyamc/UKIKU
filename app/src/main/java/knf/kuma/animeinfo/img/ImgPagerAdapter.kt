package knf.kuma.animeinfo.img

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ImgPagerAdapter(fm: FragmentManager, private val title: String, private val list: List<String>) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment = ImgFullFragment.create(list[position], title)

    override fun getCount(): Int = list.size
}