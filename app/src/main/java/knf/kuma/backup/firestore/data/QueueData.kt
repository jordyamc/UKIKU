package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject

@Keep
data class QueueData(val list: List<QueueObject> = emptyList()) {
    companion object {
        fun create(): QueueData = QueueData(CacheDB.INSTANCE.queueDAO().allRaw)
    }
}