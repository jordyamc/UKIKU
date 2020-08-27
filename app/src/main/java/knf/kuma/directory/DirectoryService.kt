package knf.kuma.directory

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.database.dao.AnimeDAO
import knf.kuma.download.FileAccessHelper
import knf.kuma.download.foreground
import knf.kuma.download.service
import knf.kuma.jobscheduler.DirUpdateWork
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DirectoryPage
import knf.kuma.widgets.emision.WEmisionProvider
import org.jsoup.HttpStatusException
import pl.droidsonroids.jspoon.Jspoon
import java.util.*
import kotlin.properties.Delegates

class DirectoryService : IntentService("Directory update") {
    private val CURRENT_TIME = System.currentTimeMillis()
    private var manager: NotificationManager? = null
    private var count = 0
    private var page = 0
    private var maxAnimes = 3200
    private val TAG = "Directory Getter"
    private var needCookies by Delegates.notNull<Boolean>()

    private val keyFailedPages = "failed_pages"

    private val startNotification: Notification
        get() {
            val notification = NotificationCompat.Builder(this, "directory_update")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSmallIcon(R.drawable.ic_directory_not)
                    .setSound(null, AudioManager.STREAM_NOTIFICATION)
                    .setColor(ContextCompat.getColor(this, EAHelper.getThemeColor()))
                    .setWhen(CURRENT_TIME)
            if (PrefsUtil.collapseDirectoryNotification)
                notification.setSubText("Verificando directorio")
            else
                notification.setContentTitle("Verificando directorio")
            return notification.build()
        }

