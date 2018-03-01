package knf.kuma.jobscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import knf.kuma.directory.DirectoryUpdateService;

public class DirUpdateJob extends Job {
    public static final String TAG = "dir-update-job";

    public static void schedule(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int time = Integer.valueOf(preferences.getString("dir_update_time", "7"));
        if (JobManager.instance().getAllJobRequestsForTag(TAG).size() == 0 &&
                preferences.getBoolean("directory_finished", false) &&
                time > 0)
            new JobRequest.Builder(TAG)
                    .setExecutionWindow(TimeUnit.DAYS.toMillis(time), TimeUnit.DAYS.toMillis(time + 1))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    public static void reSchedule(int value) {
        JobManager.instance().cancelAllForTag(TAG);
        if (value > 0)
            new JobRequest.Builder(TAG)
                    .setExecutionWindow(TimeUnit.DAYS.toMillis(value), TimeUnit.DAYS.toMillis(value + 1))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        ContextCompat.startForegroundService(getContext(), new Intent(getContext(), DirectoryUpdateService.class));
        reSchedule(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("dir_update_time", "7")));
        return Result.SUCCESS;
    }
}
