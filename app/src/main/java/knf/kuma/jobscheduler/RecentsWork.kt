package knf.kuma.jobscheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.create
import knf.kuma.commons.isFullMode
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadDialogActivity
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.NotificationObj
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.recents.RecentsNotReceiver
import org.jetbrains.anko.notificationManager
import java.util.concurrent.TimeUnit

class RecentsWork(val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {
    private val RECENTS_GROUP = "recents-group"
    private val recentsDAO = CacheDB.INSTANCE.recentAV1DAO()
    private val favsDAO = CacheDB.INSTANCE.favoriteAV1DAO()
    private val seeingDAO = CacheDB.INSTANCE.organizerDAO()
    private val notificationDAO = CacheDB.INSTANCE.notificationDAO()
    private val manager: NotificationManager by lazy { context.notificationManager }

    private val summaryBroadcast: Intent
        get() = Intent(context, RecentsNotReceiver::class.java).putExtra("mode", 1)

    override suspend fun doWork(): Result {
        if (!Network.isConnected) return Result.success().also { Log.e("Recents", "No Network") }
        //setForeground(createForegroundInfo())
        try {
            val array = JsExtractor.processLink("https://animeav1.com/")
            val recents = mutableListOf<RecentAV1>()
            for (i in 0 until array!!.length()) {
                recents.add(RecentAV1.fromJson(i, array.getJSONObject(i)))
            }
            val local = recentsDAO.all
            if (local.isEmpty() && !BuildConfig.DEBUG)
                return Result.success()
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notify_favs", false)) {
                notifyFavChaps(local, recents)
            } else {
                notifyAllChaps(local, recents)
            }
            recentsDAO.setCache(recents)
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    @Throws(Exception::class)
    private fun notifyAllChaps(
        local: List<RecentAV1>,
        objects: List<RecentAV1>
    ) {
        objects.filter {
            !local.any { l ->
                l.eid == it.eid
            }
        }.forEach {
            notifyRecent(it)
        }
    }

    @Throws(Exception::class)
    private fun notifyFavChaps(
        local: List<RecentAV1>,
        objects: List<RecentAV1>
    ) {
        objects.filter {
            !local.any { l ->
                l.eid == it.eid
            } && (favsDAO.isFav(it.aid) || seeingDAO.isSeeing(it.aid))
        }.forEach {
            notifyRecent(it)
        }
    }

    @Throws(Exception::class)
    private fun notifyRecent(recentObject: RecentAV1) {
        val obj = NotificationObj(
            recentObject.eid,
            NotificationObj.RECENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_RECENTS).create {
            setSmallIcon(R.drawable.ic_new_recent)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            setContentTitle(recentObject.name)
            setContentText(recentObject.chapter)
            priority = NotificationCompat.PRIORITY_MAX
            val tone = FileAccessHelper.toneFile
            if (tone.exists())
                setSound(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tone
                    ).also {
                        context.grantUriPermission(
                            "com.android.systemui",
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tone
                            ),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                )
            setLargeIcon(getBitmap(recentObject.animeImageUrl))
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    getAnimeIntent(recentObject, obj),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setDeleteIntent(
                PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    obj.getBroadcast(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            if (isFullMode)
                addAction(
                    android.R.drawable.stat_sys_download_done,
                    "Acciones",
                    PendingIntent.getActivity(
                        context,
                        System.currentTimeMillis().toInt(),
                        getChapIntent(recentObject, obj),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            setGroup(RECENTS_GROUP)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        }.build()
        notificationDAO.add(obj)
        manager.notify(obj.key, notification)
        notifySummary()
    }

    private fun getBitmap(link: String): Bitmap? {
        return try {
            if (PrefsUtil.showRecentImage) Glide.with(context).asBitmap().load(link).submit().get() else null
        } catch (e: Exception) {
            null
        }

    }

    private fun getAnimeIntent(animeObject: RecentAV1, notificationObj: NotificationObj): Intent {
        return Intent(context, DesignUtils.infoClass)
                .setData(animeObject.animeUrl.toUri())
                .putExtras(notificationObj.getBroadcast(context))
                .putExtra("title", animeObject.name)
                .putExtra("aid", animeObject.aid)
                .putExtra("img", animeObject.animeImageUrl)
                .putExtra("notification", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun getChapIntent(recentObject: RecentAV1, notificationObj: NotificationObj): Intent {
        return Intent(context, DownloadDialogActivity::class.java)
                .setData(recentObject.chapterUrl.toUri())
                .putExtra("eid", recentObject.eid)
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
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setGroup(RECENTS_GROUP)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, DesignUtils.mainClass),
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    summaryBroadcast,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
                .build()
        if (PrefsUtil.isGroupingEnabled)
            manager.notify(KEY_SUMMARY, notification)
    }

    companion object {
        const val CHANNEL_RECENTS = "channel.RECENTS"
        const val KEY_SUMMARY = 55971
        internal const val TAG = "recents-job"

        fun schedule(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val time = (preferences.getString("recents_time", "1") ?: "1").toInt() * 15
            PeriodicWorkRequestBuilder<RecentsWork>(
                time.coerceAtLeast(15).toLong(),
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            ).apply {
                setInitialDelay(15L, TimeUnit.MINUTES)
                //setConstraints(networkConnectedConstraints())
                //setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                addTag(TAG)
            }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.KEEP)
            /*WorkManager.getInstance(context).getWorkInfosByTagLiveData(TAG).let { ld ->
                lateinit var observer: Observer<List<WorkInfo>>
                doOnUI {
                    ld.observeForever(Observer<List<WorkInfo>> {
                        ld.removeObserver(observer)
                        if (it.isEmpty())
                            doAsync {
                                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                                val time = (preferences.getString("recents_time", "1")
                                        ?: "1").toInt() * 15
                                if (time > 0)
                                    PeriodicWorkRequestBuilder<RecentsWork>(time.coerceAtLeast(15).toLong(), TimeUnit.MINUTES).apply {
                                        setInitialDelay(15L, TimeUnit.MINUTES)
                                        //setConstraints(networkConnectedConstraints())
                                        //setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                                        addTag(TAG)
                                    }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.REPLACE)
                            }
                    }.also { observer = it })
                }
            }*/
        }

        fun reSchedule(time: Int) {
            WorkManager.getInstance(App.context).cancelAllWorkByTag(TAG)
            if (time > 0)
                PeriodicWorkRequestBuilder<RecentsWork>(
                    time.coerceAtLeast(15).toLong(),
                    TimeUnit.MINUTES
                ).apply {
                    //setConstraints(networkConnectedConstraints())
                    //setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    addTag(TAG)
                }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.UPDATE)
        }

        fun run() = OneTimeWorkRequestBuilder<RecentsWork>().build().enqueue()
    }
}