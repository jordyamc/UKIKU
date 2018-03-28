package knf.kuma.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import knf.kuma.pojos.GenreStatusObject;

/**
 * Created by jordy on 26/03/2018.
 */
@Dao
public interface GenresDAO {
    @Query("SELECT * FROM genrestatusobject WHERE count > 0 ORDER BY count DESC LIMIT 3")
    List<GenreStatusObject> getTop();

    @Query("SELECT * FROM genrestatusobject WHERE count < 0 ORDER BY name DESC")
    List<GenreStatusObject> getBlacklist();

    @Query("SELECT * FROM genrestatusobject ORDER BY name")
    List<GenreStatusObject> getAll();

    @Query("SELECT * FROM genrestatusobject WHERE name LIKE :name")
    GenreStatusObject getStatus(String name);

    @Query("SELECT * FROM genrestatusobject WHERE count > 0 ORDER BY count DESC")
    List<GenreStatusObject> getRanking();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStatus(GenreStatusObject object);

    @Query("DELETE FROM genrestatusobject WHERE count >= 0")
    void reset();
}
