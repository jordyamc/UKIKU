package knf.kuma.recents

import androidx.fragment.app.Fragment
import knf.kuma.custom.SingleFragmentActivity

class RecentsActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment = RecentFragment.get(intent.getIntExtra("initial", -1))
    override fun getActivityTitle(): String = "Recientes"
}