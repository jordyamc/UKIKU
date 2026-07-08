package knf.kuma.pojos.av1

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import knf.kuma.database.BaseConverter
import kotlinx.coroutines.flow.Flow

@Keep
@Entity
@TypeConverters(BaseConverter::class)
data class GenreRecord(
    @PrimaryKey
    val slug: String,
    val name: String,
    var count: Int,
    var isBlocked: Boolean = false
) {
    constructor(): this("", "", 0, false)

    fun add(number: Int) {
        count += number
    }
    fun sub(number: Int) {
        count -= number
        if (count < 0) count = 0
    }
}

@Dao
interface GenreRecordDao {
    @get:Query("SELECT * FROM GenreRecord WHERE count > 0 AND isBlocked = 0 ORDER BY count DESC LIMIT 3")
    val top: List<GenreRecord>

    @get:Query("SELECT * FROM GenreRecord WHERE count > 0 ORDER BY count DESC")
    val ranking: MutableList<GenreRecord>

    @get:Query("SELECT * FROM GenreRecord WHERE isBlocked = 1")
    val blocked: List<GenreRecord>

    @get:Query("SELECT * FROM GenreRecord")
    val allFlow: Flow<List<GenreRecord>>

    @get:Query("SELECT * FROM GenreRecord")
    val all: List<GenreRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GenreRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<GenreRecord>)

    @Query("SELECT * FROM GenreRecord WHERE slug = :slug")
    suspend fun findBySlug(slug: String): GenreRecord?

    @Query("DELETE FROM GenreRecord WHERE count >= 0")
    fun reset()

}
