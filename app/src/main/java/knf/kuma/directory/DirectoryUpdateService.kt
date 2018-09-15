package knf.kuma.directory

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.database.dao.AnimeDAO
import knf.kuma.pojos.DirectoryPage
import org.jsoup.Jsoup
import pl.droidsonroids.jspoon.Jspoon

class DirectoryUpdateService : IntentService("Directory re-update") {
    private val CURRENT_TIME = System.currentTimeMillis()
    private var manager: NotificationManager? = null
    private var count = 0
    private var page = 0

    private val startNotification: Notification
        get() {
            val notification = NotificationCompat.Builder(this, CHANNEL)
                    .setOngoing(true)
                    .setSubText("Actualizando directorio")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSmallIcon(R.drawable.ic_dir_update)
                    .setColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
                    .setWhen(CURRENT_TIME)

            return notification.build()
        }

    override fun onHandleIntent(intent: Intent?) {
        if (!Network.isConnected)
            stopSelf()
        isRunning = true
        startForeground(NOT_CODE, startNotification)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val animeDAO = CacheDB.INSTANCE.animeDAO()
        val jspoon = Jspoon.create()
        doFullSearch(jspoon, animeDAO)
        cancelForeground()
    }

    private fun doFullSearch(jspoon: Jspoon, animeDAO: AnimeDAO) {
        page = 0
        var finished = false
        while (!finished) {
            if (!Network.isConnected) {
                Log.e("Directory Getter", "Processed $page pages before disconnection")
                stopSelf()
                return
            }
            try {
                val document = Jsoup.connect("https://animeflv.net/browse?order=added&page=$page").cookies(BypassUtil.getMapCookie(this)).userAgent(BypassUtil.userAgent).get()
                if (document.select("article").size != 0) {
                    page++
                    val animeObjects = jspoon.adapter(DirectoryPage::class.java).fromHtml(document.outerHtml()).getAnimesRecreate(this, jspoon, object : DirectoryPage.UpdateInterface {
                        override fun onAdd() {
                            count++
                            updateNotification()
                        }

                        override fun onError() {
                            Log.e("Directory Getter", "At page: $page")
                        }
                    })
                    if (animeObjects.isNotEmpty())
                        animeDAO.insertAll(animeObjects)
                } else {
                    finished = true
                    Log.e("Directory Getter", "Processed $page pages")
                }
            } catch (e: Exception) {
                Log.e("Directory Getter", "Page error: $page")
            }

        }
        cancelForeground()
    }

    private fun cancelForeground() {
        isRunning = false
        stopForeground(true)
        if (manager != null)
            manager!!.cancel(NOT_CODE)
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_dir_update)
                .setColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
                .setWhen(CURRENT_TIME)
        if (PrefsUtil.collapseDirectoryNotification)
            notification.setSubText("Actualizando directorio: $count")
        else
            notification
                    .setContentTitle("Actualizando directorio")
                    .setContentText("Actualizados: $count")
        manager!!.notify(NOT_CODE, notification.build())
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        cancelForeground()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        var NOT_CODE = 5599
        var CHANNEL = "directory_update"
        var isRunning = false
            private set
    }
}
