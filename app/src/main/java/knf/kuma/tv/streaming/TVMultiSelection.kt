package knf.kuma.tv.streaming

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

class TVMultiSelection : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null)
            GuidedStepSupportFragment.addAsRoot(this,TVMultiSelectionFragment(),android.R.id.content)
    }
}