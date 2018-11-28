package knf.kuma.achievements

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter

class AchievementsPagerAdapter(private val fragmentManager: FragmentManager, private val onClick: OnClick) : PagerAdapter() {

    private val fragments = arrayOfNulls<Fragment>(2)

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        assert(0 <= position && position < fragments.size)
        val transaction = fragmentManager.beginTransaction()
        transaction.remove(fragments[position]!!)
        transaction.commit()
        fragments[position] = null
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = getItem(position)
        val transaction = fragmentManager.beginTransaction()
        transaction.add(container.id, fragment, "fragment:$position")
        transaction.commit()
        return fragment
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return (any as Fragment).view == view
    }

    private fun getItem(position: Int): Fragment {
        assert(0 <= position && position < fragments.size)
        if (fragments[position] == null)
            fragments[position] = AchievementFragment.get(if (position == 0) 1 else 0, onClick)
        return fragments[position]!!
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> "Bloqueados"
            else -> "Desbloqueados"
        }
    }
}