package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.SeeingObject

@Dao
@TypeConverters(BaseConverter::class)
interface SeeingDAO {
    @get:Query("SELECT * FROM seeingobject ORDER BY title")
    val allRaw: MutableList<SeeingObject>
    @Query("SELECT * FROM seeingobject WHERE aid=:aid")
    fun findByAid(aid: String): SeeingObject?
    @get:Query("SELECT count(*) FROM seeingobject")
    val countAll: Int
    @Delete
    fun remove(seeingObject: SeeingObject)
}
