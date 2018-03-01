package knf.kuma.directory;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

import knf.kuma.R;
import knf.kuma.commons.Network;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DirectoryPage;
import pl.droidsonroids.jspoon.Jspoon;

public class DirectoryUpdateService extends IntentService {
    public static int NOT_CODE=5599;
    public static String CHANNEL="directory_update";
    private NotificationManager manager;
    private int count = 0;
    private int page=0;

    public DirectoryUpdateService() {
        super("Directory re-update");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!Network.isConnected())
            stopSelf();
        startForeground(NOT_CODE,getStartNotification());
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
        Jspoon jspoon=Jspoon.create();
        doFullSearch(jspoon,animeDAO);
        cancelForeground();
    }

    private void doFullSearch(Jspoon jspoon,AnimeDAO animeDAO){
        page = 0;
        boolean finished = false;
        while (!finished) {
            if (!Network.isConnected()) {
                Log.e("Directory Getter","Processed "+page+" pages before disconnection");
                stopSelf();
                return;
            }
            try {
                Document document = Jsoup.connect("https://animeflv.net/browse?order=added&page=" + page).cookie("device", "computer").get();
                if (document.select("div.alert.alert-info").size()==0) {
                    page++;
                    List<AnimeObject> animeObjects = jspoon.adapter(DirectoryPage.class).fromHtml(document.outerHtml()).getAnimesRecreate(jspoon, new DirectoryPage.UpdateInterface() {
                        @Override
                        public void onAdd() {
                            count++;
                            updateNotification();
                        }

                        @Override
                        public void onError() {
                            Log.e("Directory Getter", "At page: "+page);
                        }
                    });
                    if (animeObjects.size()>0)
                        animeDAO.insertAll(animeObjects);

                }else {
                    finished=true;
                    Log.e("Directory Getter","Processed "+page+" pages");
                }
            }catch (Exception e){
                Log.e("Directory Getter", "Page error: "+page);
            }
        }
    }

    private void cancelForeground(){
        stopForeground(true);
        manager.cancel(NOT_CODE);
    }

    private void updateNotification(){
        Notification notification=new NotificationCompat.Builder(this,CHANNEL)
                .setOngoing(true)
                .setContentTitle("Actualizando directorio")
                .setContentText("Recreados: "+count)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_dir_update)
                .setColor(Color.parseColor("#e53935"))
                .build();
        manager.notify(NOT_CODE,notification);
    }

    private Notification getStartNotification(){
        return new NotificationCompat.Builder(this,"directory_update")
                .setOngoing(true)
                .setContentTitle("Actualizando directorio")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_dir_update)
                .setColor(Color.parseColor("#e53935"))
                .build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        manager.cancel(NOT_CODE);
        super.onTaskRemoved(rootIntent);
    }
}
