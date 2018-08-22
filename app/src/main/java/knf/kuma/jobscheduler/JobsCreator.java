package knf.kuma.jobscheduler;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class JobsCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag){
            case RecentsJob.TAG:
                return new RecentsJob();
            case DirUpdateJob.TAG:
                return new DirUpdateJob();
            case UpdateJob.TAG:
                return new UpdateJob();
            case BackupJob.TAG:
                return new BackupJob();
            default:
                return null;
        }
    }
}
