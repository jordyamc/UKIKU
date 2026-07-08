package knf.kuma.emision

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class EmissionPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val monday = EmissionFragment[1]
    private val tuesday = EmissionFragment[2]
    private val wednesday = EmissionFragment[3]
    private val thursday = EmissionFragment[4]
    private val friday = EmissionFragment[5]
    private val saturday = EmissionFragment[6]
    private val sunday = EmissionFragment[7]

    override fun getCount(): Int {
        return 7
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Lunes"
            1 -> "Martes"
            2 -> "Miércoles"
            3 -> "Jueves"
            4 -> "Viernes"
            5 -> "Sábado"
            6 -> "Domingo"
            else -> "Lunes"
        }
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> monday
            1 -> tuesday
            2 -> wednesday
            3 -> thursday
            4 -> friday
            5 -> saturday
            6 -> sunday
            else -> monday
        }
    }
}
