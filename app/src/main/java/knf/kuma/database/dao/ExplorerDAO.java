package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import knf.kuma.pojos.ExplorerObject;

@Dao
public interface ExplorerDAO {
    @Query("SELECT * FROM explorerobject ORDER BY name")
    LiveData<List<ExplorerObject>> getAll();

    @Query("SELECT * FROM explorerobject WHERE fileName LIKE :file")
    LiveData<ExplorerObject> getItem(String file);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<ExplorerObject> list);

    @Update
    void update(ExplorerObject object);

    @Delete
    void delete(ExplorerObject object);

    @Query("DELETE FROM explorerobject")
    void deleteAll();
}
