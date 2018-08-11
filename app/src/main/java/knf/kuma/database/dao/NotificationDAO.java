package knf.kuma.database.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.NotificationObj;

@Dao
@TypeConverters(BaseConverter.class)
public interface NotificationDAO {
    @Query("SELECT * FROM notificationobj")
    List<NotificationObj> getAll();

    @Query("SELECT * FROM notificationobj WHERE type=:type")
    List<NotificationObj> getByType(int type);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(NotificationObj obj);

    @Delete
    void delete(NotificationObj obj);

    @Query("DELETE FROM notificationobj")
    void clear();
}
