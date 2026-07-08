package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.SeenObject

@Dao
@TypeConverters(BaseConverter::class)
interface SeenDAO {
    @get:Query("SELECT * FROM seenobject")
    val all: MutableList<SeenObject>

    @get:Query("SELECT aid FROM seenobject GROUP BY aid")
    val allAid: List<String>
    @get:Query("SELECT count(*) FROM seenobject")
    val count: Int
    @Query("SELECT count(*) FROM seenobject WHERE aid = :aid AND number = :number")
    fun chapterIsSeen(aid: String, number: String): Boolean
    @Query("SELECT * FROM seenobject WHERE aid = :aid")
    fun getAllByAid(aid: String): List<SeenObject>

    @Query("DELETE FROM seenobject WHERE aid = :aid")
    fun deleteAllAid(aid: String): Int
    @Delete
    fun delete(seen: SeenObject)
}