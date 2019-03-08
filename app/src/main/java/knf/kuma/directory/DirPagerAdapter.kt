package knf.kuma.directory

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class DirPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val animes = DirectoryPageFragment[DirectoryPageFragment.DirType.ANIMES]
    private val ovas = DirectoryPageFragment[DirectoryPageFragment.DirType.OVAS]
    private val movies = DirectoryPageFragment[DirectoryPageFragment.DirType.MOVIES]

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> "OVA"
            2 -> "PELICULA"
            else -> "ANIME"
        }
    }

    fun onChangeOrder() {
        animes.onChangeOrder()
        ovas.onChangeOrder()
        movies.onChangeOrder()
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> ovas
            2 -> movies
            else -> animes
        }
    }

    override fun getCount(): Int {
        return 3
    }
}
