package knf.kuma.jobscheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import knf.kuma.R
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.Network
import knf.kuma.commons.isFullMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class UpdateWork(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        if (Network.isConnected && isFullMode)
            try {
                val document =
                    Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num")
                        .get()
                val nCode =
                    Integer.parseInt(document.select("body").first().ownText().trim { it <= ' ' })
                val sCode = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("last_notified_update", 0)
                if (nCode <= sCode)
                    return Result.success()
                val oCode =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                if (nCode > oCode) {
                    showNotification()
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putInt("last_notified_update", nCode).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        return Result.success()
    }

    private fun showNotification() {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL).apply {
                setSmallIcon(R.drawable.ic_not_update)
                setContentTitle("UKIKU")
                setContentText("Nueva versión disponible")
                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        5598,
                        Intent(context, DesignUtils.mainClass),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                color = ContextCompat.getColor(context, R.color.colorAccent)
            }.build()
            context.notificationManager.notify(954857, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val TAG = "update-job"
        const val CHANNEL = "app-updater"

        fun schedule() {
            doAsync {
                PeriodicWorkRequestBuilder<UpdateWork>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS).apply {
                    setConstraints(networkConnectedConstraints())
                    addTag(TAG)
                }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.KEEP)
            }
        }
    }
}