package knf.kuma.queue

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.preference.PreferenceManager
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.RecordObject
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster


object QueueManager {
    fun isInQueue(eid: String): Boolean {
        return CacheDB.INSTANCE.queueDAO().isInQueue(eid)
    }

    fun add(uri: Uri, isFile: Boolean, chapter: AnimeObject.WebInfo.AnimeChapter?) {
        if (chapter == null) return
        CacheDB.INSTANCE.queueDAO().add(QueueObject(uri, isFile, chapter))
        Toaster.toast("Episodio añadido a cola")
    }

    fun remove(queueObject: QueueObject) {
        CacheDB.INSTANCE.queueDAO().remove(queueObject)
    }

    fun update(vararg objects: QueueObject) {
        CacheDB.INSTANCE.queueDAO().update(*objects)
    }

    fun remove(list: MutableList<QueueObject>) {
        CacheDB.INSTANCE.queueDAO().remove(list)
    }

    fun remove(eid: String?) {
        if (eid == null) return
        CacheDB.INSTANCE.queueDAO().removeByEID(eid)
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
                                Uri.fromFile(FileAccessHelper.INSTANCE.getFile(file.fileName)),
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
        val startUri = if (list[0].isFile) FileAccessHelper.INSTANCE.getDataUri(list[0].chapter.fileName) else list[0].getUri()
        val titles = QueueObject.getTitles(list)
        val uris = QueueObject.getUris(list)
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
        val startUri = FileAccessHelper.INSTANCE.getDataUri(list[0].fileName)
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
        doAsync {
            for (queueObject in list) {
                CacheDB.INSTANCE.chaptersDAO().addChapter(queueObject.chapter)
            }
            if (list.isNotEmpty())
                CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(list[list.size - 1].chapter))
        }
    }

    private fun markAllSeenDownloaded(list: List<ExplorerObject.FileDownObj>) {
        doAsync {
            for (file in list) {
                CacheDB.INSTANCE.chaptersDAO().addChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(file))
            }
            if (list.isNotEmpty())
                CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(list[list.size - 1])))
        }
    }
}
