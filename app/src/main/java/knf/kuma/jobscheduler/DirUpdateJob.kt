package knf.kuma.jobscheduler

import android.content.Context
import androidx.preference.PreferenceManager
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.directory.DirectoryUpdateService
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit

class DirUpdateJob : Job() {

    override fun onRunJob(params: Params): Result {
        if (PrefsUtil.isDirectoryFinished && !DirectoryUpdateService.isRunning)
            DirectoryUpdateService.run(context)
        reSchedule(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("dir_update_time", "7")
                ?: "7"))
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "dir-update-job"

        fun schedule(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val time = Integer.valueOf(preferences.getString("dir_update_time", "7") ?: "7")
            if (JobManager.instance().getAllJobRequestsForTag(TAG).size == 0 &&
                    preferences.getBoolean("directory_finished", false) &&
                    time > 0)
                JobRequest.Builder(TAG)
                        .setExecutionWindow(TimeUnit.DAYS.toMillis(time.toLong()), TimeUnit.DAYS.toMillis((time + 1).toLong()))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }

        fun reSchedule(value: Int) {
            JobManager.instance().cancelAllForTag(TAG)
            if (value > 0)
                JobRequest.Builder(TAG)
                        .setExecutionWindow(TimeUnit.DAYS.toMillis(value.toLong()), TimeUnit.DAYS.toMillis((value + 1).toLong()))
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .build().schedule()
        }

        fun runNow() {
            if (Network.isConnected) {
                JobManager.instance().cancelAllForTag(TAG)
                JobRequest.Builder(TAG)
                        .startNow()
                        .build().schedule()
            } else {
                Toaster.toast("Se necesita internet")
            }
        }
    }
}
