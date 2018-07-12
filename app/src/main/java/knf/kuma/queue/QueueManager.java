package knf.kuma.queue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.player.ExoPlayer;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.QueueObject;
import knf.kuma.pojos.RecordObject;
import xdroid.toaster.Toaster;

public class QueueManager {
    public static boolean isInQueue(String eid) {
        return CacheDB.INSTANCE.queueDAO().isInQueue(eid);
    }

    public static void add(Uri uri, boolean isFile, AnimeObject.WebInfo.AnimeChapter chapter) {
        CacheDB.INSTANCE.queueDAO().add(new QueueObject(uri, isFile, chapter));
        Toaster.toast("Episodio añadido a cola");
    }

    public static void remove(QueueObject queueObject) {
        CacheDB.INSTANCE.queueDAO().remove(queueObject);
    }

    public static void update(QueueObject... objects) {
        CacheDB.INSTANCE.queueDAO().update(objects);
    }

    public static void remove(List<QueueObject> list) {
        CacheDB.INSTANCE.queueDAO().remove(list);
    }

    public static void remove(String eid) {
        CacheDB.INSTANCE.queueDAO().removeByEID(eid);
    }

    public static void removeAll(String aid) {
        CacheDB.INSTANCE.queueDAO().removeByID(aid);
    }

    static void startQueue(Context context, List<QueueObject> list) {
        if (list.size() > 0) {
            markAllSeen(list);
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")
                    || isMxInstalled(context) == null)
                startQueueInternal(context, list);
            else
                startQueueExternal(context, list);
        } else
            Toaster.toast("La lista esta vacía");
    }

    private static void startQueueInternal(Context context, List<QueueObject> list) {
        Intent intent = new Intent(context, ExoPlayer.class)
                .putExtra("isPlayList", true)
                .putExtra("playlist", list.get(0).chapter.aid)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startQueueExternal(Context context, List<QueueObject> list) {
        Uri startUri = list.get(0).isFile ? FileAccessHelper.INSTANCE.getDataUri(list.get(0).chapter.getFileName()) : list.get(0).uri;
        String[] titles = QueueObject.getTitles(list);
        Uri[] uris = QueueObject.getUris(list);
        uris[0] = startUri;
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setPackage(isMxInstalled(context))
                .setDataAndType(startUri, "video/mp4")
                .putExtra("title", titles[0])
                .putExtra("video_list_is_explicit", true)
                .putExtra("video_list", uris)
                .putExtra("video_list.name", titles)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static String isMxInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.mxtech.videoplayer.pro", PackageManager.GET_ACTIVITIES);
            return "com.mxtech.videoplayer.pro";
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            pm.getPackageInfo("com.mxtech.videoplayer.ad", PackageManager.GET_ACTIVITIES);
            return "com.mxtech.videoplayer.ad";
        } catch (PackageManager.NameNotFoundException e) {
        }
        return null;
    }

    private static void markAllSeen(List<QueueObject> list) {
        for (QueueObject object : list) {
            CacheDB.INSTANCE.chaptersDAO().addChapter(object.chapter);
        }
        if (list.size() != 0)
            CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(list.get(list.size() - 1).chapter));
    }
}
