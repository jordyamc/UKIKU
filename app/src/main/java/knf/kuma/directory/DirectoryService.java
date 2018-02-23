package knf.kuma.directory;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import knf.kuma.R;
import knf.kuma.commons.Network;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DirectoryPage;
import pl.droidsonroids.jspoon.Jspoon;

/**
 * Created by Jordy on 06/01/2018.
 */

public class DirectoryService extends IntentService {
    public static int NOT_CODE=5598;
    public static String CHANNEL="directory_update";
    private NotificationManager manager;
    private int count = 0;
    private int page=0;

    public DirectoryService() {
        super("Directory update");
    }

    public static boolean isDirectoryFinished(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("directory_finished", false);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        startForeground(NOT_CODE,getStartNotification());
        if (!Network.isConnected()) {
            stopSelf();
            stopForeground(true);
            notCancel(NOT_CODE);
        }
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
        Jspoon jspoon=Jspoon.create();
        doPartialSearch(jspoon,animeDAO);
        doFullSearch(jspoon,animeDAO);
        cancelForeground();
    }

    @SuppressLint("ApplySharedPref")
    private void doPartialSearch(Jspoon jspoon, AnimeDAO animeDAO){
        final Set<String> strings=PreferenceManager.getDefaultSharedPreferences(this).getStringSet("failed_pages",new LinkedHashSet<String>());
        final Set<String> newStrings=new LinkedHashSet<>();
        int partialCount=0;
        if (strings.size()==0)
            Log.e("Directory Getter","No pending pages");
        for (final String s:strings){
            partialCount++;
            if (!Network.isConnected()) {
                Log.e("Directory Getter","Processed "+partialCount+" pages before disconnection");
                stopSelf();
                return;
            }
            try {
                Document document = Jsoup.connect("https://animeflv.net/browse?order=added&page=" + s).cookie("device", "computer").get();
                if (document.select("div.alert.alert-info").size()==0) {
                    List<AnimeObject> animeObjects = jspoon.adapter(DirectoryPage.class).fromHtml(document.outerHtml()).getAnimes(animeDAO, jspoon, new DirectoryPage.UpdateInterface() {
                        @Override
                        public void onAdd() {
                            count++;
                            updateNotification();
                        }

                        @Override
                        public void onError() {
                            Log.e("Directory Getter", "At page: "+s);
                            if (!newStrings.contains(s))
                                newStrings.add(String.valueOf(s));
                        }
                    });
                    if (animeObjects.size()>0)
                        animeDAO.insertAll(animeObjects);
                }
            }catch (Exception e){
                Log.e("Directory Getter", "Page error: "+s);
                if (!newStrings.contains(String.valueOf(s)))
                    newStrings.add(String.valueOf(s));
            }
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet("failed_pages",newStrings).commit();
    }

    private void doFullSearch(Jspoon jspoon,AnimeDAO animeDAO){
        page = 0;
        boolean finished = false;
        final Set<String> strings=PreferenceManager.getDefaultSharedPreferences(this).getStringSet("failed_pages",new LinkedHashSet<String>());
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
                    List<AnimeObject> animeObjects = jspoon.adapter(DirectoryPage.class).fromHtml(document.outerHtml()).getAnimes(animeDAO, jspoon, new DirectoryPage.UpdateInterface() {
                        @Override
                        public void onAdd() {
                            count++;
                            updateNotification();
                        }

                        @Override
                        public void onError() {
                            Log.e("Directory Getter", "At page: "+page);
                            if (!strings.contains(String.valueOf(page)))
                                strings.add(String.valueOf(page));
                        }
                    });
                    if (animeObjects.size()>0) {
                        animeDAO.insertAll(animeObjects);
                    }else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("directory_finished",false)){
                        Log.e("Directory Getter","Stop searching at page "+page);
                        cancelForeground();
                        break;
                    }
                }else {
                    finished=true;
                    Log.e("Directory Getter","Processed "+page+" pages");
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("directory_finished",true).apply();
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet("failed_pages",strings).apply();
                    DirUpdateJob.schedule(this);
                }
            }catch (Exception e){
                Log.e("Directory Getter", "Page error: "+page);
                if (!strings.contains(String.valueOf(page)))
                    strings.add(String.valueOf(page));
            }
        }
    }

    private void cancelForeground(){
        stopForeground(true);
        notCancel(NOT_CODE);
    }

    private void updateNotification(){
        Notification notification=new NotificationCompat.Builder(this,CHANNEL)
                .setOngoing(true)
                .setContentTitle("Creando directorio")
                .setContentText("Agregados: "+count)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_directory_not)
                .setColor(Color.parseColor("#e53935"))
                .setSound(null)
                .build();
        notShow(NOT_CODE, notification);
    }

    private Notification getStartNotification(){
        return new NotificationCompat.Builder(this,"directory_update")
                .setOngoing(true)
                .setContentTitle("Verificando directorio")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_directory_not)
                .setSound(null,AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setColor(Color.parseColor("#e53935"))
                .build();
    }

    private void notShow(int code, Notification notification) {
        if (manager != null)
            manager.notify(code, notification);
    }

    private void notCancel(int code) {
        if (manager != null)
            manager.cancel(code);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        notCancel(NOT_CODE);
        super.onTaskRemoved(rootIntent);
    }
}
