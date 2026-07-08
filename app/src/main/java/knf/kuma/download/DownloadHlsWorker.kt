package knf.kuma.download

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExoPlayerAssetLoader
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import knf.kuma.App
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.FileUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.DownloadObject
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@UnstableApi
class DownloadHlsWorker(
    val appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_ID = "eid"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL = "service.Downloads"
        private const val CHANNEL_ONGOING = "service.Downloads.Ongoing"
        private const val POLL_INTERVAL_MS = 500L

        fun start(context: Context, eid: String) {
            val request = OneTimeWorkRequestBuilder<DownloadHlsWorker>()
                .setInputData(workDataOf(
                    KEY_ID to eid
                ))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    val notificationManager: NotificationManager by lazy { appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    @Volatile private var transformer: Transformer? = null

    var isUserCancelled = false

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val download = CacheDB.INSTANCE.downloadsDAO().getByEid(id)?: return Result.failure()
        val outputFileName = "hls_download_${System.currentTimeMillis()}.mp4"

        setForeground(createForegroundInfo(download))
        val outputFile = File(applicationContext.getExternalFilesDir(null), outputFileName)
        if (outputFile.exists()) outputFile.delete()

        return try {
            runTransform(download, outputFile)
            Result.success()
        } catch (t: Throwable) {
            t.printStackTrace()
            outputFile.delete()
            val current = CacheDB.INSTANCE.downloadsDAO().getByEid(download.eid)
            if (current != null) {
                if (download.state == DownloadObject.CANCELLED) {
                    isUserCancelled = true
                    errorNotification(download)
                }
                CacheDB.INSTANCE.downloadsDAO().delete(download)
            }
            if (isUserCancelled) {
                Result.success()
            } else {
                Result.failure()
            }
        } finally {
            transformer = null
        }
    }
    private suspend fun runTransform(
        download: DownloadObject,
        outputFile: File
    ) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val downloadDao = CacheDB.INSTANCE.downloadsDAO()
                Log.e("HLS", "On download: ${download.link}")
                val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(download.link)).build()

                val transformerBuilder = Transformer.Builder(applicationContext)
                if (download.headers != null) {
                    val headers = download.headers.headers.toMap()
                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(headers)
                        .apply { if (headers.contains("User-Agent")) setUserAgent(headers["User-Agent"]) }
                    val dataSourceFactory = DefaultDataSource.Factory(applicationContext, httpDataSourceFactory)
                    val mediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
                    val assetLoaderFactory = ExoPlayerAssetLoader.Factory(
                        applicationContext,
                        DefaultDecoderFactory.Builder(applicationContext).build(),
                        Clock.DEFAULT,
                        mediaSourceFactory
                    )
                    transformerBuilder.setAssetLoaderFactory(assetLoaderFactory)

                }

                val t = transformerBuilder
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            val downloadObject = downloadDao.getByEid(download.eid)
                            if (downloadObject != null) {
                                Log.e("Download", "Moving temp")
                                downloadObject.setEta(-2)
                                downloadObject.progress = 0
                                downloadDao.update(downloadObject)
                                FileUtil.moveFile(
                                    downloadObject.file,
                                    object : FileUtil.MoveCallback {
                                        override fun onProgress(pair: Pair<Int, Boolean>) {
                                            if (!pair.second) {
                                                downloadObject.progress = pair.first
                                                setForegroundAsync(createForegroundInfo(downloadObject))
                                                downloadDao.update(downloadObject)
                                            } else if (pair.first == -1) {
                                                outputFile.delete()
                                                downloadDao.delete(downloadObject)
                                                errorNotification(downloadObject)
                                                if (cont.isActive) cont.resume(Unit)
                                            } else {
                                                downloadObject.progress = 100
                                                downloadObject.state = DownloadObject.COMPLETED
                                                downloadDao.update(downloadObject)
                                                completedNotification(downloadObject)
                                                if (cont.isActive) cont.resume(Unit)
                                            }
                                        }
                                    })
                            } else {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (cont.isActive) cont.resumeWithException(exportException)
                        }
                    })
                    .build()

                transformer = t
                t.start(editedMediaItem, outputFile.absolutePath)
                startProgressPolling(t, download)
                cont.invokeOnCancellation { t.cancel() }
            }
        }

    }

    private fun startProgressPolling(t: Transformer, download: DownloadObject) {
        val handler = Handler(Looper.getMainLooper())
        val progressHolder = ProgressHolder()
        val dao = CacheDB.INSTANCE.downloadsDAO()
        val runnable = object : Runnable {
            override fun run() {
                if (transformer !== t) return
                val state = t.getProgress(progressHolder)
                runBlocking(Dispatchers.IO) {
                    if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        val current = dao.getByEid(download.eid)
                        if (current == null) {
                            isUserCancelled = true
                            WorkManager.getInstance(appContext).cancelWorkById(id)
                            return@runBlocking
                        }
                        val pct = progressHolder.progress.coerceIn(0, 100)
                        download.progress = pct
                        dao.update(download)
                        setProgressAsync(workDataOf(KEY_PROGRESS to pct))
                        setForegroundAsync(createForegroundInfo(download))
                    }
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(runnable)
    }

    private fun createForegroundInfo(downloadObject: DownloadObject): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ONGOING).apply {
            setSmallIcon(if (downloadObject.eta.toLong() == -2L) R.drawable.ic_move else android.R.drawable.stat_sys_download)
            setContentTitle(downloadObject.name)
            setContentText(downloadObject.chapter)
            setOnlyAlertOnce(true)
            setProgress(100, downloadObject.progress, downloadObject.state == DownloadObject.PENDING)
            color = ContextCompat.getColor(App.context, EAHelper.getThemeColor())
            setGroup("manager")
            setOngoing(true)
            setSound(null)
            setWhen(downloadObject.time)
            priority = NotificationCompat.PRIORITY_LOW
            if (downloadObject.eta.toLong() != -2L) {
                val cancelIntent = Intent(applicationContext, CancelDownloadReceiver::class.java).apply {
                    putExtra(CancelDownloadReceiver.EXTRA_WORK_ID, id.toString())
                    putExtra(CancelDownloadReceiver.EXTRA_EID, downloadObject.eid)
                }
                val cancelPendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    id.hashCode(),
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                addAction(R.drawable.ic_delete, "Cancelar", cancelPendingIntent)
            }
            setSubText(downloadObject.subtext)
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(downloadObject.eid.toInt(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(downloadObject.eid.toInt(), notification)
        }
    }

    private fun completedNotification(downloadObject: DownloadObject) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL)
            .setColor(ContextCompat.getColor(appContext, android.R.color.holo_green_dark))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(downloadObject.name)
            .setContentText(downloadObject.chapter)
            .setContentIntent(ServersFactory.getPlayIntent(appContext, downloadObject.name, downloadObject.file))
            .setOngoing(false)
            .setAutoCancel(true)
            .setWhen(downloadObject.time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(downloadObject.eid?.toInt() ?: 0, notification)
        updateMedia(downloadObject)
    }

    private fun errorNotification(downloadObject: DownloadObject) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL)
            .setColor(ContextCompat.getColor(appContext, android.R.color.holo_red_dark))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(downloadObject.name)
            .setContentText("Error al descargar " + downloadObject.chapter.lowercase(Locale.ENGLISH))
            .setOngoing(false)
            .setWhen(downloadObject.time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(downloadObject.eid?.toInt() ?: 0, notification)
    }

    private fun updateMedia(downloadObject: DownloadObject) {
        try {
            val file = downloadObject.file
            appContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(FileAccessHelper.getFile(file))))
            MediaScannerConnection.scanFile(appContext, arrayOf(FileAccessHelper.getFile(file).absolutePath), arrayOf("video/mp4"), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    class CancelDownloadReceiver: BroadcastReceiver() {
        companion object {
            const val EXTRA_WORK_ID = "work_id"
            const val EXTRA_EID = "download_eid"
        }

        override fun onReceive(context: Context, intent: Intent) {
            val workIdString = intent.getStringExtra(EXTRA_WORK_ID) ?: return
            val eid = intent.getStringExtra(EXTRA_EID) ?: return
            val workId = try {
                UUID.fromString(workIdString)
            } catch (_: IllegalArgumentException) {
                return
            }

            CacheDB.INSTANCE.downloadsDAO().apply {
                val download = getByEid(eid)
                if (download != null) {
                    download.state = DownloadObject.CANCELLED
                    update(download)
                }
            }

            WorkManager.getInstance(context.applicationContext).cancelWorkById(workId)
        }

    }

}