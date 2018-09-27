package knf.kuma.custom

import android.os.Bundle
import android.os.PersistableBundle
import es.munix.multidisplaycast.CastControlsActivity
import knf.kuma.commons.EAHelper

class ThemedControlsActivity : CastControlsActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState, persistentState)
    }
}