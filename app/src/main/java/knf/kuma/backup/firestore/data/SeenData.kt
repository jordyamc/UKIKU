package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeenObject

@Keep
data class SeenData(val list: List<SeenObject> = emptyList()) {
    companion object {
        fun create(): SeenData = SeenData(CacheDB.INSTANCE.seenDAO().all)
    }
}