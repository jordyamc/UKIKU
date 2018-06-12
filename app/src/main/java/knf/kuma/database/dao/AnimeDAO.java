package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.Set;

import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.AnimeObject;

@Dao
@TypeConverters(BaseConverter.class)
public interface AnimeDAO {
    @Query("SELECT count(*) FROM AnimeObject")
    int init();

    @Query("SELECT * FROM AnimeObject WHERE link = :link")
    LiveData<AnimeObject> getAnime(String link);

    @Query("SELECT * FROM AnimeObject WHERE aid = :aid")
    LiveData<AnimeObject> getAnimeByAid(String aid);

    @Query("SELECT * FROM AnimeObject WHERE link = :link")
    AnimeObject getAnimeRaw(String link);

    @Query("SELECT * FROM AnimeObject ORDER BY RANDOM() LIMIT :limit")
    LiveData<List<AnimeObject>> getRandom(int limit);

    @Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    LiveData<List<AnimeObject>> getByDay(int day, Set<String> list);

    @Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    List<AnimeObject> getByDayDirect(int day, Set<String> list);

    @Query("SELECT * FROM AnimeObject ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getAll();

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearch(String query);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    LiveData<List<AnimeObject>> getSearchList(String query);

    @Query("SELECT * FROM AnimeObject WHERE aid LIKE :query ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchID(String query);

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genres ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchG(String genres);

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genre ORDER BY name")
    DataSource.Factory<Integer, AnimeObject> getAllGenre(String genre);

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genres ORDER BY name")
    List<AnimeObject> getByGenres(String genres);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND genres LIKE :genres ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchTG(String query,String genres);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND state LIKE :state ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchS(String query,String state);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND state LIKE :state AND genres LIKE :genres ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchSG(String query,String state,String genres);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND type LIKE :type ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchTY(String query,String type);

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND type LIKE :type AND genres LIKE :genres ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getSearchTYG(String query,String type,String genres);

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getAnimeDir();

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY rate_stars DESC")
    DataSource.Factory<Integer,AnimeObject> getAnimeDirVotes();

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getOvaDir();

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY rate_stars DESC")
    DataSource.Factory<Integer,AnimeObject> getOvaDirVotes();

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY name")
    DataSource.Factory<Integer,AnimeObject> getMovieDir();

    @Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY rate_stars DESC")
    DataSource.Factory<Integer,AnimeObject> getMovieDirVotes();

    @Query("SELECT * FROM AnimeObject WHERE fileName LIKE :file")
    AnimeObject getByFile(String file);

    @Query("SELECT * FROM AnimeObject WHERE fileName IN (:names)")
    List<AnimeObject> getAllByFile(List<String> names);

    @Query("SELECT count(*) FROM AnimeObject WHERE sid LIKE :sid")
    Boolean existSid(String sid);

    @Query("SELECT count(*) FROM AnimeObject WHERE link LIKE :link")
    Boolean existLink(String link);

    @Query("SELECT * FROM AnimeObject WHERE link LIKE :link")
    AnimeObject getByLink(String link);

    @Query("SELECT * FROM AnimeObject WHERE aid LIKE :aid")
    AnimeObject getByAid(String aid);

    @Query("SELECT count(*) FROM AnimeObject WHERE `key` LIKE :aid")
    int getCount(int aid);

    @Update
    void updateAnime(AnimeObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AnimeObject object);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AnimeObject> objects);

    @Query("DELETE FROM animeobject")
    void nuke();

}
