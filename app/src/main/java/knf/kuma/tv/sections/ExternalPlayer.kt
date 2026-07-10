package knf.kuma.tv.sections

import android.content.Context
import knf.kuma.R
import knf.kuma.commons.PrefsUtil
import xdroid.toaster.Toaster

class ExternalPlayer : SectionObject() {
    override val image: Int
        get() = R.drawable.ic_player_white

    override val title: String
        get() = "Cambiar reproductor"

    override fun open(context: Context?) {
        if (PrefsUtil.useInternalPlayer) {
            PrefsUtil.useInternalPlayer = false
            Toaster.toastLong("Usando reproductor externo")
        } else {
            PrefsUtil.useInternalPlayer = true
            Toaster.toastLong("Usando reproductor interno")
        }
    }
}