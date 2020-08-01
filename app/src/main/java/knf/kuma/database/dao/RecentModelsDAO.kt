package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import knf.kuma.recents.RecentModel

@Dao
interface RecentModelsDAO {
    @get:Query("SELECT * FROM recentmodel ORDER BY `key`")
    val allLive: LiveData<List<RecentModel>>

    @get:Query("SELECT * FROM recentmodel ORDER BY `key`")
    val all: List<RecentModel>

    @Query("DELETE FROM recentmodel")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCache(objects: List<RecentModel>)
}
