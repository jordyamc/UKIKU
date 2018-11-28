package knf.kuma.jobscheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import knf.kuma.BuildConfig
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.create
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadDialogActivity
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.Recents
import knf.kuma.recents.RecentsNotReceiver
import org.jsoup.Jsoup
import pl.droidsonroids.jspoon.Jspoon
import java.util.concurrent.TimeUnit

class RecentsJob : Job() {
    private val RECENTS_GROUP = "recents-group"
    private val recentsDAO = CacheDB.INSTANCE.recentsDAO()
    private val favsDAO = CacheDB.INSTANCE.favsDAO()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()
    private val animeDAO = CacheDB.INSTANCE.animeDAO()
    private val notificationDAO = CacheDB.INSTANCE.notificationDAO()
    private var manager: NotificationManager? = null

    private val summaryBroadcast: Intent
        get() = Intent(context, RecentsNotReceiver::class.java).putExtra("mode", 1)

    override fun onRunJob(params: Job.Params): Job.Result {
        try {
            manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val recents = Jspoon.create().adapter(Recents::class.java).fromHtml(Jsoup.connect("https://animeflv.net/").cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().outerHtml())
            val objects = RecentObject.create(recents.list!!)
            for ((i, recentObject) in objects.withIndex())
                recentObject.key = i
            val local = recentsDAO.all
            if (local.isEmpty() && !BuildConfig.DEBUG)
                return Job.Result.SUCCESS
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notify_favs", false)) {
                notifyFavChaps(local, objects)
            } else {
                notifyAllChaps(local, objects)
            }
            recentsDAO.setCache(objects)
            return Job.Result.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
            return Job.Result.FAILURE
        }

    }

    @Throws(Exception::class)
    private fun notifyAllChaps(local: MutableList<RecentObject>, objects: MutableList<RecentObject>) {
        for (recentObject in objects) {
            if (!local.contains(recentObject))
                notifyRecent(recentObject)
        }
    }

    @Throws(Exception::class)
    private fun notifyFavChaps(local: MutableList<RecentObject>, objects: MutableList<RecentObject>) {
        for (recentObject in objects) {
            if (!local.contains(recentObject) && (favsDAO.isFav(Integer.parseInt(recentObject.aid!!)) || seeingDAO.isSeeing(recentObject.aid!!)))
                notifyRecent(recentObject)
        }
    }

    @Throws(Exception::class)
    private fun notifyRecent(recentObject: RecentObject) {
        val animeObject = getAnime(recentObject)
        val obj = NotificationObj(Integer.parseInt(recentObject.eid!!), NotificationObj.RECENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_RECENTS).create {
            setSmallIcon(R.drawable.ic_new_recent)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            setContentTitle(recentObject.name)
            setContentText(recentObject.chapter)
            priority = NotificationCompat.PRIORITY_MAX
            val tone = FileAccessHelper.INSTANCE.toneFile
            if (tone.exists())
                setSound(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tone)
                            context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            uri
                        } else
                            Uri.fromFile(tone)
                )
            setLargeIcon(getBitmap(recentObject))
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setContentIntent(PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), getAnimeIntent(animeObject, obj), PendingIntent.FLAG_UPDATE_CURRENT))
            setDeleteIntent(PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), obj.getBroadcast(context), PendingIntent.FLAG_UPDATE_CURRENT))
            addAction(android.R.drawable.stat_sys_download_done, "Acciones", PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), getChapIntent(recentObject, obj), PendingIntent.FLAG_UPDATE_CURRENT))
            setGroup(RECENTS_GROUP)
        }.build()
        notificationDAO.add(obj)
        manager?.notify(obj.key, notification)
        notifySummary()
    }

    private fun getBitmap(recentObject: RecentObject): Bitmap? {
        return try {
            if (PrefsUtil.showRecentImage) PicassoSingle[context].load(recentObject.img).get() else null
        } catch (e: Exception) {
            null
        }

    }

    @Throws(Exception::class)
    private fun getAnime(recentObject: RecentObject): AnimeObject {
        var animeObject: AnimeObject? = animeDAO.getByAid(recentObject.aid!!)
        if (animeObject == null) {
            animeObject = AnimeObject(recentObject.anime!!, Jspoon.create().adapter(AnimeObject.WebInfo::class.java).fromHtml(Jsoup.connect(recentObject.anime).get().outerHtml()))
            animeDAO.insert(animeObject)
        }
        return animeObject
    }

    private fun getAnimeIntent(animeObject: AnimeObject, notificationObj: NotificationObj): Intent {
        return Intent(context, ActivityAnime::class.java)
                .setData(Uri.parse(animeObject.link))
                .putExtras(notificationObj.getBroadcast(context))
                .putExtra("title", animeObject.name)
                .putExtra("aid", animeObject.aid)
                .putExtra("img", animeObject.img)
                .putExtra("notification", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun getChapIntent(recentObject: RecentObject, notificationObj: NotificationObj): Intent {
        return Intent(context, DownloadDialogActivity::class.java)
                .setData(Uri.parse(recentObject.url))
                .putExtras(notificationObj.getBroadcast(context))
                .putExtra("notification", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun notifySummary() {
        val notification = NotificationCompat.Builder(context, CHANNEL_RECENTS)
                .setSmallIcon(R.drawable.ic_recents_group)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setContentTitle("Nuevos capitulos")
                .setContentText("Hay nuevos capitulos recientes!!")
                .setGroupSummary(true)
                .setGroup(RECENTS_GROUP)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, Main::class.java), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), summaryBroadcast, PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("group_notifications", true))
            manager?.notify(KEY_SUMMARY, notification)
    }

    companion object {
        const val CHANNEL_RECENTS = "channel.RECENTS"
        const val KEY_SUMMARY = 55971
        internal const val TAG = "recents-job"

        fun schedule(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val time = preferences.getString("recents_time", "1")!!.toInt() * 15
            if (time > 0 && JobManager.instance().getAllJobRequestsForTag(TAG).size == 0)
                JobRequest.Builder(TAG)
                        .setPeriodic(TimeUnit.MINUTES.toMillis(time.toLong()))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }

        fun reSchedule(time: Int) {
            JobManager.instance().cancelAllForTag(TAG)
            if (time > 0)
                JobRequest.Builder(TAG)
                        .setPeriodic(TimeUnit.MINUTES.toMillis(time.toLong()))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }

        fun run() {
            JobRequest.Builder(TAG)
                    .startNow()
                    .build().schedule()
        }
    }
}
