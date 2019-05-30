package knf.kuma.jobscheduler

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import knf.kuma.App
import knf.kuma.backup.BUUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.AutoBackupObject
import org.jetbrains.anko.doAsync
import java.util.concurrent.TimeUnit

class BackUpWork(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        BUUtils.init(context)
        return if (BUUtils.isLogedIn) {
            val backupObject = BUUtils.waitAutoBackup(context)
            if (backupObject != null) {
                if (backupObject == AutoBackupObject(context))
                    BUUtils.backupAllNUI(context)
                else
                    WorkManager.getInstance().cancelAllWorkByTag(TAG)
            }
            Result.success()
        } else
            Result.failure()
    }

    companion object {
        internal const val TAG = "backup-job"

        fun checkInit() {
            doAsync {
                BUUtils.init(App.context)
                if (BUUtils.isLogedIn) {
                    val backupObject = BUUtils.waitAutoBackup(App.context)
                    if (backupObject != null && backupObject == AutoBackupObject(App.context)) {
                        val days = backupObject.value
                        if (days == null) {
                            BUUtils.backup(AutoBackupObject(App.context, PrefsUtil.autoBackupTime), object : BUUtils.AutoBackupInterface {
                                override fun onResponse(backupObject: AutoBackupObject?) {

                                }
                            })
                        } else if (days != PrefsUtil.autoBackupTime) {
                            PreferenceManager.getDefaultSharedPreferences(App.context).edit().putString("auto_backup", days).apply()
                            reSchedule(days.toInt())
                        }
                    }
                }
            }
        }

        fun reSchedule(days: Int) {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
            if (days > 0) {
                PeriodicWorkRequestBuilder<BackUpWork>(days.toLong(), TimeUnit.DAYS).apply {
                    setConstraints(networkConnectedConstraints())
                    setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                    addTag(TAG)
                }.build().enqueue()
            }
        }
    }
}