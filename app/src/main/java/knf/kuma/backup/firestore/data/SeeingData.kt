package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.Organizer

@Keep
data class SeeingData(val list: List<Organizer> = emptyList()) {
    companion object {
        fun create(): SeeingData = SeeingData(CacheDB.INSTANCE.organizerDAO().allRaw)
    }
}