package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

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
