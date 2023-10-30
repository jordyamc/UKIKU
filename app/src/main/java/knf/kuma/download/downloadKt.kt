package knf.kuma.download

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import knf.kuma.ads.AdsUtils
import knf.kuma.commons.noCrash
import org.jetbrains.anko.activityManager
import java.util.Locale

val isDeviceSamsung: Boolean get() = Build.MANUFACTURER.lowercase(Locale.getDefault()) == "samsung"

fun Context.service(intent: Intent) {
    noCrash {
        if (isDeviceSamsung && AdsUtils.remoteConfigs.getBoolean("samsung_disable_foreground"))
            startService(intent)
        else
            ContextCompat.startForegroundService(this, intent)
    }
}

fun Service.foreground(id: Int, notification: Notification) {
    noCrash {
        if (isDeviceSamsung && AdsUtils.remoteConfigs.getBoolean("samsung_disable_foreground")) return@noCrash
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }
}

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean{
    activityManager.getRunningServices(Int.MAX_VALUE).forEach {
        if (it.service.className == serviceClass.name)
            return true
    }
    return false
}