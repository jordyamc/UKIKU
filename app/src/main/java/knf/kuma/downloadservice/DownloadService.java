package knf.kuma.downloadservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.NotificationObj;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Jordy on 10/01/2018.
 */

public class DownloadService extends IntentService {
    public static final String CHANNEL="service.Downloads";
    public static final String CHANNEL_ONGOING="service.Downloads.Ongoing";
    private static final int DOWNLOADING_ID=8879;
    private DownloadsDAO downloadsDAO= CacheDB.INSTANCE.downloadsDAO();

    private NotificationManager manager;

    private DownloadObject current;

    private String eid;

    private String file;

    public DownloadService() {
        super("Download service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        eid=intent.getStringExtra("eid");
        current=downloadsDAO.getByEid(eid);
        if (current==null)
            return;
        file=current.file;
        startForeground(DOWNLOADING_ID,getStartNotification());
        try {
            Request.Builder request = new Request.Builder()
                    .url(intent.getDataString());
            if (intent.getBooleanExtra("constructor",false)){
                request.addHeader("Cookie",intent.getStringExtra("cookie"));
                request.addHeader("Referer",intent.getStringExtra("referer"));
                request.addHeader("User-Agent",intent.getStringExtra("ua"));
            }
            Response response = new OkHttpClient().newCall(request.build()).execute();
            current.t_bytes = response.body().contentLength();
            BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream());
            BufferedOutputStream outputStream;
            if (response.code() == 200) {
                outputStream = new BufferedOutputStream(FileAccessHelper.INSTANCE.getOutputStream(current.file), 1024);
            } else {
                downloadsDAO.delete(current);
                return;
            }
            current.state = DownloadObject.DOWNLOADING;
            downloadsDAO.update(current);
            byte data[] = new byte[1024];
            int count;
            while ((count = inputStream.read(data, 0, 1024)) >= 0) {
                DownloadObject revised = downloadsDAO.getByEid(eid);
                if (revised == null) {
                    FileAccessHelper.INSTANCE.delete(file);
                    downloadsDAO.delete(current);
                    cancelForeground();
                    return;
                }
                outputStream.write(data, 0, count);
                current.d_bytes += count;
                int prog = (int) ((current.d_bytes * 100) / current.t_bytes);
                if (prog > current.progress) {
                    current.progress = prog;
                    updateNotification();
                    downloadsDAO.update(current);
                }
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            completedNotification();
        }catch (Exception e){
            e.printStackTrace();
            FileAccessHelper.INSTANCE.getFile(file).delete();
            downloadsDAO.delete(current);
            errorNotification();
        }
    }

    private void updateNotification(){
        Notification notification=new NotificationCompat.Builder(this,CHANNEL_ONGOING)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setProgress(100,current.progress,false)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        manager.notify(DOWNLOADING_ID,notification);
    }

    private void completedNotification(){
        current.state= DownloadObject.COMPLETED;
        downloadsDAO.update(current);
        Notification notification=new NotificationCompat.Builder(this,CHANNEL)
                .setColor(getResources().getColor(android.R.color.holo_green_dark))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        manager.notify(Integer.parseInt(current.eid),notification);
        cancelForeground();
    }

    private void errorNotification(){
        Notification notification=new NotificationCompat.Builder(this,CHANNEL)
                .setColor(getResources().getColor(android.R.color.holo_red_dark))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(current.name)
                .setContentText("Error al descargar "+current.chapter.toLowerCase())
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        manager.notify(Integer.parseInt(current.eid),notification);
        cancelForeground();
    }

    private Notification getStartNotification(){
        return new NotificationCompat.Builder(this,CHANNEL_ONGOING)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setProgress(100,current.progress,true)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void cancelForeground(){
        stopForeground(true);
        manager.cancel(DOWNLOADING_ID);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        cancelForeground();
        errorNotification();
        FileAccessHelper.INSTANCE.delete(file);
        downloadsDAO.delete(current);
        super.onTaskRemoved(rootIntent);
    }
}
