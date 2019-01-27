package knf.kuma.jobscheduler

import androidx.preference.PreferenceManager
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import knf.kuma.App
import knf.kuma.backup.BUUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.AutoBackupObject
import org.jetbrains.anko.doAsync
import java.util.concurrent.TimeUnit

class BackupJob : Job() {

    override fun onRunJob(params: Job.Params): Job.Result {
        BUUtils.init(context)
        return if (BUUtils.isLogedIn) {
            val backupObject = BUUtils.waitAutoBackup(context)
            if (backupObject != null) {
                if (backupObject == AutoBackupObject(context))
                    BUUtils.backupAllNUI(context)
                else
                    JobManager.instance().cancelAllForTag(TAG)
            }
            Job.Result.SUCCESS
        } else
            Job.Result.FAILURE
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
            JobManager.instance().cancelAllForTag(TAG)
            if (days > 0)
                JobRequest.Builder(TAG)
                        .setPeriodic(TimeUnit.DAYS.toMillis(days.toLong()))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }
    }
}
