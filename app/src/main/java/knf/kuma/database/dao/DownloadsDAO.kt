package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import knf.kuma.pojos.DownloadObject

@Dao
interface DownloadsDAO {
    @get:Query("SELECT * FROM downloadobject")
    val all: DataSource.Factory<Int, DownloadObject>

    @get:Query("SELECT * FROM downloadobject WHERE state<=0")
    val active: LiveData<List<DownloadObject>>

    @Query("SELECT * FROM downloadobject WHERE eid LIKE :eid")
    fun getByEid(eid: String): DownloadObject?

    @Query("SELECT * FROM downloadobject WHERE did = :did")
    fun getByDid(did: Int): DownloadObject?

    @Query("SELECT * FROM downloadobject WHERE file LIKE :name")
    fun getByFile(name: String): DownloadObject?

    @Query("SELECT * FROM downloadobject WHERE eid = :eid")
    fun getLiveByEid(eid: String): LiveData<DownloadObject>

    @Query("SELECT * FROM downloadobject WHERE `key` LIKE :key")
    fun getLiveByKey(key: Int): LiveData<DownloadObject>

    @Query("SELECT count(*) FROM downloadobject WHERE state=-1")
    fun countPending(): Int

    @Query("SELECT count(*) FROM downloadobject WHERE state=-1 OR state=0")
    fun countActive(): Int

    @Query("DELETE FROM downloadobject WHERE eid LIKE :eid")
    fun deleteByEid(eid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(downloadObject: DownloadObject)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(downloadObject: DownloadObject)

    @Delete
    fun delete(downloadObject: DownloadObject)
}
