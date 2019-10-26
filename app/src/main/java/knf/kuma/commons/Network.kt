package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import knf.kuma.App
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

@SuppressLint("StaticFieldLeak")
object Network {

    val isConnected: Boolean
        get() {
            return try {
                val cm = App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetworkInfo
                activeNetwork != null && activeNetwork.isConnected
            } catch (e: Exception) {
                false
            }

        }

    val ipAddress: String
        get() {
            val wm = App.context.applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            return Formatter.formatIpAddress(wm?.connectionInfo?.ipAddress ?: 0)
        }

    val isAdsBlocked: Boolean by lazy {
        return@lazy try {
            BufferedReader(InputStreamReader(FileInputStream("/etc/hosts"))).readLines().forEach {
                if (it.contains("admob") || it.contains("appbrains"))
                    return@lazy true
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
