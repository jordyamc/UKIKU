package knf.kuma.jobscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import knf.kuma.commons.Network;
import knf.kuma.directory.DirectoryUpdateService;
import xdroid.toaster.Toaster;

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

    public static void runNow() {
        if (Network.isConnected()) {
            JobManager.instance().cancelAllForTag(TAG);
            new JobRequest.Builder(TAG)
                    .startNow()
                    .build().schedule();
        } else {
            Toaster.toast("Se necesita internet");
        }
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        if (DirectoryUpdateService.isRunning())
            ContextCompat.startForegroundService(getContext(), new Intent(getContext(), DirectoryUpdateService.class));
        reSchedule(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("dir_update_time", "7")));
        return Result.SUCCESS;
    }
}