    override fun onCreate() {
        foreground(NOT_CODE, startNotification)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground(NOT_CODE, startNotification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        foreground(NOT_CODE, startNotification)
        if (!Network.isConnected) {
            cancelForeground()
            stopSelf()
            return
        }
        needCookies = BypassUtil.isCloudflareActive(BypassUtil.testLink)
        isRunning = true
        setStatus(STATE_VERIFYING)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val animeDAO = CacheDB.INSTANCE.animeDAO()
        count = animeDAO.count
        SSLSkipper.skip()
        val jspoon = Jspoon.create()
        calculateMax()
        setStatus(STATE_PARTIAL)
        doPartialSearch(jspoon, animeDAO)
        setStatus(STATE_FULL)
        doEcchiRemove(animeDAO)
        checkDownloaded(animeDAO)
        doFullSearch(jspoon, animeDAO)
        doEmissionRefresh(jspoon, animeDAO)
        cancelForeground()
    }

    private fun calculateMax() {
        noCrash {
            val main = jsoupCookiesDir("https://animeflv.net/browse",needCookies).get()
            val lastPage = main.select("ul.pagination li:matches(\\d+)").last().text().trim().toInt()
            val last = jsoupCookiesDir("https://animeflv.net/browse?page=$lastPage",needCookies).get()
            maxAnimes = ((24 * (lastPage - 1)) + last.select("article").size) - 1
            Log.e(TAG, "Max pages = $maxAnimes")
        }
    }

    private fun doEcchiRemove(animeDAO: AnimeDAO) {
        if (BuildConfig.BUILD_TYPE == "playstore" || PrefsUtil.isFamilyFriendly) {
            animeDAO.nukeEcchi()
        }
    }

    private fun checkDownloaded(animeDAO: AnimeDAO) {
        val jspoon = Jspoon.create()
        FileAccessHelper.downloadExplorerCreator.createLinksList().forEach {
            if (!animeDAO.existLink("%${it.substringAfterLast(".net")}")){
                try {
                    val response = okHttpCookies(it).execute(followRedirects = true)
                    val body = response.body?.string()
                    if (response.code == 200 && body != null) {
                        val webInfo = jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(body)
                        animeDAO.insert(AnimeObject(it, webInfo))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (needCookies)
                    Thread.sleep(5000)
            }
        }
    }

    private fun doEmissionRefresh(jspoon: Jspoon, animeDAO: AnimeDAO) {
        try {
            animeDAO.allLinksInEmission.forEach {
                try {
                    val animeObject = AnimeObject(it, jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(jsoupCookiesDir(it,needCookies).get().outerHtml()))
                    val current = animeDAO.getAnimeByAid(animeObject.aid)
                    if (current == null || current != animeObject)
                        animeDAO.updateAnime(animeObject)
                    WEmisionProvider.update(this)
                } catch (e: Exception) {
                    return@forEach
                }
            }
        }catch (e:Exception){
            //
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun doPartialSearch(jspoon: Jspoon, animeDAO: AnimeDAO) {
        val strings = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(keyFailedPages, LinkedHashSet())
        val newStrings = LinkedHashSet<String>()
        var partialCount = 0
        if (strings?.size == 0)
            Log.e(TAG, "No pending pages")
        for (s in LinkedHashSet(strings)) {
            partialCount++
            if (!Network.isConnected) {
                Log.e(TAG, "Processed $partialCount pages before disconnection")
                stopSelf()
                return
            }
            try {
                if (needCookies)
                    Thread.sleep(6000)
                else
                    Thread.sleep(1000)
                val document = jsoupCookiesDir("https://animeflv.net/browse?order=added&page=$s", needCookies).get()
                if (document.select("article").size != 0) {
                    val animeObjects = jspoon.adapter(DirectoryPage::class.java).fromHtml(document.outerHtml()).getAnimes(animeDAO, jspoon, object : DirectoryPage.UpdateInterface {
                        override fun onAdd() {
                            count++
                            updateNotification()
                        }

                        override fun onError() {
                            Log.e(TAG, "Error at page: $s")
                            if (!newStrings.contains(s))
                                newStrings.add(s.toString())
                        }
                    }, needCookies)
                    if (animeObjects.isNotEmpty())
                        animeDAO.insertAll(animeObjects)
                }
            } catch (e: HttpStatusException) {
                if (e.statusCode == 403 || e.statusCode == 503) {
                    setStatus(STATE_INTERRUPTED)
                    stopSelf()
                    cancelForeground()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Page error: $s | ${e.message}")
                if (!newStrings.contains(s.toString()))
                    newStrings.add(s.toString())
            }

        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet(keyFailedPages, newStrings).commit()
    }

    private fun doFullSearch(jspoon: Jspoon, animeDAO: AnimeDAO) {
        page = 1
        var finished = false
        val strings = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(keyFailedPages, LinkedHashSet())
        while (!finished) {
            if (!Network.isConnected) {
                Log.e(TAG, "Processed $page pages before disconnection")
                stopSelf()
                return
            }
            try {
                if (needCookies)
                    Thread.sleep(6000)
                val document = jsoupCookiesDir("https://animeflv.net/browse?order=added&page=$page", needCookies).get()
                Log.e(TAG, "Read page $page")
                if (document.select("ul.ListAnimes").isNotEmpty() && document.select("article").isNotEmpty()) {
                    page++
                    val animeObjects = jspoon.adapter(DirectoryPage::class.java).fromHtml(document.outerHtml()).getAnimes(animeDAO, jspoon, object : DirectoryPage.UpdateInterface {
                        override fun onAdd() {
                            count++
                            updateNotification()
                        }

                        override fun onError() {
                            Log.e(TAG, "At page: $page")
                            if (strings?.contains(page.toString()) == false)
                                strings.add(page.toString())
                        }
                    }, needCookies)
                    if (animeObjects.isNotEmpty()) {
                        animeDAO.insertAll(animeObjects)
                    } else if (PrefsUtil.isDirectoryFinished || animeDAO.count >= maxAnimes) {
                        PrefsUtil.isDirectoryFinished = animeDAO.count >= maxAnimes
                        Log.e(TAG, "Stop searching at page $page")
                        cancelForeground()
                        break
                    }
                } else {
                    finished = true
                    Log.e(TAG, "Processed $page pages")
                    PrefsUtil.isDirectoryFinished = animeDAO.count >= maxAnimes
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet(keyFailedPages, strings).apply()
                    DirUpdateWork.schedule(this)
                    setStatus(STATE_FINISHED)
                }
            } catch (e: HttpStatusException) {
                Log.e(TAG, "Page error: $page | Code: ${e.statusCode}")
                if (e.statusCode == 403 || e.statusCode == 503) {
                    e.printStackTrace()
                    finished = true
                    setStatus(STATE_INTERRUPTED)
                    stopSelf()
                    cancelForeground()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Page error: $page | ${e.message}")
                if (strings?.contains(page.toString()) == false)
                    strings.add(page.toString())
                page++
                if (page > maxAnimes / 24){
                    finished = true
                    setStatus(STATE_INTERRUPTED)
                }
            }
        }
    }

    private fun cancelForeground() {
        noCrash {
            isRunning = false
            stopForeground(true)
            notCancel(NOT_CODE)
        }
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL).apply {
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_MIN
            setSmallIcon(R.drawable.ic_directory_not)
            color = ContextCompat.getColor(this@DirectoryService, EAHelper.getThemeColor())
            setWhen(CURRENT_TIME)
            setSound(null)
            if (PrefsUtil.collapseDirectoryNotification)
                setSubText("Creando directorio: $count/$maxAnimes~")
            else {
                setContentTitle("Creando directorio")
                setContentText("Agregados: $count/$maxAnimes~")
                if (maxAnimes > 0)
                    setProgress(maxAnimes, count, false)
            }
        }
        notShow(NOT_CODE, notification.build())
    }

    private fun setStatus(status: Int) {
        doOnUI { liveStatus.setValue(status) }
    }

    private fun notShow(code: Int, notification: Notification) {
        manager?.notify(code, notification)
    }

    private fun notCancel(code: Int) {
        manager?.cancel(code)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        cancelForeground()
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

        fun run(context: Context?) {
            if (context == null) return
            if (!isRunning)
                context.service(Intent(context, DirectoryService::class.java))
        }

        fun getLiveStatus(): LiveData<Int> {
            return liveStatus
        }
    }
}
