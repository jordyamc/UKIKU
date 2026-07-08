package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeenObject
import knf.kuma.pojos.av1.Record

@Keep
data class SeenData(val list: List<Record> = emptyList()) {
    companion object {
        fun create(): SeenData = SeenData(CacheDB.INSTANCE.recordAV1DAO().all)
    }
}