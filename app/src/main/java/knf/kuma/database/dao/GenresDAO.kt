package knf.kuma.database.dao

import androidx.room.*
import knf.kuma.pojos.GenreStatusObject

/**
 * Created by jordy on 26/03/2018.
 */
@Dao
interface GenresDAO {
    @get:Query("SELECT * FROM genrestatusobject WHERE count > 0 ORDER BY count DESC LIMIT 3")
    val top: MutableList<GenreStatusObject>

    @get:Query("SELECT * FROM genrestatusobject WHERE count < 0 ORDER BY name DESC")
    val blacklist: MutableList<GenreStatusObject>

    @get:Query("SELECT * FROM genrestatusobject ORDER BY name")
    val all: MutableList<GenreStatusObject>

    @get:Query("SELECT * FROM genrestatusobject WHERE count > 0 ORDER BY count DESC")
    val ranking: MutableList<GenreStatusObject>

    @Query("SELECT * FROM genrestatusobject WHERE name LIKE :name")
    fun getStatus(name: String): GenreStatusObject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStatus(statusObject: GenreStatusObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStatus(statusObjects: List<GenreStatusObject>)

    @Update
    fun update(list: List<GenreStatusObject>)

    @Query("DELETE FROM genrestatusobject WHERE count >= 0")
    fun reset()
}
