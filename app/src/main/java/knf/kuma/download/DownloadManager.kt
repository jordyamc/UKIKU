package knf.kuma.download

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.util.Pair
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import knf.kuma.R
import knf.kuma.commons.FileUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.DownloadObject
import knf.kuma.videoservers.ServersFactory
import okhttp3.OkHttpClient
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class DownloadManager : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null && intent.action == "stop.foregrouns") {
            stopForeground(true)
            stopSelf()
        }
        return Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(23498, foregroundNotification())
    }

    companion object {
        const val CHANNEL_FOREGROUND = "service.LifeSaver"
        internal const val ACTION_PAUSE = 0
        internal const val ACTION_RESUME = 1
        internal const val ACTION_CANCEL = 2
        private const val CHANNEL = "service.Downloads"
        private const val CHANNEL_ONGOING = "service.Downloads.Ongoing"
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
        private var fetch: Fetch? = null
        private val downloadDao = CacheDB.INSTANCE.downloadsDAO()
        private var notificationManager: NotificationManager? = null

        fun setParallelDownloads(newValue: String) {
            if (fetch != null) fetch!!.setDownloadConcurrentLimit(Integer.parseInt(newValue))
        }

        fun init(context: Context) {
            DownloadManager.context = context
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val configuration = FetchConfiguration.Builder(context)
                    .setDownloadConcurrentLimit(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("max_parallel_downloads", "3")!!))
                    .enableLogging(BuildConfig.DEBUG)
                    .enableRetryOnNetworkGain(true)
                    .setHttpDownloader(OkHttpDownloader(OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()))
                    .build()
            fetch = Fetch.getInstance(configuration).addListener(object : FetchListener {
                override fun onAdded(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.PENDING
                        downloadDao.update(downloadObject)
                    }
                }

                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.PENDING
                        downloadDao.update(downloadObject)
                    }
                }

                override fun onWaitingNetwork(download: Download) {

                }

                override fun onCompleted(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        if (FileAccessHelper.INSTANCE.isTempFile(download.file)) {
                            downloadObject.setEta(-2)
                            downloadDao.update(downloadObject)
                            FileUtil.moveFile(downloadObject.file!!, object : FileUtil.MoveCallback {
                                override fun onProgress(pair: Pair<Int, Boolean>) {
                                    if (!pair.second) {
                                        downloadObject.progress = pair.first
                                        updateNotification(downloadObject, false)
                                        downloadDao.update(downloadObject)
                                    } else if (pair.first == -1) {
                                        downloadDao.delete(downloadObject)
                                        errorNotification(downloadObject)
                                    } else {
                                        downloadObject.progress = 100
                                        downloadObject.state = DownloadObject.COMPLETED
                                        downloadDao.update(downloadObject)
                                        notificationManager!!.cancel(downloadObject.eid!!.toInt())
                                        completedNotification(downloadObject)
                                    }
                                    stopIfNeeded()
                                }
                            })
                        } else {
                            downloadObject.state = DownloadObject.COMPLETED
                            downloadDao.update(downloadObject)
                            completedNotification(downloadObject)
                        }
                    }
                    stopIfNeeded()
                }

                override fun onError(download: Download, error: Error, throwable: Throwable?) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        errorNotification(downloadObject)
                        downloadDao.delete(downloadObject)
                        throwable!!.printStackTrace()
                        fetch!!.delete(download.id)
                        throwable.printStackTrace()
                        Crashlytics.logException(throwable)
                        stopIfNeeded()
                    }
                }

                override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {

                }

                override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.DOWNLOADING
                        downloadDao.update(downloadObject)
                        updateNotification(downloadObject, false)
                    }
                    ContextCompat.startForegroundService(context, Intent(context, DownloadManager::class.java))
                }

                override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.DOWNLOADING
                        downloadObject.setEta(etaInMilliSeconds)
                        downloadObject.setSpeed(downloadedBytesPerSecond)
                        downloadObject.progress = download.progress
                        downloadObject.t_bytes = download.total
                        downloadObject.d_bytes = download.downloaded
                        downloadDao.update(downloadObject)
                        updateNotification(downloadObject, false)
                    }
                }

                override fun onPaused(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.PAUSED
                        downloadObject.setEta(-1)
                        downloadDao.update(downloadObject)
                        updateNotification(downloadObject, true)
                    }
                    stopIfNeeded()
                }

                override fun onResumed(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null) {
                        downloadObject.state = DownloadObject.PENDING
                        downloadObject.time = System.currentTimeMillis()
                        downloadDao.update(downloadObject)
                        updateNotification(downloadObject, false)
                    }
                }

                override fun onCancelled(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null)
                        notificationManager!!.cancel(downloadObject.getDid())
                    stopIfNeeded()
                }

                override fun onRemoved(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null)
                        notificationManager!!.cancel(downloadObject.getDid())
                    stopIfNeeded()
                }

                override fun onDeleted(download: Download) {
                    val downloadObject = downloadDao.getByDid(download.id)
                    if (downloadObject != null)
                        notificationManager!!.cancel(downloadObject.getDid())
                    stopIfNeeded()
                }
            })
        }

        fun start(downloadObject: DownloadObject): Boolean {
            try {
                val file = FileAccessHelper.INSTANCE.getFileCreate(downloadObject.file!!)
                val request = Request(downloadObject.link!!, file!!.absolutePath)
                if (downloadObject.headers != null)
                    for (header in downloadObject.headers!!.headers)
                        request.addHeader(header.first, header.second)
                downloadObject.setDid(request.id)
                downloadObject.canResume = true
                downloadDao.insert(downloadObject)
                fetch!!.enqueue(request, Func { Log.e("Download", "Queued " + it.id) }, Func {
                    if (it.throwable != null) it.throwable!!.printStackTrace()
                    downloadDao.delete(downloadObject)
                })
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al iniciar descarga")
                return false
            }

        }

        fun cancel(eid: String) {
            val downloadObject = downloadDao.getByEid(eid)
            if (downloadObject != null) {
                downloadDao.delete(downloadObject)
                notificationManager!!.cancel(downloadObject.eid!!.toInt())
                if (downloadObject.did != null)
                    fetch!!.delete(downloadObject.getDid())
            }
        }

        fun pause(downloadObject: DownloadObject) {
            pause(downloadObject.getDid())
        }

        fun pauseAll() {
            fetch?.getDownloadsWithStatus(Status.DOWNLOADING, Func { it ->
                val list = mutableListOf<Int>()
                it.forEach {
                    list.add(it.id)
                }
                fetch?.pause(list)
            })
        }

        fun pause(did: Int) {
            doAsync { fetch!!.pause(did) }
        }

        fun resume(downloadObject: DownloadObject) {
            resume(downloadObject.getDid())
        }

        internal fun resume(did: Int) {
            doAsync { fetch!!.resume(did) }
        }

        private fun updateNotification(downloadObject: DownloadObject, isPaused: Boolean) {
            val notification = NotificationCompat.Builder(context!!, CHANNEL_ONGOING)
                    .setSmallIcon(if (isPaused) R.drawable.ic_pause_not else if (downloadObject.eta!!.toInt() == -2) R.drawable.ic_move else android.R.drawable.stat_sys_download)
                    .setContentTitle(downloadObject.name)
                    .setContentText(downloadObject.chapter)
                    .setOnlyAlertOnce(!isPaused || downloadObject.eta!!.toInt() == -2)
                    .setProgress(100, downloadObject.progress, downloadObject.state == DownloadObject.PENDING)
                    .setOngoing(!isPaused)
                    .setSound(null)
                    .setWhen(downloadObject.time)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
            if (downloadObject.eta!!.toInt() != -2) {
                if (isPaused)
                    notification.addAction(R.drawable.ic_play_not, "Reanudar", getPending(downloadObject, ACTION_RESUME))
                else
                    notification.addAction(R.drawable.ic_pause_not, "Pausar", getPending(downloadObject, ACTION_PAUSE))
                notification.addAction(R.drawable.ic_delete, "Cancelar", getPending(downloadObject, ACTION_CANCEL))
            }
            if (!isPaused)
                notification.setSubText(downloadObject.subtext)
            notificationManager!!.notify(downloadObject.eid!!.toInt(), notification.build())
        }

        private fun completedNotification(downloadObject: DownloadObject) {
            val notification = NotificationCompat.Builder(context!!, CHANNEL)
                    .setColor(ContextCompat.getColor(context!!, android.R.color.holo_green_dark))
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(downloadObject.name)
                    .setContentText(downloadObject.chapter)
                    .setContentIntent(ServersFactory.getPlayIntent(context!!, downloadObject.name!!, downloadObject.file!!))
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setWhen(downloadObject.time)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            notificationManager!!.notify(downloadObject.eid!!.toInt(), notification)
            updateMedia(downloadObject)
        }

        private fun errorNotification(downloadObject: DownloadObject) {
            val notification = NotificationCompat.Builder(context!!, CHANNEL)
                    .setColor(ContextCompat.getColor(context!!, android.R.color.holo_red_dark))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(downloadObject.name)
                    .setContentText("Error al descargar " + downloadObject.chapter!!.toLowerCase())
                    .setOngoing(false)
                    .setWhen(downloadObject.time)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            notificationManager!!.notify(downloadObject.eid!!.toInt(), notification)
        }

        private fun foregroundNotification(): Notification {
            val builder = NotificationCompat.Builder(context!!, CHANNEL_FOREGROUND)
                    .setSmallIcon(R.drawable.ic_service)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
            if (PrefsUtil.collapseDirectoryNotification)
                builder.setSubText("Descargas en progreso")
            else
                builder.setContentTitle("Descargas en progreso")
            return builder.build()
        }

        private fun updateMedia(downloadObject: DownloadObject) {
            try {
                val file = downloadObject.file
                context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(FileAccessHelper.INSTANCE.getFile(file!!))))
                MediaScannerConnection.scanFile(context, arrayOf(FileAccessHelper.INSTANCE.getFile(file).absolutePath), arrayOf("video/mp4"), null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        private fun getPending(downloadObject: DownloadObject, action: Int): PendingIntent {
            val intent = Intent(context, DownloadReceiver::class.java)
                    .putExtra("did", downloadObject.getDid())
                    .putExtra("eid", downloadObject.eid)
                    .putExtra("action", action)
            return PendingIntent.getBroadcast(context, downloadObject.key + action, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        private fun stopIfNeeded() {
            if (downloadDao.countActive() == 0)
                ContextCompat.startForegroundService(context!!, Intent(context, DownloadManager::class.java).setAction("stop.foregrouns"))
        }
    }
}
