package knf.kuma.queue

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.preference.PreferenceManager
import knf.kuma.achievements.AchievementManager
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster


object QueueManager {
    fun isInQueue(eid: String): Boolean {
        return CacheDB.INSTANCE.queueDAO().isInQueue(eid)
    }

    fun add(uri: Uri, isFile: Boolean, chapter: AnimeObject.WebInfo.AnimeChapter?) {
        if (chapter == null) return
        CacheDB.INSTANCE.queueDAO().add(QueueObject(uri, isFile, chapter))
        syncData { queue() }
        Toaster.toast("Episodio añadido a cola")
    }

    fun remove(queueObject: QueueObject) {
        CacheDB.INSTANCE.queueDAO().remove(queueObject)
        syncData { queue() }
    }

    fun update(vararg objects: QueueObject) {
        CacheDB.INSTANCE.queueDAO().update(*objects)
        syncData { queue() }
    }

    fun remove(list: MutableList<QueueObject>) {
        CacheDB.INSTANCE.queueDAO().remove(list)
        syncData { queue() }
    }

    fun remove(eid: String?) {
        if (eid == null) return
        CacheDB.INSTANCE.queueDAO().removeByEID(eid)
        syncData { queue() }
    }

    fun removeAll(aid: String) {
        CacheDB.INSTANCE.queueDAO().removeByID(aid)
    }

    internal fun startQueue(context: Context, list: List<QueueObject>) {
        if (list.isNotEmpty()) {
            AchievementManager.onPlayQueue(list.size)
            markAllSeen(list)
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0" || isMxInstalled(context) == null)
                startQueueInternal(context, list)
            else
                startQueueExternal(context, list)
        } else
            Toaster.toast("La lista esta vacía")
    }

    internal fun startQueueDownloaded(context: Context?, list: List<ExplorerObject.FileDownObj>) {
        if (context == null) return
        if (list.isNotEmpty()) {
            markAllSeenDownloaded(list)
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0") == "0" || isMxInstalled(context) == null)
                startQueueInternalDownloaded(context, list)
            else
                startQueueExternalDownloaded(context, list)
        } else
            Toaster.toast("La lista esta vacía")
    }

    private fun startQueueInternal(context: Context, list: List<QueueObject>) {
        val intent = PrefsUtil.getPlayerIntent()
                .putExtra("isPlayList", true)
                .putExtra("playlist", list[0].chapter.aid)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun startQueueInternalDownloaded(context: Context, list: List<ExplorerObject.FileDownObj>) {
        doAsync {
            for (file in list)
                CacheDB.INSTANCE.queueDAO().add(
                        QueueObject(
                                Uri.fromFile(FileAccessHelper.getFile(file.fileName)),
                                true,
                                AnimeObject.WebInfo.AnimeChapter.fromDownloaded(file)))
            val intent = PrefsUtil.getPlayerIntent()
                    .putExtra("isPlayList", true)
                    .putExtra("playlist", list[0].aid)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun startQueueExternal(context: Context, list: List<QueueObject>) {
        val startUri = if (list[0].isFile) FileAccessHelper.getDataUri(list[0].chapter.fileName) else list[0].createUri()
        val titles = QueueObject.getTitles(list)
        val uris = QueueObject.uris(list)
        uris[0] = startUri ?: Uri.EMPTY
        val intent = Intent(Intent.ACTION_VIEW)
                .setPackage(isMxInstalled(context))
                .setDataAndType(startUri, "video/mp4")
                .putExtra("title", titles[0])
                .putExtra("video_list_is_explicit", true)
                .putExtra("video_list", uris)
                .putExtra("video_list.name", titles)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun startQueueExternalDownloaded(context: Context, list: List<ExplorerObject.FileDownObj>) {
        val startUri = FileAccessHelper.getDataUri(list[0].fileName)
        val titles = ExplorerObject.FileDownObj.getTitles(list)
        val uris = ExplorerObject.FileDownObj.getUris(list)
        uris[0] = startUri ?: Uri.EMPTY
        val intent = Intent(Intent.ACTION_VIEW)
                .setPackage(isMxInstalled(context))
                .setDataAndType(startUri, "video/mp4")
                .putExtra("title", titles[0])
                .putExtra("video_list_is_explicit", true)
                .putExtra("video_list", uris)
                .putExtra("video_list.name", titles)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isMxInstalled(context: Context): String? {
        val pm = context.packageManager
        try {
            pm.getPackageInfo("com.mxtech.videoplayer.pro", PackageManager.GET_ACTIVITIES)
            return "com.mxtech.videoplayer.pro"
        } catch (e: PackageManager.NameNotFoundException) {
        }
        try {
            pm.getPackageInfo("com.mxtech.videoplayer.ad", PackageManager.GET_ACTIVITIES)
            return "com.mxtech.videoplayer.ad"
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return null
    }

    private fun markAllSeen(list: List<QueueObject>) {
        if (list.isNotEmpty())
            doAsync {
                CacheDB.INSTANCE.seenDAO().addAll(list.map { SeenObject.fromChapter(it.chapter) })
                CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(list.last().chapter))
                syncData {
                    history()
                    seen()
                }
            }
    }

    private fun markAllSeenDownloaded(list: List<ExplorerObject.FileDownObj>) {
        if (list.isNotEmpty())
            doAsync {
                CacheDB.INSTANCE.seenDAO().addAll(list.map { SeenObject.fromDownloaded(it) })
                CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(list.last())))
                syncData {
                    history()
                    seen()
                }
            }
    }
}
