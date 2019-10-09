package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.FavoriteObject

@Keep
data class FavsData(val list: List<FavoriteObject> = emptyList()) {
    companion object {
        fun create(): FavsData = FavsData(CacheDB.INSTANCE.favsDAO().allRaw)
    }
}