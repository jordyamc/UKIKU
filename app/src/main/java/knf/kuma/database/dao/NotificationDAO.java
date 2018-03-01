package knf.kuma.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import java.util.List;

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
