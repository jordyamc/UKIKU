package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import knf.kuma.App

@SuppressLint("StaticFieldLeak")
object Network {

    val isConnected: Boolean
        get() {
            return try {
                val cm = App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetworkInfo
                activeNetwork != null && activeNetwork.isConnected
            } catch (e: NullPointerException) {
                false
            }

        }

    val ipAddress: String
        get() {
            val wm = App.context.applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            return Formatter.formatIpAddress(wm?.connectionInfo?.ipAddress ?: 0)
        }
}
