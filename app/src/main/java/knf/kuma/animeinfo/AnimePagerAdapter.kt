package knf.kuma.animeinfo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import knf.kuma.animeinfo.fragments.ChaptersFragment
import knf.kuma.animeinfo.fragments.DetailsFragment

class AnimePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val detailsFragment = DetailsFragment.get()
    private val chaptersFragment = ChaptersFragment.get()

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> "EPISODIOS"
            else -> "INFO"
        }
    }

    fun onChaptersReselect() {
        chaptersFragment.onReselect()
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> chaptersFragment
            else -> detailsFragment
        }
    }

    override fun getCount(): Int {
        return 2
    }
}
