package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.Record

@Keep
data class HistoryData(val list: List<Record> = emptyList()) {
    companion object {
        fun create(): HistoryData = HistoryData(CacheDB.INSTANCE.recordAV1DAO().all)
        fun createBatched(): List<HistoryData> = CacheDB.INSTANCE.recordAV1DAO().all.chunked(500).map {
            HistoryData(it)
        }
    }
}