package knf.kuma.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.EAHelper

class BottomPreferencesFragment : BottomFragment() {

    var count = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EAHelper.enter1("C")
        return inflater.inflate(R.layout.fragment_preferences, container, false)
    }

    override fun onReselect() {
        EAHelper.enter1("C")
        count++
        if (count == 20) AchievementManager.unlock(listOf(40))
    }

    companion object {

        fun get(): BottomPreferencesFragment {
            return BottomPreferencesFragment()
        }
    }
}
