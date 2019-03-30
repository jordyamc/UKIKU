package knf.kuma.download

import android.content.Context
import android.os.StatFs
import android.text.format.Formatter
import android.view.View
import knf.kuma.commons.toast
import knf.kuma.pojos.AnimeObject
import knf.kuma.videoservers.ServersFactory

object MultipleDownloadManager {
    private const val CHAPTER_SIZE = 160000000L
    private var index = 0
    private var chaptersList: List<AnimeObject.WebInfo.AnimeChapter> = listOf()

    fun startDownload(context: Context, view: View, list: List<AnimeObject.WebInfo.AnimeChapter>, addQueue: Boolean) {
        if (list.isEmpty()) return
        if (!addQueue && !isSpaceAvailable(list.size)) {
            "Se requieren mínimo ${minSpaceString(context, list.size)} libres!".toast()
            return
        }
        clear(list)
        downloadNext(context, view, addQueue)
    }

    private fun downloadNext(context: Context, view: View, addQueue: Boolean) {
        if (index >= chaptersList.size) return
        val current = chaptersList[index]
        ServersFactory.start(context, current.link, current, addQueue, addQueue, object : ServersFactory.ServersInterface {
            override fun onFinish(started: Boolean, success: Boolean) {
                index++
                downloadNext(context, view, addQueue)
            }

            override fun onCast(url: String?) {
            }

            override fun onProgressIndicator(boolean: Boolean) {
            }

            override fun getView(): View? {
                return view
            }
        })
    }

    private fun clear(list: List<AnimeObject.WebInfo.AnimeChapter>) {
        index = 0
        chaptersList = list
    }

    private fun minSpaceString(context: Context, size: Int): String {
        return Formatter.formatFileSize(context, size * CHAPTER_SIZE)
    }

    fun isSpaceAvailable(size: Int): Boolean {
        return try {
            getAvailable() > size * CHAPTER_SIZE
        } catch (e: Exception) {
            true
        }
    }

    private fun getAvailable(): Long {
        val stat = StatFs(FileAccessHelper.INSTANCE.rootFile.path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }
}