package knf.kuma.jobscheduler

import android.content.Context
import androidx.work.*
import knf.kuma.App
import knf.kuma.backup.Backups
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeContext
import knf.kuma.pojos.AutoBackupObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class BackUpWork(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val service = Backups.createService()
        return if (service?.isLoggedIn == true) {
            val backupObject = runBlocking(Dispatchers.IO) {
                service.search(Backups.keyAutoBackup)
            }
            if (backupObject != null) {
                if (backupObject == AutoBackupObject(context))
                    Backups.backupAll()
                else
                    WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
            Result.success()
        } else
            Result.failure()
    }

    companion object {
        internal const val TAG = "backupObj-job"

        fun checkInit() {
            GlobalScope.launch(Dispatchers.IO) {
                val service = Backups.createService()
                if (service?.isLoggedIn == true) {
                    val obj = service.search(Backups.keyAutoBackup) as? AutoBackupObject
                    val localObj = AutoBackupObject(App.context)
                    if (obj == localObj) {
                        val days = obj.value
                        if (days.isNullOrBlank())
                            service.backup(localObj, Backups.keyAutoBackup)
                        else if (days != PrefsUtil.autoBackupTime) {
                            PrefsUtil.autoBackupTime = days
                            reSchedule(days.toInt())
                        }
                    }
                }
            }
        }

        fun reSchedule(days: Int) {
            WorkManager.getInstance(safeContext).cancelAllWorkByTag(TAG)
            if (days > 0) {
                PeriodicWorkRequestBuilder<BackUpWork>(days.toLong(), TimeUnit.DAYS, 1, TimeUnit.HOURS).apply {
                    setConstraints(networkConnectedConstraints())
                    setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                    addTag(TAG)
                }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.REPLACE)
            }
        }
    }
}