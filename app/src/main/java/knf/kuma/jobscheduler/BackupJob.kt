package knf.kuma.jobscheduler

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import knf.kuma.backup.BUUtils
import knf.kuma.pojos.AutoBackupObject
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
