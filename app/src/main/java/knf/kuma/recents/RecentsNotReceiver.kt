package knf.kuma.recents

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import knf.kuma.database.CacheDB
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.pojos.NotificationObj
import org.jetbrains.anko.notificationManager

class RecentsNotReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationDAO = CacheDB.INSTANCE.notificationDAO()
        if (intent.getIntExtra("mode", 0) == 1)
            removeAll(context)
        else {
            notificationDAO.delete(NotificationObj.fromIntent(intent))
            if (notificationDAO.getByType(NotificationObj.RECENT).isEmpty())
                context.notificationManager.cancel(RecentsWork.KEY_SUMMARY)
        }
    }

    companion object {
        fun removeAll(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationDAO = CacheDB.INSTANCE.notificationDAO()
            for (obj in notificationDAO.all)
                manager.cancel(obj.key)
            notificationDAO.clear()
            manager.cancel(RecentsWork.KEY_SUMMARY)
        }
    }
}
