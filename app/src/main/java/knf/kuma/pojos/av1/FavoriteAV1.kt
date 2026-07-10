package knf.kuma.pojos.av1

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import knf.kuma.database.BaseConverter
import kotlinx.coroutines.flow.Flow

open class FavoriteBase

@Keep
@Entity
data class FavoriteAV1(
    @PrimaryKey
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("category")
    var category: String
): FavoriteBase() {

    constructor(): this(0, "", "", "", "")

    fun animeUrl() = "https://animeav1.com/media/$slug"
    fun imageUrl() = "https://cdn.animeav1.com/covers/$aid.jpg"
    fun typeText() = when (type) {
        "tv-anime" -> "Anime"
        "pelicula" -> "Película"
        "ova" -> "OVA"
        else -> "Especial"
    }

    fun isValid(): Boolean = aid != 0

    companion object {
        const val CATEGORY_NONE = "_NONE_"
    }
}

data class FavoriteSectionAV1(val name: String): FavoriteBase()

@Dao
@TypeConverters(BaseConverter::class)
abstract class FavoriteAV1Dao {
    @get:Query("SELECT * FROM Favoriteav1 ORDER BY name")
    abstract val all: Flow<List<FavoriteAV1>>

    @get:Query("SELECT * FROM Favoriteav1 ORDER BY name")
    abstract val allRaw: List<FavoriteAV1>

    @get:Query("SELECT aid FROM FavoriteAV1")
    abstract val allAids: List<Int>

    @get:Query("SELECT category FROM FavoriteAV1 GROUP BY category ORDER BY category")
    abstract val categories: List<String>

    @get:Query("SELECT * FROM FavoriteAV1 ORDER BY aid ASC")
    abstract val allID: Flow<List<FavoriteAV1>>

    @get:Query("SELECT * FROM FavoriteAV1 ORDER BY category")
    abstract val byCategory: List<FavoriteAV1>

    @get:Query("SELECT * FROM FavoriteAV1 ORDER BY CASE WHEN category = '_NONE_' THEN 1 ELSE 0 END, category ASC")
    abstract val byCategoryFlow: Flow<List<FavoriteAV1>>

    @get:Query("SELECT count(*) FROM FavoriteAV1")
    abstract val count: Int

    @get:Query("SELECT count(*) FROM FavoriteAV1")
    abstract val countFlow: Flow<Int>

    @get:Query("SELECT count(*) FROM FavoriteAV1")
    abstract val countLive: LiveData<Int>

    @Query("UPDATE FavoriteAV1 SET category = :newName WHERE category = :oldName")
    abstract fun renameCategory(oldName: String, newName: String)

    @Query("SELECT * FROM FavoriteAV1 WHERE category != :category ORDER BY name")
    abstract fun getNotInCategory(category: String): List<FavoriteAV1>

    @Query("SELECT * FROM FavoriteAV1 WHERE category = :category ORDER BY name")
    abstract fun getAllInCategory(category: String): List<FavoriteAV1>

    @Query("SELECT count(*) FROM FavoriteAV1 WHERE aid = :aid")
    abstract fun isFav(aid: Int): Boolean

    @Query("SELECT count(*) FROM FavoriteAV1 WHERE aid = :aid")
    abstract suspend fun isFavSuspend(aid: Int): Boolean

    @Query("SELECT count(*) FROM FavoriteAV1 WHERE name = :name")
    abstract fun isFavName(name: String): Boolean

    @Query("SELECT count(*) FROM FavoriteAV1 WHERE aid = :aid")
    abstract fun isFavFlow(aid: Int): Flow<Boolean>

    @Query("SELECT * FROM FavoriteAV1 WHERE aid = :aid")
    abstract fun favObserver(aid: Int): Flow<FavoriteAV1?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addFav(favorite: FavoriteAV1)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(list: List<FavoriteAV1>)

    @Delete
    abstract fun delete(favorite: FavoriteAV1)

    @Query("DELETE FROM favoriteobject")
    abstract fun clear()
}