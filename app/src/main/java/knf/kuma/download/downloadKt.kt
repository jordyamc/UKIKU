package knf.kuma.download

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import knf.kuma.ads.AdsUtils

val isDeviceSamsung: Boolean get() = Build.MANUFACTURER.toLowerCase() == "samsung"

fun Context.service(intent: Intent) {
    if (isDeviceSamsung && AdsUtils.remoteConfigs.getBoolean("samsung_disable_foreground"))
        startService(intent)
    else
        ContextCompat.startForegroundService(this, intent)
}

fun Service.foreground(id: Int, notification: Notification) {
    if (isDeviceSamsung && AdsUtils.remoteConfigs.getBoolean("samsung_disable_foreground")) return
    startForeground(id, notification)
}