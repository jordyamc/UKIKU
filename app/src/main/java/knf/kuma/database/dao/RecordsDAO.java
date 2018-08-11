package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import knf.kuma.pojos.RecordObject;

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
