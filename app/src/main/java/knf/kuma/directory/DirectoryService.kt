package knf.kuma.directory

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.database.dao.AnimeDAO
import knf.kuma.jobscheduler.DirUpdateJob
import knf.kuma.pojos.DirectoryPage
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import pl.droidsonroids.jspoon.Jspoon
import java.util.*

class DirectoryService : IntentService("Directory update") {
    private val CURRENT_TIME = System.currentTimeMillis()
    private var manager: NotificationManager? = null
    private var count = 0
    private var page = 0

    private val startNotification: Notification
        get() {
            val notification = NotificationCompat.Builder(this, "directory_update")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSmallIcon(R.drawable.ic_directory_not)
                    .setSound(null, AudioManager.STREAM_NOTIFICATION)
                    .setColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
                    .setWhen(CURRENT_TIME)
            if (PrefsUtil.collapseDirectoryNotification)
                notification.setSubText("Verificando directorio")
            else
                notification.setContentTitle("Verificando directorio")
            return notification.build()
        }

    override fun onHandleIntent(intent: Intent?) {
        startForeground(NOT_CODE, startNotification)
        isRunning = true
        if (!Network.isConnected) {
            stopSelf()
            cancelForeground()
        }
        setStatus(STATE_VERIFYING)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val animeDAO = CacheDB.INSTANCE.animeDAO()
        if (!PrefsUtil.isDirectoryFinished)
            count = animeDAO.count
        SSLSkipper.skip()
        val jspoon = Jspoon.create()
        setStatus(STATE_PARTIAL)
        doPartialSearch(jspoon, animeDAO)
        setStatus(STATE_FULL)
        doFullSearch(jspoon, animeDAO)
        cancelForeground()
    }

    @SuppressLint("ApplySharedPref")
    private fun doPartialSearch(jspoon: Jspoon, animeDAO: AnimeDAO) {
        val strings = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("failed_pages", LinkedHashSet())
        val newStrings = LinkedHashSet<String>()
        var partialCount = 0
        if (strings!!.size == 0)
            Log.e("Directory Getter", "No pending pages")
        for (s in LinkedHashSet(strings)) {
            partialCount++
            if (!Network.isConnected) {
                Log.e("Directory Getter", "Processed $partialCount pages before disconnection")
                stopSelf()
                return
            }
            try {
                val document = Jsoup.connect("https://animeflv.net/browse?order=added&page=$s").cookies(BypassUtil.getMapCookie(this)).userAgent(BypassUtil.userAgent).get()
                if (document.select("article").size != 0) {
                    val animeObjects = jspoon.adapter(DirectoryPage::class.java).fromHtml(document.outerHtml()).getAnimes(this, animeDAO, jspoon, object : DirectoryPage.UpdateInterface {
                        override fun onAdd() {
                            count++
                            updateNotification()
                        }

                        override fun onError() {
                            Log.e("Directory Getter", "At page: $s")
                            if (!newStrings.contains(s))
                                newStrings.add(s.toString())
                        }
                    })
                    if (animeObjects.isNotEmpty())
                        animeDAO.insertAll(animeObjects)
                }
            } catch (e: Exception) {
                Log.e("Directory Getter", "Page error: $s")
                if (!newStrings.contains(s.toString()))
                    newStrings.add(s.toString())
            }

        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet("failed_pages", newStrings).commit()
    }

    private fun doFullSearch(jspoon: Jspoon, animeDAO: AnimeDAO) {
        page = 0
        var finished = false
        val strings = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("failed_pages", LinkedHashSet())
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
                    val animeObjects = jspoon.adapter(DirectoryPage::class.java).fromHtml(document.outerHtml()).getAnimes(this, animeDAO, jspoon, object : DirectoryPage.UpdateInterface {
                        override fun onAdd() {
                            count++
                            updateNotification()
                        }

                        override fun onError() {
                            Log.e("Directory Getter", "At page: $page")
                            if (!strings!!.contains(page.toString()))
                                strings.add(page.toString())
                        }
                    })
                    if (animeObjects.isNotEmpty()) {
                        animeDAO.insertAll(animeObjects)
                    } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("directory_finished", false)) {
                        Log.e("Directory Getter", "Stop searching at page $page")
                        cancelForeground()
                        break
                    }
                } else {
                    finished = true
                    Log.e("Directory Getter", "Processed $page pages")
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("directory_finished", true).apply()
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet("failed_pages", strings).apply()
                    DirUpdateJob.schedule(this)
                    setStatus(STATE_FINISHED)
                }
            } catch (e: HttpStatusException) {
                finished = true
                setStatus(STATE_INTERRUPTED)
            } catch (e: Exception) {
                Log.e("Directory Getter", "Page error: $page")
                if (!strings!!.contains(page.toString()))
                    strings.add(page.toString())
            }

        }
        cancelForeground()
    }

    private fun cancelForeground() {
        isRunning = false
        stopForeground(true)
        notCancel(NOT_CODE)
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_directory_not)
                .setColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
                .setWhen(CURRENT_TIME)
                .setSound(null)
        if (PrefsUtil.collapseDirectoryNotification)
            notification.setSubText("Creando directorio: $count")
        else
            notification
                    .setContentTitle("Creando directorio")
                    .setContentText("Agregados: $count")
        notShow(NOT_CODE, notification.build())
    }

    private fun setStatus(status: Int) {
        launch(UI) { liveStatus.setValue(status) }
    }

    private fun notShow(code: Int, notification: Notification) {
        if (manager != null)
            manager!!.notify(code, notification)
    }

    private fun notCancel(code: Int) {
        if (manager != null)
            manager!!.cancel(code)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        isRunning = false
        stopForeground(true)
        notCancel(NOT_CODE)
        super.onTaskRemoved(rootIntent)
    }

    interface OnDirStatus {
        fun onFinished()
    }

    companion object {
        const val STATE_PARTIAL = 0
        const val STATE_FULL = 1
        const val STATE_FINISHED = 2
        const val STATE_INTERRUPTED = 3
        const val STATE_VERIFYING = 4
        var NOT_CODE = 5598
        var CHANNEL = "directory_update"
        var isRunning = false
            private set
        private val liveStatus = MutableLiveData<Int>()

        fun run(context: Context) {
            if (!isRunning)
                ContextCompat.startForegroundService(context, Intent(context, DirectoryService::class.java))
        }

        fun isDirectoryFinished(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("directory_finished", false)
        }

        fun getLiveStatus(): LiveData<Int> {
            return liveStatus
        }
    }
}
