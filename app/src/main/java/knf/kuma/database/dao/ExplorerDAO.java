package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.Locale;

import knf.kuma.pojos.ExplorerObject;

/**
 * Created by Jordy on 29/01/2018.
 */
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
