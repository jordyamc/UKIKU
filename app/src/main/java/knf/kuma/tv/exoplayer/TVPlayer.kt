package knf.kuma.tv.exoplayer

import android.os.Bundle
import android.view.WindowManager
import knf.kuma.tv.TVBaseActivity

class TVPlayer : TVBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        addFragment(PlaybackFragment[intent.extras!!])
    }
}
