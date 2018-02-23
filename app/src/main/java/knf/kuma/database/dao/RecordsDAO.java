package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import knf.kuma.pojos.RecordObject;

/**
 * Created by Jordy on 19/01/2018.
 */

@Dao
public interface RecordsDAO {
    @Query("SELECT * FROM recordobject ORDER BY date DESC")
    LiveData<List<RecordObject>> getAll();

    @Query("SELECT * FROM recordobject ORDER BY date DESC")
    List<RecordObject> getAllRaw();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(RecordObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<RecordObject> list);

    @Delete
    void delete(RecordObject object);

    @Query("DELETE FROM recordobject")
    void clear();
}
