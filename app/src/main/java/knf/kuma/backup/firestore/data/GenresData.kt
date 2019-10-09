package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.GenreStatusObject

@Keep
data class GenresData(val list: List<GenreStatusObject> = emptyList()) {
    companion object {
        fun create(): GenresData = GenresData(CacheDB.INSTANCE.genresDAO().all)
    }
}