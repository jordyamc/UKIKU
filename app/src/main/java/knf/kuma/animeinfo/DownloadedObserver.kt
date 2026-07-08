package knf.kuma.animeinfo

import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.FileWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object DownloadedObserver {
    private var observer: Job = Job()

    fun observe(scope: CoroutineScope, size: Int, fileWrapper: FileWrapper<*>) {
        observer.cancel()
        observer = scope.launch(Dispatchers.IO) {
            if (AchievementManager.isUnlocked(35) || size == fileWrapper.parentSize()) {
                unlock()
                return@launch
            }
            while (isActive) {
                if (size == fileWrapper.parentSize()) {
                    unlock()
                    return@launch
                }
                delay(1500)
            }
        }

    }

    private fun unlock() {
        AchievementManager.unlock(listOf(35))
    }
}