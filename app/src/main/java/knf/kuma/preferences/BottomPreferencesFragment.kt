package knf.kuma.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.EAHelper

class BottomPreferencesFragment : BottomFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EAHelper.enter1("C")
        return inflater.inflate(R.layout.fragment_preferences, container, false)
    }

    override fun onReselect() {
        EAHelper.enter1("C")
    }

    companion object {

        fun get(): BottomPreferencesFragment {
            return BottomPreferencesFragment()
        }
    }
}
