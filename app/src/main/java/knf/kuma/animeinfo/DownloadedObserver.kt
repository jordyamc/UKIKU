package knf.kuma.animeinfo

import android.os.FileObserver
import knf.kuma.achievements.AchievementManager
import knf.kuma.download.FileAccessHelper
import java.io.File

object DownloadedObserver {
    private var observer: FileObserver? = null
    private var directory = File("")

    fun observe(size: Int, fileName: String) {
        observer?.stopWatching()
        directory = FileAccessHelper.INSTANCE.getDownloadsDirectoryFromFile(fileName)
        if (size == directory.list().size || AchievementManager.isUnlocked(35)) {
            unlock()
            return
        }
        observer = object : FileObserver(directory.absolutePath) {
            override fun onEvent(event: Int, path: String?) {
                if (size == directory.list().size)
                    unlock()
            }
        }
        observer?.startWatching()
    }

    private fun unlock() {
        AchievementManager.unlock(listOf(35))
    }
}