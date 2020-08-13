package knf.kuma.emision

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import knf.kuma.pojos.AnimeObject

class EmissionPagerAdapterMaterial internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val monday = EmissionFragmentMaterial[AnimeObject.Day.MONDAY]
    private val tuesday = EmissionFragmentMaterial[AnimeObject.Day.TUESDAY]
    private val wednesday = EmissionFragmentMaterial[AnimeObject.Day.WEDNESDAY]
    private val thursday = EmissionFragmentMaterial[AnimeObject.Day.THURSDAY]
    private val friday = EmissionFragmentMaterial[AnimeObject.Day.FRIDAY]
    private val saturday = EmissionFragmentMaterial[AnimeObject.Day.SATURDAY]
    private val sunday = EmissionFragmentMaterial[AnimeObject.Day.SUNDAY]

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

    fun updateChanges() {
        monday.updateChanges()
        tuesday.updateChanges()
        wednesday.updateChanges()
        thursday.updateChanges()
        friday.updateChanges()
        saturday.updateChanges()
        sunday.updateChanges()
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
