package knf.kuma.emision

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import knf.kuma.pojos.AnimeObject

class EmisionPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val monday = EmisionFragment[AnimeObject.Day.MONDAY]
    private val tuesday = EmisionFragment[AnimeObject.Day.TUESDAY]
    private val wednesday = EmisionFragment[AnimeObject.Day.WEDNESDAY]
    private val thursday = EmisionFragment[AnimeObject.Day.THURSDAY]
    private val friday = EmisionFragment[AnimeObject.Day.FRIDAY]
    private val saturday = EmisionFragment[AnimeObject.Day.SATURDAY]
    private val sunday = EmisionFragment[AnimeObject.Day.SUNDAY]

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

    fun reloadPages() {
        monday.reloadList()
        tuesday.reloadList()
        wednesday.reloadList()
        thursday.reloadList()
        friday.reloadList()
        saturday.reloadList()
        sunday.reloadList()
    }
}
