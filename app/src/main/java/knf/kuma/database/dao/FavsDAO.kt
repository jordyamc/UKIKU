package knf.kuma.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.TypeConverters
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.FavoriteObject

@Dao
@TypeConverters(BaseConverter::class)
interface FavsDAO {
    @get:Query("SELECT * FROM favoriteobject ORDER BY name")
    val allRaw: MutableList<FavoriteObject>
    @get:Query("SELECT count(*) FROM favoriteobject")
    val count: Int

    @Query("SELECT count(*) FROM favoriteobject WHERE `key` = :key")
    fun isFav(key: Int): Boolean

    @Delete
    fun deleteFav(favoriteObject: FavoriteObject)
}
