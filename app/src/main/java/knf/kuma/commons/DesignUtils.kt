package knf.kuma.commons

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import knf.kuma.App
import knf.kuma.Main
import knf.kuma.MainMaterial
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.emision.EmissionActivity
import knf.kuma.emision.EmissionActivityMaterial
import knf.kuma.explorer.ExplorerActivity
import knf.kuma.explorer.ExplorerActivityMaterial

object DesignUtils {
    private const val nameMainFlat = "knf.kuma.MainMaterial"
    private const val nameMainClassic = "knf.kuma.Main"
    private const val nameInfoFlat = "knf.kuma.animeinfo.ActivityAnimeMaterial"
    private const val nameInfoClassic = "knf.kuma.animeinfo.ActivityAnime"
    private var lastPref = PrefsUtil.designStyle

    val isFlat get() = PrefsUtil.designStyle == "0"

    val mainClass: Class<*> get() = if (isFlat) MainMaterial::class.java else Main::class.java
    val infoClass: Class<*> get() = if (isFlat) ActivityAnimeMaterial::class.java else ActivityAnime::class.java
    val explorerClass: Class<*> get() = if (isFlat) ExplorerActivityMaterial::class.java else ExplorerActivity::class.java
    val emissionClass: Class<*> get() = if (isFlat) EmissionActivityMaterial::class.java else EmissionActivity::class.java

    fun change(activity: FragmentActivity, to: String? = PrefsUtil.designStyle, start: Boolean = true) {
        to ?: return
        if (to == "0") {
            enableComponent(nameMainFlat)
            enableComponent(nameInfoFlat)
            if (start){
                activity.finish()
                activity.startActivity(Intent(activity, MainMaterial::class.java).putExtra("start_position", 3))
            }
            disableComponent(nameMainClassic)
            disableComponent(nameInfoClassic)
        } else {
            enableComponent(nameMainClassic)
            enableComponent(nameInfoClassic)
            if (start){
                activity.finish()
                activity.startActivity(Intent(activity, Main::class.java).putExtra("start_position", 3))
            }
            disableComponent(nameMainFlat)
            disableComponent(nameInfoFlat)
        }
    }

    private fun disableComponent(name: String) {
        App.context.packageManager.apply {
            setComponentEnabledSetting(ComponentName(App.context.packageName, name), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    private fun enableComponent(name: String) {
        App.context.packageManager.apply {
            setComponentEnabledSetting(ComponentName(App.context.packageName, name), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }
    }

    fun listenDesignChange(activity: FragmentActivity){
        PrefsUtil.getLiveDesignType().observe(activity) {
            noCrash {
                if (it != lastPref && it.toInt() >= 0) {
                    lastPref = it
                    change(activity, it)
                }
            }
        }
    }

}