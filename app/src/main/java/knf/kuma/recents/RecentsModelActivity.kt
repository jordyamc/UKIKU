package knf.kuma.recents

import androidx.fragment.app.Fragment
import knf.kuma.custom.SingleFragmentMaterialActivity

class RecentsModelActivity : SingleFragmentMaterialActivity() {
    override fun createFragment(): Fragment = RecentModelsFragment.get(intent.getIntExtra("initial", -1))
    override fun getActivityTitle(): String = "Recientes"
}