package knf.kuma.jobscheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import knf.kuma.Main
import knf.kuma.R
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class UpdateJob : Job() {

    override fun onRunJob(params: Job.Params): Job.Result {
        try {
            val document = Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num").get()
            val nCode = Integer.parseInt(document.select("body").first().ownText().trim { it <= ' ' })
            val sCode = PreferenceManager.getDefaultSharedPreferences(context).getInt("last_notified_update", 0)
            if (nCode <= sCode)
                return Job.Result.SUCCESS
            val oCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            if (nCode > oCode) {
                showNotification()
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("last_notified_update", nCode).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Job.Result.SUCCESS
    }

    private fun showNotification() {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_not_update)
                    .setContentTitle("UKIKU")
                    .setContentText("Nueva versión disponible")
                    .setContentIntent(PendingIntent.getActivity(context, 5598, Intent(context, Main::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(954857, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val TAG = "update-job"
        const val CHANNEL = "app-updater"

        fun schedule() {
            if (JobManager.instance().getAllJobRequestsForTag(TAG).size == 0)
                JobRequest.Builder(TAG)
                        .setPeriodic(TimeUnit.HOURS.toMillis(6))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }
    }
}
