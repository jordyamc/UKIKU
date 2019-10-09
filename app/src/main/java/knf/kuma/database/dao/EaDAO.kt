package knf.kuma.database.dao

import androidx.room.*
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
