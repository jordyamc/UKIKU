package knf.kuma.recents;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.jobscheduler.RecentsJob;

/**
 * Created by Jordy on 09/01/2018.
 */

public class RecentsNotReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationDAO notificationDAO= CacheDB.INSTANCE.notificationDAO();
        if (intent.getIntExtra("mode",0)==1){
            removeAll(context);
        }else {
            notificationDAO.delete(NotificationObj.fromIntent(intent));
            List<NotificationObj> objs=notificationDAO.getByType(NotificationObj.RECENT);
            if (objs.size()<=1)
                ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(RecentsJob.KEY_SUMMARY);
        }
    }

    public static void removeAll(Context context){
        NotificationManager manager=((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE));
        NotificationDAO notificationDAO= CacheDB.INSTANCE.notificationDAO();
        for (NotificationObj obj:notificationDAO.getAll()){
            manager.cancel(obj.key);
        }
        notificationDAO.clear();
        manager.cancel(RecentsJob.KEY_SUMMARY);
    }
}
