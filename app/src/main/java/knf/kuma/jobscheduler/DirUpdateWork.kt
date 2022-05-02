package knf.kuma.jobscheduler

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeContext
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryUpdateService
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit

class DirUpdateWork(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        if (PrefsUtil.isDirectoryFinished && !DirectoryUpdateService.isRunning && !DirectoryService.isRunning)
            DirectoryUpdateService.run(context)
        return Result.success()
    }

    companion object {
        const val TAG = "dir-update-work-unique"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("dir-update-job")
            WorkManager.getInstance(context).cancelAllWorkByTag("dir-update-work")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val time = (preferences.getString("dir_update_time", "7") ?: "7").toLong()
            if (PrefsUtil.isDirectoryFinished && time > 0)
                PeriodicWorkRequestBuilder<DirUpdateWork>(time, TimeUnit.DAYS, 1, TimeUnit.HOURS).apply {
                    setConstraints(networkConnectedConstraints())
                    addTag(TAG)
                }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.KEEP)
        }

        fun reSchedule(value: Int) {
            if (value > 0)
                PeriodicWorkRequestBuilder<DirUpdateWork>(value.toLong(), TimeUnit.DAYS, 1, TimeUnit.HOURS).apply {
                    setConstraints(networkConnectedConstraints())
                    addTag(TAG)
                }.build().enqueueUnique(TAG, ExistingPeriodicWorkPolicy.REPLACE)
            else
                WorkManager.getInstance(safeContext).cancelAllWorkByTag(TAG)
        }

        fun runNow() {
            if (Network.isConnected) {
                OneTimeWorkRequestBuilder<DirUpdateWork>().apply {
                    addTag(TAG)
                    setConstraints(networkConnectedConstraints())
                }.build().enqueue()
            } else {
                Toaster.toast("Se necesita internet")
            }
        }
    }

}