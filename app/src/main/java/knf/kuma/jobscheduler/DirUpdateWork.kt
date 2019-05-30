package knf.kuma.jobscheduler

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.directory.DirectoryUpdateService
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit

class DirUpdateWork(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        if (PrefsUtil.isDirectoryFinished && !DirectoryUpdateService.isRunning && !DirectoryUpdateService.isRunning)
            DirectoryUpdateService.run(context)
        return Result.success()
    }

    companion object {
        const val TAG = "dir-update-job"

        fun schedule(context: Context) {
            doAsync {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                val time = Integer.valueOf(preferences.getString("dir_update_time", "7") ?: "7")
                if (WorkManager.getInstance().getWorkInfosByTag(TAG).get().size == 0 &&
                        PrefsUtil.isDirectoryFinished &&
                        time > 0)
                    PeriodicWorkRequestBuilder<DirUpdateWork>(time.toLong(), TimeUnit.DAYS).apply {
                        setConstraints(networkConnectedConstraints())
                        addTag(TAG)
                    }.build().enqueue()
            }
        }

        fun reSchedule(value: Int) {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
            if (PrefsUtil.isDirectoryFinished && value > 0)
                PeriodicWorkRequestBuilder<DirUpdateWork>(value.toLong(), TimeUnit.DAYS).apply {
                    setConstraints(networkConnectedConstraints())
                    addTag(TAG)
                }.build().enqueue()
        }

        fun runNow() {
            if (Network.isConnected) {
                OneTimeWorkRequestBuilder<DirUpdateWork>().apply {
                    setConstraints(networkConnectedConstraints())
                }.build().enqueue()
            } else {
                Toaster.toast("Se necesita internet")
            }
        }
    }

}