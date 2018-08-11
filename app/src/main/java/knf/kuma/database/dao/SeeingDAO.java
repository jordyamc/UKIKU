package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;
import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.SeeingObject;

@Dao
@TypeConverters(BaseConverter.class)
public interface SeeingDAO {

    @Query("SELECT * FROM seeingobject ORDER BY title")
    LiveData<List<SeeingObject>> getAll();

    @Query("SELECT * FROM seeingobject ORDER BY title")
    List<SeeingObject> getAllRaw();

    @Query("SELECT * FROM seeingobject WHERE aid LIKE :aid")
    SeeingObject getByAid(String aid);

    @Query("SELECT count(*) FROM seeingobject WHERE aid LIKE :aid")
    Boolean isSeeing(String aid);

    @Query("SELECT count(*) FROM seeingobject")
    LiveData<Integer> getCountLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(SeeingObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<SeeingObject> object);

    @Update
    void update(SeeingObject object);

    @Delete
    void remove(SeeingObject object);

    @Query("DELETE FROM seeingobject")
    void clear();
}
