package knf.kuma.database.dao

import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.NotificationObj

@Dao
@TypeConverters(BaseConverter::class)
interface NotificationDAO {
    @get:Query("SELECT * FROM notificationobj")
    val all: MutableList<NotificationObj>

    @Query("SELECT * FROM notificationobj WHERE type=:type")
    fun getByType(type: Int): MutableList<NotificationObj>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(obj: NotificationObj)

    @Delete
    fun delete(obj: NotificationObj)

    @Query("DELETE FROM notificationobj")
    fun clear()
}
