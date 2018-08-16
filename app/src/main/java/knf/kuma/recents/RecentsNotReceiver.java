package knf.kuma.recents;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.pojos.NotificationObj;

public class RecentsNotReceiver extends BroadcastReceiver {
    public static void removeAll(@NotNull Context context) {
        NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        NotificationDAO notificationDAO = CacheDB.INSTANCE.notificationDAO();
        for (NotificationObj obj : notificationDAO.getAll()) {
            manager.cancel(obj.key);
        }
        notificationDAO.clear();
        manager.cancel(RecentsJob.KEY_SUMMARY);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationDAO notificationDAO = CacheDB.INSTANCE.notificationDAO();
        if (intent.getIntExtra("mode", 0) == 1) {
            removeAll(context);
        } else {
            notificationDAO.delete(NotificationObj.fromIntent(intent));
            List<NotificationObj> objs = notificationDAO.getByType(NotificationObj.RECENT);
            if (objs.size() == 0)
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(RecentsJob.KEY_SUMMARY);
        }
    }
}
