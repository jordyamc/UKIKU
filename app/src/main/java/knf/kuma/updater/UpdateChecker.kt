package knf.kuma.updater

import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import knf.kuma.commons.Network
import knf.kuma.commons.isFullMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

object UpdateChecker {
    fun check(context: FragmentActivity, listener: CheckListener) {
        if (Network.isConnected && isFullMode)
            context.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val document =
                        Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num")
                            .get()
                    val nCode = Integer.parseInt(document.select("body").first().ownText().trim())
                    val oCode = PackageInfoCompat.getLongVersionCode(
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        )
                    ).toInt()
                    if (nCode > oCode) {
                        delay(2000)
                        listener.onNeedUpdate(oCode.toString(), nCode.toString())
                    } else {
                        context.filesDir.listFiles()
                            ?.filter {
                                !it.isDirectory && it.name.startsWith("update") && it.name.endsWith(
                                    ".apk"
                                )
                            }
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
