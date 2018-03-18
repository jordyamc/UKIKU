package knf.kuma.jobscheduler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import org.jsoup.Jsoup;

import java.util.List;
import java.util.concurrent.TimeUnit;

import knf.kuma.Main;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.BypassUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.database.dao.SeeingDAO;
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
    public final String RECENTS_GROUP = "recents-group";
    private RecentsDAO recentsDAO = CacheDB.INSTANCE.recentsDAO();
    private FavsDAO favsDAO=CacheDB.INSTANCE.favsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
    private AnimeDAO animeDAO=CacheDB.INSTANCE.animeDAO();
    private NotificationDAO notificationDAO = CacheDB.INSTANCE.notificationDAO();
    private NotificationManager manager;

    public static void schedule(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int time=Integer.valueOf(preferences.getString("recents_time","1"))*15;
        if (time>0 && JobManager.instance().getAllJobRequestsForTag(TAG).size() == 0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(time))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build().schedule();
    }

    public static void reSchedule(int time) {
        JobManager.instance().cancelAllForTag(TAG);
        if (time>0)
            new JobRequest.Builder(TAG)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(time))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
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
            if (local.size()==0)
                return Result.SUCCESS;
            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("notify_favs",false)){
                notifyFavChaps(local,objects);
            }else {
                notifyAllChaps(local,objects);
            }
            recentsDAO.clear();
            recentsDAO.setCache(objects);
            return Result.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.FAILURE;
        }
    }

    private void notifyAllChaps(List<RecentObject> local,List<RecentObject> objects){
        for (RecentObject object : objects) {
            if (!local.contains(object))
                notifyRecent(object);
        }
    }

    private void notifyFavChaps(List<RecentObject> local,List<RecentObject> objects){
        for (RecentObject object : objects) {
            if (!local.contains(object) && favsDAO.isFav(Integer.parseInt(object.aid)) && seeingDAO.isSeeing(object.aid))
                notifyRecent(object);
        }
    }

    private boolean listContains(List<RecentObject> list,RecentObject object){
        for (RecentObject o:list){
            if (o.eid.equals(object.eid))
                return true;
        }
        return false;
    }

    private void notifyRecent(RecentObject recentObject) {
        AnimeObject animeObject=animeDAO.getByAid(recentObject.aid);
        NotificationObj obj = new NotificationObj(Integer.parseInt(recentObject.eid), NotificationObj.RECENT);
        Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL_RECENTS)
                .setSmallIcon(R.drawable.ic_new_recent)
                .setColor(getContext().getResources().getColor(R.color.colorAccent))
                .setContentTitle(recentObject.name)
                .setContentText(recentObject.chapter)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(getContext(),(int)System.currentTimeMillis(),getAnimeIntent(animeObject,obj),PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(getContext(), (int) System.currentTimeMillis(), obj.getBroadcast(getContext()), PendingIntent.FLAG_UPDATE_CURRENT))
                .setGroup(RECENTS_GROUP)
                .build();
        notificationDAO.add(obj);
        manager.notify(obj.key, notification);
        notifySummary();
    }

    @NonNull
    private Intent getAnimeIntent(AnimeObject object, NotificationObj notificationObj){
        return new Intent(getContext(), ActivityAnime.class)
                .setData(Uri.parse(object.link))
                .putExtras(notificationObj.getBroadcast(getContext()))
                .putExtra("title", object.name)
                .putExtra("aid", object.aid)
                .putExtra("img", object.img)
                .putExtra("notification",true);
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
        manager.notify(KEY_SUMMARY, notification);
    }

    private Intent getSummaryBroadcast(){
        return new Intent(getContext(), RecentsNotReceiver.class).putExtra("mode",1);
    }
}
