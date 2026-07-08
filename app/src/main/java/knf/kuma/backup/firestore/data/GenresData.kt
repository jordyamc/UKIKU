package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.GenreRecord

@Keep
data class GenresData(val list: List<GenreRecord> = emptyList()) {
    companion object {
        fun create(): GenresData = GenresData(CacheDB.INSTANCE.genreRecordDAO().all)
    }
}