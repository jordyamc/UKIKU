package knf.kuma;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.evernote.android.job.JobManager;

import es.munix.multidisplaycast.CastManager;
import io.fabric.sdk.android.Fabric;
import knf.kuma.commons.CastUtil;
import knf.kuma.directory.DirectoryService;
import knf.kuma.commons.Network;
import knf.kuma.database.CacheDB;
import knf.kuma.downloadservice.DownloadService;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.jobscheduler.JobsCreator;
import knf.kuma.jobscheduler.RecentsJob;

/**
 * Created by Jordy on 03/01/2018.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new JobsCreator());
        Fabric.with(this, new Crashlytics(),new Answers());
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("theme_option","0")));
        CastManager.register(this);
        Network.init(this);
        CacheDB.init(this);
        CastUtil.init(this);
        FileAccessHelper.init(this);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            createChannels();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannels(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationChannel dir_channel=new NotificationChannel(DirectoryService.CHANNEL,getString(R.string.directory_channel_title),NotificationManager.IMPORTANCE_MIN);
            dir_channel.setSound(null, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            manager.createNotificationChannel(dir_channel);
            manager.createNotificationChannel(new NotificationChannel(RecentsJob.CHANNEL_RECENTS,"Capitulos recientes",NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(DownloadService.CHANNEL,"Descargas",NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(DownloadService.CHANNEL_ONGOING,"Descargas en progreso",NotificationManager.IMPORTANCE_LOW));
        }
    }
}
