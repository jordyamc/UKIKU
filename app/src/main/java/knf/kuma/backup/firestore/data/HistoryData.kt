package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecordObject

@Keep
data class HistoryData(val list: List<RecordObject> = emptyList()) {
    companion object {
        fun create(): HistoryData = HistoryData(CacheDB.INSTANCE.recordsDAO().allRaw)
    }
}