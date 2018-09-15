package knf.kuma.jobscheduler

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class JobsCreator : JobCreator {
    override fun create(tag: String): Job? {
        return when (tag) {
            RecentsJob.TAG -> RecentsJob()
            DirUpdateJob.TAG -> DirUpdateJob()
            UpdateJob.TAG -> UpdateJob()
            BackupJob.TAG -> BackupJob()
            else -> null
        }
    }
}
