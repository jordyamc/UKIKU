package knf.kuma.jobscheduler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.concurrent.TimeUnit;

import knf.kuma.Main;
import knf.kuma.R;

public class UpdateJob extends Job {
    public static final String TAG = "update-job";
    public static final String CHANNEL = "app-updater";

    public static void schedule() {
        if (JobManager.instance().getAllJobRequestsForTag(TAG).size() == 0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.HOURS.toMillis(6))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        try {
            Document document = Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num").get();
            int n_code = Integer.parseInt(document.select("body").first().ownText().trim());
            int s_code = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("last_notified_update", 0);
            if (n_code <= s_code)
                return Result.SUCCESS;
            int o_code = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionCode;
            if (n_code > o_code) {
                showNotification();
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt("last_notified_update", n_code).apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.SUCCESS;
    }

    private void showNotification() {
        try {
            Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL)
                    .setSmallIcon(R.drawable.ic_not_update)
                    .setContentTitle("UKIKU")
                    .setContentText("Nueva versión disponible")
                    .setContentIntent(PendingIntent.getActivity(getContext(), 5598, new Intent(getContext(), Main.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setColor(getContext().getResources().getColor(R.color.colorAccent))
                    .build();
            ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(954857, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
