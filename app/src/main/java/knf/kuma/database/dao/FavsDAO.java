package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.TypeConverters;
import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.FavoriteObject;

@Dao
@TypeConverters(BaseConverter.class)
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
public interface FavsDAO {
    @Query("SELECT * FROM favoriteobject ORDER BY name")
    LiveData<List<FavoriteObject>> getAll();

    @Query("SELECT * FROM favoriteobject ORDER BY name")
    List<FavoriteObject> getAllRaw();

    @Query("SELECT `key`,name,category FROM favoriteobject GROUP BY category ORDER BY category")
    List<FavoriteObject> getCatagories();

    @Query("SELECT * FROM favoriteobject ORDER BY aid ASC")
    LiveData<List<FavoriteObject>> getAllID();

    @Query("SELECT * FROM favoriteobject WHERE category NOT LIKE :category ORDER BY name")
    List<FavoriteObject> getNotInCategory(String category);

    @Query("SELECT * FROM favoriteobject WHERE category LIKE :category ORDER BY name")
    List<FavoriteObject> getAllInCategory(String category);

    @Query("SELECT * FROM favoriteobject ORDER BY category")
    List<FavoriteObject> getByCategory();

    @Query("SELECT count(*) FROM favoriteobject WHERE `key` LIKE :key")
    Boolean isFav(int key);

    @Query("SELECT * FROM favoriteobject WHERE `key` LIKE :key LIMIT 1")
    LiveData<FavoriteObject> favObserver(int key);

    @Query("SELECT count(*) FROM favoriteobject")
    int getCount();

    @Query("SELECT count(*) FROM favoriteobject")
    LiveData<Integer> getCountLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFav(FavoriteObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<FavoriteObject> list);

    @Delete
    void deleteFav(FavoriteObject object);

    @Query("DELETE FROM favoriteobject")
    void clear();
}
