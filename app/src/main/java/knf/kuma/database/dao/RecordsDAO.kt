package knf.kuma.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import knf.kuma.pojos.RecordObject

@Dao
interface RecordsDAO {
    @get:Query("SELECT count(*) FROM recordobject")
    val count: Int
    @get:Query("SELECT * FROM recordobject ORDER BY date DESC")
    val allRaw: MutableList<RecordObject>

    @get:Query("SELECT aid FROM recordobject GROUP BY aid")
    val allAid: List<String>

    @Query("SELECT * FROM recordobject WHERE aid=:aid")
    fun findByAid(aid: String): List<RecordObject>

    @Query("DELETE FROM recordobject WHERE aid = :aid")
    fun deleteAllAid(aid: String): Int

    @Delete
    fun delete(recordObject: RecordObject)
}
