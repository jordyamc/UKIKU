package knf.kuma.updater

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import knf.kuma.BuildConfig
import knf.kuma.commons.Network
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup

object UpdateChecker {
    fun check(context: Context, listener: CheckListener) {
        if (Network.isConnected && BuildConfig.BUILD_TYPE != "playstore")
            doAsync {
                try {
                    val document = Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num").get()
                    val nCode = Integer.parseInt(document.select("body").first().ownText().trim())
                    val oCode = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)).toInt()
                    if (nCode > oCode) {
                        listener.onNeedUpdate(oCode.toString(), nCode.toString())
                    } else {
                        context.filesDir.listFiles()
                                ?.filter { !it.isDirectory && it.name.startsWith("update") && it.name.endsWith(".apk") }
                                ?.forEach {
                                    it.delete()
                                }
                        Log.e("Version", "Up to date: $oCode")
                        listener.onUpdateNotRequired()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    listener.onUpdateNotRequired()
                }
            }
        else
            listener.onUpdateNotRequired()
    }

    interface CheckListener {
        fun onNeedUpdate(o_code: String, n_code: String)
        fun onUpdateNotRequired()
    }
}
