package knf.kuma.achievements

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class AchievementsFragmentsPagerAdapter(fragmentManager: FragmentManager, private val onClick: OnClick) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment = AchievementFragment.get(if (position == 0) 1 else 0, onClick)

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