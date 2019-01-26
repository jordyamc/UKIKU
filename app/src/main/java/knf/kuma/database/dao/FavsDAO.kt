package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.FavoriteObject

@Dao
@TypeConverters(BaseConverter::class)
interface FavsDAO {
    @get:Query("SELECT * FROM favoriteobject ORDER BY name")
    val all: LiveData<MutableList<FavoriteObject>>

    @get:Query("SELECT * FROM favoriteobject ORDER BY name")
    val allRaw: MutableList<FavoriteObject>

    @get:Query("SELECT * FROM favoriteobject GROUP BY category ORDER BY category")
    val catagories: MutableList<FavoriteObject>

    @get:Query("SELECT * FROM favoriteobject ORDER BY aid + 0 ASC")
    val allID: LiveData<MutableList<FavoriteObject>>

    @get:Query("SELECT * FROM favoriteobject ORDER BY category")
    val byCategory: MutableList<FavoriteObject>

    @get:Query("SELECT count(*) FROM favoriteobject")
    val count: Int

    @get:Query("SELECT count(*) FROM favoriteobject")
    val countLive: LiveData<Int>

    @Query("SELECT * FROM favoriteobject WHERE category NOT LIKE :category ORDER BY name")
    fun getNotInCategory(category: String): MutableList<FavoriteObject>

    @Query("SELECT * FROM favoriteobject WHERE category LIKE :category ORDER BY name")
    fun getAllInCategory(category: String): MutableList<FavoriteObject>

    @Query("SELECT count(*) FROM favoriteobject WHERE `key` LIKE :key")
    fun isFav(key: Int): Boolean

    @Query("SELECT * FROM favoriteobject WHERE `key` LIKE :key LIMIT 1")
    fun favObserver(key: Int): LiveData<FavoriteObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFav(favoriteObject: FavoriteObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(list: List<FavoriteObject>)

    @Delete
    fun deleteFav(favoriteObject: FavoriteObject)

    @Query("DELETE FROM favoriteobject")
    fun clear()
}
