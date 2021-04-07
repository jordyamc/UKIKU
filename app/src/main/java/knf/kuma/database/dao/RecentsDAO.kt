package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import knf.kuma.pojos.RecentObject
import knf.kuma.recents.RecentModelCh

@Dao
interface RecentsDAO {
    @get:Query("SELECT * FROM recentobject ORDER BY `key`")
    val objects: LiveData<MutableList<RecentObject>>

    @get:Query("SELECT * FROM recentobject ORDER BY `key`")
    val all: MutableList<RecentObject>

    @get:Query("SELECT name, chapter, url, aid, eid FROM recentobject ORDER BY `key`")
    val allSimple: MutableList<RecentModelCh>

    @Query("DELETE FROM recentobject")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCache(objects: MutableList<RecentObject>)
}
