package knf.kuma.tv.streaming

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

class TVServerSelection : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        if (bundle != null && savedInstanceState == null)
            if (bundle.containsKey(TVServerSelectionFragment.SERVERS_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment[bundle.getStringArrayList(TVServerSelectionFragment.SERVERS_DATA)!!, bundle.getString("name")!!, false], android.R.id.content)
            else if (bundle.containsKey(TVServerSelectionFragment.VIDEO_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment[bundle.getStringArrayList(TVServerSelectionFragment.VIDEO_DATA)!!, bundle.getString("name")!!, true], android.R.id.content)
    }
}
