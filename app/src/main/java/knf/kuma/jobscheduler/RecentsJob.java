package knf.kuma.jobscheduler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import org.jsoup.Jsoup;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import knf.kuma.BuildConfig;
import knf.kuma.Main;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.download.DownloadDialogActivity;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.Recents;
import knf.kuma.recents.RecentsNotReceiver;
import pl.droidsonroids.jspoon.Jspoon;

public class RecentsJob extends Job {
    public static final String CHANNEL_RECENTS = "channel.RECENTS";
    public static final int KEY_SUMMARY = 55971;
    static final String TAG = "recents-job";
    private final String RECENTS_GROUP = "recents-group";
    private RecentsDAO recentsDAO = CacheDB.INSTANCE.recentsDAO();
    private FavsDAO favsDAO = CacheDB.INSTANCE.favsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
    private AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
    private NotificationDAO notificationDAO = CacheDB.INSTANCE.notificationDAO();
    private NotificationManager manager;

    public static void schedule(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int time = Integer.valueOf(Objects.requireNonNull(preferences.getString("recents_time", "1"))) * 15;
        if (time > 0 && JobManager.instance().getAllJobRequestsForTag(TAG).size() == 0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(time))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    public static void reSchedule(int time) {
        JobManager.instance().cancelAllForTag(TAG);
        if (time > 0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(time))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    public static void run() {
        new JobRequest.Builder(TAG)
                .startNow()
                .build().schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        try {
            manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Recents recents = Jspoon.create().adapter(Recents.class).fromHtml(Jsoup.connect("https://animeflv.net/").cookies(BypassUtil.getMapCookie(getContext())).userAgent(BypassUtil.userAgent).get().outerHtml());
            List<RecentObject> objects = RecentObject.create(recents.list);
            List<RecentObject> local = recentsDAO.getAll();
            if (local.size() == 0 && !BuildConfig.DEBUG)
                return Result.SUCCESS;
            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("notify_favs", false)) {
                notifyFavChaps(local, objects);
            } else {
                notifyAllChaps(local, objects);
            }
            recentsDAO.clear();
            recentsDAO.setCache(objects);
            return Result.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            return Result.FAILURE;
        }
    }

    private void notifyAllChaps(List<RecentObject> local, List<RecentObject> objects) throws Exception {
        for (RecentObject object : objects) {
            if (!local.contains(object))
                notifyRecent(object);
        }
    }

    private void notifyFavChaps(List<RecentObject> local, List<RecentObject> objects) throws Exception {
        for (RecentObject object : objects) {
            if (!local.contains(object) && (favsDAO.isFav(Integer.parseInt(object.aid)) || seeingDAO.isSeeing(object.aid)))
                notifyRecent(object);
        }
    }

    private void notifyRecent(RecentObject recentObject) throws Exception {
        AnimeObject animeObject = getAnime(recentObject);
        NotificationObj obj = new NotificationObj(Integer.parseInt(recentObject.eid), NotificationObj.RECENT);
        Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL_RECENTS)
                .setSmallIcon(R.drawable.ic_new_recent)
                .setColor(getContext().getResources().getColor(R.color.colorAccent))
                .setContentTitle(recentObject.name)
                .setContentText(recentObject.chapter)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(getBitmap(recentObject))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(getContext(), (int) System.currentTimeMillis(), getAnimeIntent(animeObject, obj), PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(getContext(), (int) System.currentTimeMillis(), obj.getBroadcast(getContext()), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(android.R.drawable.stat_sys_download_done, "Acciones", PendingIntent.getActivity(getContext(), (int) System.currentTimeMillis(), getChapIntent(recentObject, obj), PendingIntent.FLAG_UPDATE_CURRENT))
                .setGroup(RECENTS_GROUP)
                .build();
        notificationDAO.add(obj);
        manager.notify(obj.key, notification);
        notifySummary();
    }

    @Nullable
    private Bitmap getBitmap(RecentObject object) {
        try {
            if (PrefsUtil.INSTANCE.getShowRecentImage())
                return PicassoSingle.get(getContext()).load(object.img).get();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private AnimeObject getAnime(@NonNull RecentObject recentObject) throws Exception {
        AnimeObject animeObject = animeDAO.getByAid(recentObject.aid);
        if (animeObject == null) {
            animeObject = new AnimeObject(recentObject.anime, Jspoon.create().adapter(AnimeObject.WebInfo.class).fromHtml(Jsoup.connect(recentObject.anime).get().outerHtml()));
            animeDAO.insert(animeObject);
        }
        return animeObject;
    }

    @NonNull
    private Intent getAnimeIntent(@NonNull AnimeObject object, @NonNull NotificationObj notificationObj) {
        return new Intent(getContext(), ActivityAnime.class)
                .setData(Uri.parse(object.link))
                .putExtras(notificationObj.getBroadcast(getContext()))
                .putExtra("title", object.name)
                .putExtra("aid", object.aid)
                .putExtra("img", object.img)
                .putExtra("notification", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @NonNull
    private Intent getChapIntent(@NonNull RecentObject object, @NonNull NotificationObj notificationObj) {
        return new Intent(getContext(), DownloadDialogActivity.class)
                .setData(Uri.parse(object.url))
                .putExtras(notificationObj.getBroadcast(getContext()))
                .putExtra("notification", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private void notifySummary() {
        Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL_RECENTS)
                .setSmallIcon(R.drawable.ic_recents_group)
                .setColor(getContext().getResources().getColor(R.color.colorAccent))
                .setContentTitle("Nuevos capitulos")
                .setContentText("Hay nuevos capitulos recientes!!")
                .setGroupSummary(true)
                .setGroup(RECENTS_GROUP)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), Main.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(getContext(), (int) System.currentTimeMillis(), getSummaryBroadcast(), PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("group_notifications", true))
            manager.notify(KEY_SUMMARY, notification);
    }

    private Intent getSummaryBroadcast() {
        return new Intent(getContext(), RecentsNotReceiver.class).putExtra("mode", 1);
    }
}
