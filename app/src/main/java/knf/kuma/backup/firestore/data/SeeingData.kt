package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject

@Keep
data class SeeingData(val list: List<SeeingObject> = emptyList()) {
    companion object {
        fun create(): SeeingData = SeeingData(CacheDB.INSTANCE.seeingDAO().allRaw)
    }
}