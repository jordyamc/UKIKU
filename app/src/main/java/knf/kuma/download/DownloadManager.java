package knf.kuma.download;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import com.crashlytics.android.Crashlytics;
import com.tonyodev.fetch2.BuildConfig;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import java.io.File;
import java.util.List;

import knf.kuma.R;
import knf.kuma.commons.FileUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.videoservers.ServersFactory;
import okhttp3.OkHttpClient;

public class DownloadManager {
    static final int ACTION_PAUSE = 0;
    static final int ACTION_RESUME = 1;
    static final int ACTION_CANCEL = 2;
    private static final String CHANNEL = "service.Downloads";
    private static final String CHANNEL_ONGOING = "service.Downloads.Ongoing";
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static Fetch fetch;
    private static DownloadsDAO downloadDao = CacheDB.INSTANCE.downloadsDAO();
    private static NotificationManager notificationManager;

    public static void setParalelDownloads(String newValue) {
        if (fetch != null) fetch.setDownloadConcurrentLimit(Integer.parseInt(newValue));
    }

    public static void init(Context context) {
        DownloadManager.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        FetchConfiguration configuration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("max_parallel_downloads", "3")))
                .enableLogging(BuildConfig.DEBUG)
                .enableRetryOnNetworkGain(true)
                .setHttpDownloader(new OkHttpDownloader(new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()))
                .build();
        fetch = Fetch.Impl.getInstance(configuration).addListener(new FetchListener() {
            @Override
            public void onAdded(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.PENDING;
                    downloadDao.update(object);
                }
            }

            @Override
            public void onQueued(Download download, boolean b) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.PENDING;
                    downloadDao.update(object);
                }
            }

            @Override
            public void onWaitingNetwork(Download download) {

            }

            @Override
            public void onCompleted(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    if (FileAccessHelper.INSTANCE.isTempFile(download.getFile())) {
                        object.setEta(-2);
                        downloadDao.update(object);
                        FileUtil.moveFile(object.file, pair -> {
                            if (!pair.second) {
                                object.progress = pair.first;
                                updateNotification(object, false);
                                downloadDao.update(object);
                            } else if (pair.first == -1) {
                                downloadDao.delete(object);
                                errorNotification(object);
                            } else {
                                object.progress = 100;
                                object.state = DownloadObject.COMPLETED;
                                downloadDao.update(object);
                                notificationManager.cancel(Integer.parseInt(object.eid));
                                completedNotification(object);
                            }
                        });
                    } else {
                        object.state = DownloadObject.COMPLETED;
                        downloadDao.update(object);
                        completedNotification(object);
                    }
                }
            }

            @Override
            public void onError(Download download, Error error, Throwable throwable) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null)
                    errorNotification(object);
                downloadDao.delete(object);
                throwable.printStackTrace();
                fetch.delete(download.getId());
                throwable.printStackTrace();
                Crashlytics.logException(throwable);
            }

            @Override
            public void onDownloadBlockUpdated(Download download, DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onStarted(Download download, List<? extends DownloadBlock> list, int i) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.DOWNLOADING;
                    downloadDao.update(object);
                    updateNotification(object, false);
                }
            }

            @Override
            public void onProgress(Download download, long time, long speed) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.DOWNLOADING;
                    object.setEta(time);
                    object.setSpeed(speed);
                    object.progress = download.getProgress();
                    object.t_bytes = download.getTotal();
                    object.d_bytes = download.getDownloaded();
                    downloadDao.update(object);
                    updateNotification(object, false);
                }
            }

            @Override
            public void onPaused(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.PAUSED;
                    object.setEta(-1);
                    downloadDao.update(object);
                    updateNotification(object, true);
                }
            }

            @Override
            public void onResumed(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null) {
                    object.state = DownloadObject.PENDING;
                    downloadDao.update(object);
                    updateNotification(object, false);
                }
            }

            @Override
            public void onCancelled(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null)
                    notificationManager.cancel(object.getDid());
            }

            @Override
            public void onRemoved(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null)
                    notificationManager.cancel(object.getDid());
            }

            @Override
            public void onDeleted(Download download) {
                DownloadObject object = downloadDao.getByDid(download.getId());
                if (object != null)
                    notificationManager.cancel(object.getDid());
            }
        });
    }

    public static boolean start(DownloadObject downloadObject) {
        try {
            File file = FileAccessHelper.INSTANCE.getFileCreate(downloadObject.file);
            Request request = new Request(downloadObject.link, file.getAbsolutePath());
            if (downloadObject.headers != null)
                for (Pair<String, String> header: downloadObject.headers.getHeaders())
                    request.addHeader(header.first, header.second);
            downloadObject.setDid(request.getId());
            downloadDao.insert(downloadObject);
            fetch.enqueue(request, result -> Log.e("Download", "Queued " + result.getId()), result -> {
                if (result.getThrowable() != null) result.getThrowable().printStackTrace();
                downloadDao.delete(downloadObject);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void cancel(String eid) {
        DownloadObject downloadObject = downloadDao.getByEid(eid);
        if (downloadObject != null) {
            downloadDao.delete(downloadObject);
            notificationManager.cancel(Integer.parseInt(downloadObject.eid));
            if (downloadObject.did != null)
                fetch.delete(downloadObject.getDid());
        }
    }

    public static void pause(DownloadObject downloadObject) {
        pause(downloadObject.getDid());
    }

    public static void pause(int did) {
        AsyncTask.execute(() -> fetch.pause(did));
    }

    public static void resume(DownloadObject downloadObject) {
        resume(downloadObject.getDid());
    }

    static void resume(int did) {
        AsyncTask.execute(() -> fetch.resume(did));
    }

    private static void updateNotification(DownloadObject downloadObject, boolean isPaused) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(isPaused ? R.drawable.ic_pause_not : downloadObject.getEta() == -2 ? R.drawable.ic_move : android.R.drawable.stat_sys_download)
                .setContentTitle(downloadObject.name)
                .setContentText(downloadObject.chapter)
                .setOnlyAlertOnce(!isPaused || downloadObject.getEta() == -2)
                .setProgress(100, downloadObject.progress, downloadObject.state == DownloadObject.PENDING)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        if (downloadObject.getEta() != -2) {
            if (isPaused)
                notification.addAction(R.drawable.ic_play_not, "Reanudar", getPending(downloadObject, ACTION_RESUME));
            else
                notification.addAction(R.drawable.ic_pause_not, "Pausar", getPending(downloadObject, ACTION_PAUSE));
            notification.addAction(R.drawable.ic_delete, "Cancelar", getPending(downloadObject, ACTION_CANCEL));
        }
        if (!isPaused)
            notification.setSubText(downloadObject.getSubtext());
        notificationManager.notify(Integer.parseInt(downloadObject.eid), notification.build());
    }

    private static void completedNotification(DownloadObject downloadObject) {
        Notification notification = new NotificationCompat.Builder(context, CHANNEL)
                .setColor(context.getResources().getColor(android.R.color.holo_green_dark))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(downloadObject.name)
                .setContentText(downloadObject.chapter)
                .setContentIntent(ServersFactory.getPlayIntent(context, downloadObject.name, downloadObject.file))
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        notificationManager.notify(Integer.parseInt(downloadObject.eid), notification);
        updateMedia(downloadObject);
    }

    private static void errorNotification(DownloadObject downloadObject) {
        Notification notification = new NotificationCompat.Builder(context, CHANNEL)
                .setColor(context.getResources().getColor(android.R.color.holo_red_dark))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(downloadObject.name)
                .setContentText("Error al descargar " + downloadObject.chapter.toLowerCase())
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        notificationManager.notify(Integer.parseInt(downloadObject.eid), notification);
    }

    private static void updateMedia(DownloadObject downloadObject) {
        try {
            String file = downloadObject.file;
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(FileAccessHelper.INSTANCE.getFile(file))));
            MediaScannerConnection.scanFile(context, new String[]{FileAccessHelper.INSTANCE.getFile(file).getAbsolutePath()}, new String[]{"video/mp4"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PendingIntent getPending(DownloadObject downloadObject, int action) {
        Intent intent = new Intent(context, DownloadReceiver.class)
                .putExtra("did", downloadObject.getDid())
                .putExtra("eid", downloadObject.eid)
                .putExtra("action", action);
        return PendingIntent.getBroadcast(context, downloadObject.key + action, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
