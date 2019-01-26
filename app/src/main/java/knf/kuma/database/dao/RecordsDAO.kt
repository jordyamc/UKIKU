package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.pojos.RecordObject

@Dao
interface RecordsDAO {
    @get:Query("SELECT * FROM recordobject ORDER BY date DESC")
    val all: LiveData<List<RecordObject>>

    @get:Query("SELECT * FROM recordobject ORDER BY date DESC")
    val allRaw: MutableList<RecordObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(recordObject: RecordObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(list: List<RecordObject>)

    @Delete
    fun delete(recordObject: RecordObject)

    @Query("DELETE FROM recordobject")
    fun clear()
}
