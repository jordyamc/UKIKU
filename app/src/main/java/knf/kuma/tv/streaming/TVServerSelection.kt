package knf.kuma.tv.streaming

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

class TVServerSelection : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null)
            if (intent.hasExtra(TVServerSelectionFragment.SERVERS_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment[intent.getStringArrayListExtra(TVServerSelectionFragment.SERVERS_DATA), intent.getStringExtra("name"), false], android.R.id.content)
            else if (intent.hasExtra(TVServerSelectionFragment.VIDEO_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment[intent.getStringArrayListExtra(TVServerSelectionFragment.VIDEO_DATA), intent.getStringExtra("name"), true], android.R.id.content)
    }
}
