package knf.kuma.directory

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class DirPagerAdapterMaterial(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val animes = DirectoryPageFragmentMaterial[DirectoryPageFragmentMaterial.DirType.ANIMES]
    private val ovas = DirectoryPageFragmentMaterial[DirectoryPageFragmentMaterial.DirType.OVAS]
    private val movies = DirectoryPageFragmentMaterial[DirectoryPageFragmentMaterial.DirType.MOVIES]

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
