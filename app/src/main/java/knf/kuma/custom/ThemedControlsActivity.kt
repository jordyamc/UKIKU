package knf.kuma.custom

import android.os.Bundle
import es.munix.multidisplaycast.CastControlsActivity
import knf.kuma.commons.EAHelper

class ThemedControlsActivity : CastControlsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
    }

}