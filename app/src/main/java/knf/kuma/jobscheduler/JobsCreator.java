package knf.kuma.jobscheduler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by Jordy on 09/01/2018.
 */

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
            default:
                return null;
        }
    }
}
