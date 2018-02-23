package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import java.util.List;

import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.FavoriteObject;

/**
 * Created by Jordy on 05/01/2018.
 */

@Dao
@TypeConverters(BaseConverter.class)
public interface FavsDAO {
    @Query("SELECT * FROM favoriteobject ORDER BY name")
    LiveData<List<FavoriteObject>> getAll();

    @Query("SELECT * FROM favoriteobject ORDER BY name")
    List<FavoriteObject> getAllRaw();
    @Query("SELECT * FROM favoriteobject ORDER BY aid ASC")
    LiveData<List<FavoriteObject>> getAllID();

    @Query("SELECT * FROM favoriteobject WHERE category LIKE :category")
    List<FavoriteObject> getCategory(String category);

    @Query("SELECT count(*) FROM favoriteobject WHERE `key` LIKE :key")
    Boolean isFav(int key);

    @Query("SELECT * FROM favoriteobject WHERE `key` LIKE :key LIMIT 1")
    LiveData<FavoriteObject> favObserver(int key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFav(FavoriteObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<FavoriteObject> list);

    @Delete
    void deleteFav(FavoriteObject object);

    @Query("DELETE FROM favoriteobject")
    void clear();
}
