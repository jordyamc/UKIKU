package knf.kuma.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.EAObject

@Dao
@TypeConverters(BaseConverter::class)
interface EaDAO {
    @get:Query("SELECT * FROM eaobject")
    val all: List<EAObject>

    @Query("SELECT count(*) FROM eaobject WHERE code=:code")
    fun isUnlocked(code: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun unlock(eaObject: EAObject)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun unlock(eaObjects: List<EAObject>)
}
