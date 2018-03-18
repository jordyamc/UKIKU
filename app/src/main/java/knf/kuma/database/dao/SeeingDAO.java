package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import java.util.List;

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
