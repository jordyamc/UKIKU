package knf.kuma.jobscheduler;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import knf.kuma.backup.BUUtils;
import knf.kuma.pojos.AutoBackupObject;

public class BackupJob extends Job {
    static final String TAG = "backup-job";

    public static void reSchedule(int days) {
        JobManager.instance().cancelAllForTag(TAG);
        if (days > 0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.DAYS.toMillis(days))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        BUUtils.init(getContext());
        if (BUUtils.isLogedIn()) {
            AutoBackupObject object = BUUtils.waitAutoBackup(getContext());
            if (object != null) {
                if (object.equals(new AutoBackupObject(getContext())))
                    BUUtils.backupAllNUI(getContext());
                else
                    JobManager.instance().cancelAllForTag(TAG);
            }
            return Result.SUCCESS;
        } else
            return Result.RESCHEDULE;
    }
}
