package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import knf.kuma.pojos.RecentObject;

@Dao
public interface RecentsDAO {
    @Query("SELECT * FROM recentobject ORDER BY eid DESC")
    LiveData<List<RecentObject>> getObjects();

    @Query("SELECT * FROM recentobject ORDER BY eid DESC")
    List<RecentObject> getAll();

    @Query("DELETE FROM recentobject")
    void clear();

    @Insert
    void setCache(List<RecentObject> objects);
}
