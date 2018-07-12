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
import knf.kuma.pojos.AnimeObject;

@Dao
@TypeConverters({BaseConverter.class})
public interface ChaptersDAO {

    @Query("SELECT count(*) FROM animechapter")
    int init();

    @Query("SELECT * FROM animechapter")
    List<AnimeObject.WebInfo.AnimeChapter> getAll();

    @Query("SELECT * FROM animechapter WHERE eid = :eid LIMIT 1")
    LiveData<AnimeObject.WebInfo.AnimeChapter> chapterSeen(String eid);

    @Query("SELECT count(*) FROM animechapter WHERE eid = :eid")
    Boolean chapterIsSeen(String eid);

    @Query("SELECT * FROM animechapter WHERE eid IN (:eids) ORDER BY eid DESC LIMIT 1")
    AnimeObject.WebInfo.AnimeChapter getLast(List<String> eids);

    @Query("SELECT * FROM animechapter WHERE aid LIKE :aid ORDER BY `key` DESC LIMIT 1")
    AnimeObject.WebInfo.AnimeChapter getLastByAid(String aid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addChapter(AnimeObject.WebInfo.AnimeChapter chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<AnimeObject.WebInfo.AnimeChapter> list);

    @Delete
    void deleteChapter(AnimeObject.WebInfo.AnimeChapter chapter);

    @Query("DELETE FROM animechapter")
    void clear();
}
