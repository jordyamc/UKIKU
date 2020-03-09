package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.pojos.ExplorerObject

@Dao
interface ExplorerDAO {
    @get:Query("SELECT * FROM explorerobject ORDER BY name")
    val all: LiveData<MutableList<ExplorerObject>>

    @Query("SELECT * FROM explorerobject WHERE fileName LIKE :file")
    fun getItem(file: String): LiveData<ExplorerObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<ExplorerObject>)

    @Update
    fun update(explorerObject: ExplorerObject)

    @Delete
    fun delete(explorerObject: ExplorerObject)

    @Query("DELETE FROM explorerobject")
    fun deleteAll()
}
