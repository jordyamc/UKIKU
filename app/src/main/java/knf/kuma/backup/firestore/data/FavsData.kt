package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.FavoriteAV1

@Keep
data class FavsData(val list: List<FavoriteAV1> = emptyList()) {
    companion object {
        fun create(): FavsData = FavsData(CacheDB.INSTANCE.favoriteAV1DAO().allRaw)
    }
}