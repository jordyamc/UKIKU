package knf.kuma.downloadservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.concurrent.TimeUnit;

import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.queue.QueueManager;
import knf.kuma.videoservers.ServersFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends IntentService {
    public static final String CHANNEL = "service.Downloads";
    public static final String CHANNEL_ONGOING = "service.Downloads.Ongoing";
    private static final int DOWNLOADING_ID = 8879;
    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();

    private NotificationManager manager;

    private DownloadObject current;

    private String file;
    private int bufferSize = PrefsUtil.bufferSize();

    public DownloadService() {
        super("Download service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent == null)
            return;
        current = downloadsDAO.getByEid(intent.getStringExtra("eid"));
        if (current == null)
            return;
        file = current.file;
        startForeground(DOWNLOADING_ID, getStartNotification());
        try {
            Request.Builder request = new Request.Builder()
                    .url(intent.getDataString());
            if (current.headers != null)
                for (Pair<String, String> pair : current.headers.getHeaders()) {
                    request.addHeader(pair.first, pair.second);
                }
            Response response = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS).build().newCall(request.build()).execute();
            current.t_bytes = response.body().contentLength();
            BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream());
            BufferedOutputStream outputStream;
            if (response.code() == 200 || response.code() == 206) {
                outputStream = new BufferedOutputStream(FileAccessHelper.INSTANCE.getOutputStream(current.file), bufferSize * 1024);
            } else {
                Log.e("Download error", "Code: " + response.code());
                errorNotification();
                downloadsDAO.delete(current);
                QueueManager.remove(current.eid);
                response.close();
                cancelForeground();
                return;
            }
            current.state = DownloadObject.DOWNLOADING;
            downloadsDAO.update(current);
            byte data[] = new byte[bufferSize * 1024];
            int count;
            while ((count = inputStream.read(data, 0, bufferSize * 1024)) >= 0) {
                DownloadObject revised = downloadsDAO.getByEid(intent.getStringExtra("eid"));
                if (revised == null) {
                    FileAccessHelper.INSTANCE.delete(file);
                    downloadsDAO.delete(current);
                    QueueManager.remove(current.eid);
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
            response.close();
            completedNotification();
        } catch (Exception e) {
            e.printStackTrace();
            FileAccessHelper.INSTANCE.delete(file);
            downloadsDAO.delete(current);
            QueueManager.remove(current.eid);
            errorNotification();
        }
    }

    private void updateNotification() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ONGOING)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setProgress(100, current.progress, false)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        int pending = downloadsDAO.countPending();
        if (pending > 0)
            notification.setSubText(pending + " " + (pending == 1 ? "pendiente" : "pendientes"));
        manager.notify(DOWNLOADING_ID, notification.build());
    }

    private void completedNotification() {
        current.state = DownloadObject.COMPLETED;
        downloadsDAO.update(current);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                .setColor(getResources().getColor(android.R.color.holo_green_dark))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setContentIntent(ServersFactory.getPlayIntent(this, current.name, file))
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        manager.notify(Integer.parseInt(current.eid), notification);
        updateMedia();
        cancelForeground();
    }

    private void updateMedia() {
        try {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(FileAccessHelper.INSTANCE.getFile(file))));
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{FileAccessHelper.INSTANCE.getFile(file).getAbsolutePath()}, new String[]{"video/mp4"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void errorNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                .setColor(getResources().getColor(android.R.color.holo_red_dark))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(current.name)
                .setContentText("Error al descargar " + current.chapter.toLowerCase())
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        manager.notify(Integer.parseInt(current.eid), notification);
        cancelForeground();
    }

    private Notification getStartNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ONGOING)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(current.name)
                .setContentText(current.chapter)
                .setProgress(100, current.progress, true)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void cancelForeground() {
        stopForeground(true);
        if (manager != null)
            manager.cancel(DOWNLOADING_ID);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        cancelForeground();
        FileAccessHelper.INSTANCE.delete(file);
        if (current != null) {
            if (manager != null)
                errorNotification();
            downloadsDAO.delete(current);
            QueueManager.remove(current.eid);
        }
        super.onTaskRemoved(rootIntent);
    }
}
