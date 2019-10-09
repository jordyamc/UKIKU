package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.QueueObject

@Dao
@TypeConverters(BaseConverter::class)
interface QueueDAO {

    @get:Query("SELECT MIN(aid) AS id,`key`,aid,name,number,eid,isFile,uri,time,link FROM queueobject ORDER BY name")
    val allAlone: LiveData<MutableList<QueueObject>>

    @get:Query("SELECT * FROM queueobject ORDER BY name")
    val all: LiveData<MutableList<QueueObject>>

    @get:Query("SELECT * FROM queueobject ORDER BY name")
    val allRaw: MutableList<QueueObject>

    @get:Query("SELECT * FROM queueobject ORDER BY time ASC")
    val allAsort: LiveData<MutableList<QueueObject>>

    @get:Query("SELECT count(*) FROM queueobject")
    val countLive: LiveData<Int>

    @Query("SELECT count(*) FROM queueobject WHERE eid = :eid")
    fun isInQueue(eid: String): Boolean

    @Query("SELECT count(*) FROM queueobject WHERE eid = :eid")
    fun isInQueueLive(eid: String): LiveData<Boolean>

    @Query("SELECT count(*) FROM queueobject WHERE aid LIKE :aid")
    fun countAlone(aid: String): Int

    @Query("SELECT * FROM queueobject WHERE aid = :aid ORDER BY eid+0 ASC")
    fun getByAid(aid: String): LiveData<MutableList<QueueObject>>

    @Query("SELECT * FROM queueobject WHERE aid = :aid ORDER BY eid+0 ASC")
    fun getAllByAid(aid: String): MutableList<QueueObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(queueObject: QueueObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(queueObjects: List<QueueObject>)

    @Update
    fun update(vararg objects: QueueObject)

    @Delete
    fun remove(queueObject: QueueObject)

    @Delete
    fun remove(list: MutableList<QueueObject>)

    @Query("DELETE FROM queueobject WHERE aid LIKE :aid")
    fun removeByID(aid: String)

    @Query("DELETE FROM queueobject WHERE eid LIKE :eid")
    fun removeByEID(eid: String)

    @Query("DELETE FROM queueobject")
    fun nuke()
}
