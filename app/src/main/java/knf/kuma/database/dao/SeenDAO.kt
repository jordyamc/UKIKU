package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.SeenObject

@Dao
@TypeConverters(BaseConverter::class)
interface SeenDAO {
    @get:Query("SELECT * FROM seenobject")
    val all: MutableList<SeenObject>

    @get:Query("SELECT count(*) FROM seenobject")
    val countLive: LiveData<Int>

    @get:Query("SELECT count(*) FROM seenobject")
    val count: Int

    @Query("SELECT * FROM seenobject WHERE eid = :eid LIMIT 1")
    fun chapterSeen(eid: String): LiveData<SeenObject>

    @Query("SELECT count(*) FROM seenobject WHERE eid = :eid")
    fun chapterIsSeen(eid: String): Boolean

    @Query("SELECT * FROM seenobject WHERE eid IN (:eids) ORDER BY eid+0 DESC LIMIT 1")
    fun getLast(eids: MutableList<String>): SeenObject?

    @Query("SELECT * FROM seenobject WHERE aid = :aid ORDER BY eid+0 DESC LIMIT 1")
    fun getLastByAid(aid: String): SeenObject?

    @Query("SELECT * FROM seenobject WHERE eid IN (:eids)")
    fun getAllFrom(eids: MutableList<String>): List<SeenObject>

    @Query("SELECT * FROM seenobject WHERE aid = :aid")
    fun getAllByAid(aid: String): List<SeenObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addChapter(chapter: SeenObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(list: List<SeenObject>)

    @Delete
    fun deleteChapter(chapter: SeenObject)

    @Query("DELETE FROM seenobject")
    fun clear()
}